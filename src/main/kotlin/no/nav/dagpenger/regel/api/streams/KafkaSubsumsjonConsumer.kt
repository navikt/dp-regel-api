package no.nav.dagpenger.regel.api.streams

import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.plain.PondConsumer
import no.nav.dagpenger.regel.api.APPLICATION_NAME
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.models.PacketKeys
import no.nav.dagpenger.streams.KafkaCredential
import java.util.Properties
import java.util.function.Predicate

internal fun Packet.hasBehovId() = this.hasField(PacketKeys.BEHOV_ID)

internal class SubsumsjonPond(private val packetStrategies: List<SubsumsjonPacketStrategy>, private val configuration: Configuration) : PondConsumer(configuration.kafka.brokers) {
    override val SERVICE_APP_ID: String = APPLICATION_NAME

    override fun filterPredicates() = listOf(Predicate<Packet> { p -> p.hasBehovId() })

    override fun onPacket(packet: Packet) = packetStrategies.forEach { it.run(packet) }

    override fun getConsumerConfig(credential: KafkaCredential?): Properties {
        return super.getConsumerConfig(configuration.kafka.credential())
    }
}
