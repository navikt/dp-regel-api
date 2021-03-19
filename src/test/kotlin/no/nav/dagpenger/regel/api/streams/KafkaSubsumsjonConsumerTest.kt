package no.nav.dagpenger.regel.api.streams

import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyAll
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.models.PacketKeys
import no.nav.dagpenger.regel.api.models.behovId
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
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
        val packet = Packet().apply { this.putValue(PacketKeys.BEHOV_ID, "01DSFHJA5MJWPW7TV0GGCSBC54") }
        val mock = mockk<SubsumsjonPacketStrategy>().apply {
            every { this@apply.run(match { it.behovId.id == "01DSFHJA5MJWPW7TV0GGCSBC54" }) } just Runs
        }
        runTest(listOf(mock, mock), packet) {
            verify(exactly = 2) { mock.run(match { it.behovId.id == "01DSFHJA5MJWPW7TV0GGCSBC54" }) }
        }
    }

    @Test
    fun `Skip packets with problems `() {

        val packet = Packet().apply {
            this.putValue(PacketKeys.BEHOV_ID, "01DSFHJA5MJWPW7TV0GGCSBC54")
            this.addProblem(Problem(title = "feil"))
        }

        val mock = mockk<SubsumsjonPacketStrategy>()
        runTest(listOf(mock), packet) {
            verifyAll { mock wasNot Called }
        }
    }

    private companion object {
        val config = Properties().apply {
            this[StreamsConfig.APPLICATION_ID_CONFIG] = "test"
            this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
        }

        fun runTest(strategies: List<SubsumsjonPacketStrategy>, packet: Packet, testBlock: () -> Unit) {
            val configuration = Configuration()
            SubsumsjonPond(strategies, configuration, configuration.regelTopic).let {
                TopologyTestDriver(it.buildTopology(), config).use { topologyTestDriver ->
                    val input = topologyTestDriver.createInputTopic(
                        configuration.regelTopic.name,
                        configuration.regelTopic.keySerde.serializer(),
                        configuration.regelTopic.valueSerde.serializer()
                    )
                    input.pipeInput(packet)

                    testBlock()
                }
            }
        }
    }
}
