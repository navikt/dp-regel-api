package no.nav.dagpenger.regel.api.streams

import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.PacketKeys
import no.nav.dagpenger.regel.api.models.Status
import no.nav.dagpenger.regel.api.models.Subsumsjon.Mapper.subsumsjonFrom
import no.nav.dagpenger.regel.api.models.behovId

private val LOGGER = KotlinLogging.logger {}

internal interface SubsumsjonPacketStrategy {
    val strategyName: String
        get() = this.javaClass.name

    fun shouldHandle(packet: Packet): Boolean

    fun handle(packet: Packet)

    fun run(packet: Packet) {
        if (shouldHandle(packet)) {
            LOGGER.info { "Strategy triggered: $strategyName" }
            handle(packet)
        }
    }
}

internal class SuccessStrategy(private val subsumsjonStore: SubsumsjonStore) : SubsumsjonPacketStrategy {
    override fun shouldHandle(packet: Packet): Boolean = !packet.hasProblem() && behovPending(packet.behovId)

    override fun handle(packet: Packet) {
        runCatching { subsumsjonStore.insertSubsumsjon(subsumsjonFrom(packet)) }
            .onFailure {
                LOGGER.error(it) { "Failure handling packet: $packet" }
            }
    }

    private fun behovPending(behovId: String) = runCatching { subsumsjonStore.behovStatus(behovId) }
        .onFailure { LOGGER.error(it) { "Failed to get status of behov: $behovId" } }
        .map { it == Status.Pending }
        .getOrDefault(false)
}

internal class CompleteResultStrategy(private val delegate: SuccessStrategy) : SubsumsjonPacketStrategy {
    override fun handle(packet: Packet) = delegate.handle(packet)

    override fun shouldHandle(packet: Packet) = packet.hasFields(PacketKeys.GRUNNLAG_RESULTAT, PacketKeys.SATS_RESULTAT, PacketKeys.MINSTEINNTEKT_RESULTAT, PacketKeys.PERIODE_RESULTAT) &&
        delegate.shouldHandle(packet)
}

internal class ManuellGrunnlagStrategy(private val delegate: SuccessStrategy) : SubsumsjonPacketStrategy {
    override fun handle(packet: Packet) = delegate.handle(packet)

    override fun shouldHandle(packet: Packet) = packet.hasFields(PacketKeys.MANUELT_GRUNNLAG, PacketKeys.GRUNNLAG_RESULTAT, PacketKeys.SATS_RESULTAT) &&
        delegate.shouldHandle(packet)
}

internal class ProblemStrategy : SubsumsjonPacketStrategy {
    override fun shouldHandle(packet: Packet) = packet.hasProblem()
    override fun handle(packet: Packet) {
        // todo LOg for now
        LOGGER.error { "Packet has problem  : ${packet.getProblem()}" }
    }
}

internal fun subsumsjonPacketStrategies(subsumsjonStore: SubsumsjonStore): List<SubsumsjonPacketStrategy> = listOf(
    ProblemStrategy(),
    ManuellGrunnlagStrategy(SuccessStrategy(subsumsjonStore)),
    CompleteResultStrategy(SuccessStrategy(subsumsjonStore))
)
