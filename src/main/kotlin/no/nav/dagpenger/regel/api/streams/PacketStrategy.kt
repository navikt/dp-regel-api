package no.nav.dagpenger.regel.api.streams

import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.PacketKeys
import no.nav.dagpenger.regel.api.models.Subsumsjon

private val LOGGER = KotlinLogging.logger {}

internal interface SubsumsjonPacketStrategy {
    fun shouldHandle(packet: Packet): Boolean

    fun handle(packet: Packet)

    fun run(packet: Packet) {
        if (shouldHandle(packet)) handle(packet)
    }
}

internal class SuccessStrategy(private val subsumsjonStore: SubsumsjonStore) : SubsumsjonPacketStrategy {
    override fun shouldHandle(packet: Packet): Boolean = !packet.hasProblem()

    override fun handle(packet: Packet) {
        runCatching { subsumsjonStore.insertSubsumsjon(Subsumsjon.subsumsjonFrom(packet)) }
                .onFailure {
                    LOGGER.error(it) { "Failure handling packet: $packet" }
                }
    }
}

internal class CompleteResultStrategy(private val delegate: SuccessStrategy) : SubsumsjonPacketStrategy by delegate {
    override fun shouldHandle(packet: Packet): Boolean = delegate.shouldHandle(packet) &&
            packet.hasFields(PacketKeys.GRUNNLAG_RESULTAT, PacketKeys.SATS_RESULTAT, PacketKeys.MINSTEINNTEKT_RESULTAT, PacketKeys.PERIODE_RESULTAT)
}

internal class ManuellGrunnlagStrategy(private val delegate: SuccessStrategy) : SubsumsjonPacketStrategy by delegate {
    override fun shouldHandle(packet: Packet): Boolean = delegate.shouldHandle(packet) &&
            packet.hasFields(PacketKeys.MANUELT_GRUNNLAG, PacketKeys.GRUNNLAG_RESULTAT, PacketKeys.SATS_RESULTAT)
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
