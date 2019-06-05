package no.nav.dagpenger.regel.api.streams

import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyAll
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.regel.api.models.PacketKeys
import no.nav.dagpenger.regel.api.models.behovId
import no.nav.dagpenger.streams.Topics
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.junit.jupiter.api.Test
import java.util.Properties

internal class KafkaSubsumsjonConsumerTest {

    @Test
    fun `Packet is ignored if behovId is missing`() {
        val mock = mockk<SubsumsjonPacketStrategy>()
        runTest(listOf(mock), Packet()) {
            verifyAll { mock wasNot Called }
        }
    }

    @Test
    fun `Packet is handled if behovId is set on packet`() {
        val packet = Packet().apply { this.putValue(PacketKeys.BEHOV_ID, "behovId") }
        val mock = mockk<SubsumsjonPacketStrategy>().apply {
            every { this@apply.run(match { it.behovId == "behovId" }) } just Runs
        }
        runTest(listOf(mock, mock), packet) {
            verify(exactly = 2) { mock.run(match { it.behovId == "behovId" }) }
        }
    }

    private companion object {
        val factory = ConsumerRecordFactory<String, Packet>(
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.serializer(),
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.serializer()
        )

        val config = Properties().apply {
            this[StreamsConfig.APPLICATION_ID_CONFIG] = "test"
            this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
        }

        fun runTest(strategies: List<SubsumsjonPacketStrategy>, packet: Packet, testBlock: () -> Unit) {
            SubsumsjonPond(strategies).let {
                TopologyTestDriver(it.buildTopology(), config).use { topologyTestDriver ->
                    topologyTestDriver.pipeInput(factory.create(packet))
                    testBlock()
                }
            }
        }
    }
}