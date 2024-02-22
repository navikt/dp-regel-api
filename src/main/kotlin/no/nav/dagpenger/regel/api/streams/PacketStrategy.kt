package no.nav.dagpenger.regel.api.streams

import io.prometheus.client.Summary
import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.BehovId
import no.nav.dagpenger.regel.api.models.PacketKeys
import no.nav.dagpenger.regel.api.models.Status
import no.nav.dagpenger.regel.api.models.Subsumsjon.Mapper.subsumsjonFrom
import no.nav.dagpenger.regel.api.models.behovId
import java.time.Duration
import java.time.LocalDateTime

private val LOGGER = KotlinLogging.logger {}

const val PACKET_PROCESS_TIME_METRIC_NAME = "packet_process_time_nanoseconds"
val packetProcessTimeLatency: Summary =
    Summary.build()
        .name(PACKET_PROCESS_TIME_METRIC_NAME)
        .quantile(0.5, 0.05) // Add 50th percentile (= median) with 5% tolerated error
        .quantile(0.9, 0.01) // Add 90th percentile with 1% tolerated error
        .quantile(0.99, 0.001) // Add 99th percentile with 0.1% tolerated error
        .help("Process time for a single packet")
        .labelNames("strategy")
        .register()

internal interface SubsumsjonPacketStrategy {
    val simpleStrategyName: String get() = this.javaClass.simpleName

    fun shouldHandle(packet: Packet): Boolean

    fun handle(packet: Packet)

    fun run(packet: Packet) {
        if (shouldHandle(packet)) {
            val started: LocalDateTime? =
                packet.getNullableStringValue("system_started")
                    ?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
            LOGGER.info { "Strategy triggered: $simpleStrategyName" }
            handle(packet)
            started?.let { Duration.between(it, LocalDateTime.now()) }?.let {
                packetProcessTimeLatency.labels(this.simpleStrategyName).observe(it.nano.toDouble())
            }
        }
    }
}

internal class PendingBehovStrategy(private val subsumsjonStore: SubsumsjonStore) : SubsumsjonPacketStrategy {
    override fun shouldHandle(packet: Packet): Boolean = behovPending(packet.behovId)

    override fun handle(packet: Packet) {
        runCatching { subsumsjonStore.insertSubsumsjon(subsumsjonFrom(packet)) }
            .onFailure {
                LOGGER.error(it) { "Failure handling packet: $packet" }
            }
    }

    private fun behovPending(behovId: BehovId) =
        runCatching { subsumsjonStore.behovStatus(behovId) }
            .onFailure { LOGGER.error(it) { "Failed to get status of behov: $behovId" } }
            .map { it == Status.Pending }
            .getOrDefault(false)
}

internal class SuccessStrategy(private val delegate: PendingBehovStrategy) : SubsumsjonPacketStrategy {
    override fun handle(packet: Packet) = delegate.handle(packet)

    override fun shouldHandle(packet: Packet): Boolean = !packet.hasProblem() && delegate.shouldHandle(packet)
}

internal class CompleteResultStrategy(private val delegate: SuccessStrategy) : SubsumsjonPacketStrategy {
    override fun handle(packet: Packet) = delegate.handle(packet)

    override fun shouldHandle(packet: Packet) =
        packet.hasFields(
            PacketKeys.GRUNNLAG_RESULTAT,
            PacketKeys.SATS_RESULTAT,
            PacketKeys.MINSTEINNTEKT_RESULTAT,
            PacketKeys.PERIODE_RESULTAT,
        ) &&
            delegate.shouldHandle(packet)
}

internal class ManuellGrunnlagStrategy(private val delegate: SuccessStrategy) : SubsumsjonPacketStrategy {
    override fun handle(packet: Packet) = delegate.handle(packet)

    override fun shouldHandle(packet: Packet) =
        packet.hasFields(PacketKeys.MANUELT_GRUNNLAG, PacketKeys.GRUNNLAG_RESULTAT, PacketKeys.SATS_RESULTAT) &&
            delegate.shouldHandle(packet)
}

internal class ForrigeGrunnlagStrategy(private val delegate: SuccessStrategy) : SubsumsjonPacketStrategy {
    override fun handle(packet: Packet) = delegate.handle(packet)

    override fun shouldHandle(packet: Packet) =
        packet.hasFields(PacketKeys.FORRIGE_GRUNNLAG, PacketKeys.GRUNNLAG_RESULTAT, PacketKeys.SATS_RESULTAT) &&
            delegate.shouldHandle(packet)
}

internal class ProblemStrategy(private val delegate: PendingBehovStrategy) : SubsumsjonPacketStrategy {
    override fun shouldHandle(packet: Packet) = packet.hasProblem() && delegate.shouldHandle(packet)

    override fun handle(packet: Packet) = delegate.handle(packet)
}

internal fun subsumsjonPacketStrategies(subsumsjonStore: SubsumsjonStore): List<SubsumsjonPacketStrategy> {
    val pendingBehovStrategy = PendingBehovStrategy(subsumsjonStore)
    val successStrategy = SuccessStrategy(pendingBehovStrategy)
    return listOf(
        ProblemStrategy(pendingBehovStrategy),
        ManuellGrunnlagStrategy(successStrategy),
        ForrigeGrunnlagStrategy(successStrategy),
        CompleteResultStrategy(successStrategy),
    )
}
