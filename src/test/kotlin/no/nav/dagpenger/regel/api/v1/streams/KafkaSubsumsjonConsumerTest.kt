package no.nav.dagpenger.regel.api.v1.streams

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyAll
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.regel.api.v1.db.BehovNotFoundException
import no.nav.dagpenger.regel.api.v1.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.v1.models.Behov
import no.nav.dagpenger.regel.api.v1.models.PacketKeys
import no.nav.dagpenger.regel.api.v1.models.Status
import no.nav.dagpenger.streams.Topics
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*


internal class KafkaSubsumsjonConsumerTest {

    companion object {
        val factory = ConsumerRecordFactory<String, Packet>(
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.serializer(),
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.serializer()
        )

        val config = Properties().apply {
            this[StreamsConfig.APPLICATION_ID_CONFIG] = "test"
            this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
        }
    }


    @Test
    fun `Ignore packets which does not satisfy any rules`() {
        val subsumsjonStore = mockk<SubsumsjonStore>()

        with(subsumsjonStore, Packet("{}")) {
            verify { subsumsjonStore wasNot Called }
        }
    }

    @Test
    fun `Packet which satisfies Manuell Grunnlag rules are processed `() {
        val subsumsjonStore = mockk<SubsumsjonStore>().apply {
            every { this@apply.insertSubsumsjon(any()) } returns 1
            every { this@apply.behovStatus("behovId") } returns Status.Pending
        }

        val packet = Behov("behovId", "aktorId", 1, LocalDate.now()).toPacket().apply {
            this.putValue(PacketKeys.MANUELT_GRUNNLAG, 2)
            this.putValue(PacketKeys.GRUNNLAG_RESULTAT, mapOf<String, Any>())
            this.putValue(PacketKeys.SATS_RESULTAT, mapOf<String, Any>())
            this.putValue(PacketKeys.GRUNNLAG_INNTEKTSPERIODER, mapOf<String, Any>())
        }

        with(subsumsjonStore, packet) {
            verifyAll {
                subsumsjonStore.insertSubsumsjon(any())
                subsumsjonStore.behovStatus("behovId")
            }
        }
    }

    @Test
    fun `Ignore Packages with result allready processed `() {
        val subsumsjonStore = mockk<SubsumsjonStore>().apply {
            every { this@apply.behovStatus("behovId") } returns Status.Done("id")
        }

        val packet = Packet("{}").apply {
            this.putValue(PacketKeys.BEHOV_ID, "behovId")
            this.putValue(PacketKeys.MANUELT_GRUNNLAG, 2)
            this.putValue(PacketKeys.GRUNNLAG_RESULTAT, mapOf<String, Any>())
            this.putValue(PacketKeys.SATS_RESULTAT, mapOf<String, Any>())
        }

        with(subsumsjonStore, packet) {
            verifyAll {
                subsumsjonStore.behovStatus("behovId")
            }
        }
    }

    @Test
    fun `Handle missing required packet value behovId gracefully`() {
        val subsumsjonStore = mockk<SubsumsjonStore>()

        val packet = Packet("{}").apply {
            this.putValue(PacketKeys.MANUELT_GRUNNLAG, 2)
            this.putValue(PacketKeys.GRUNNLAG_RESULTAT, mapOf<String, Any>())
            this.putValue(PacketKeys.SATS_RESULTAT, mapOf<String, Any>())
        }

        with(subsumsjonStore, packet) {
            verify { subsumsjonStore wasNot Called }
        }
    }

    @Test
    fun `Handle missing behov gracefully`() {
        val subsumsjonStore = mockk<SubsumsjonStore>().apply {
            every { this@apply.behovStatus("behovId") } throws BehovNotFoundException("not found")
        }


        val packet = Packet("{}").apply {
            this.putValue(PacketKeys.BEHOV_ID, "behovId")
            this.putValue(PacketKeys.MANUELT_GRUNNLAG, 2)
            this.putValue(PacketKeys.GRUNNLAG_RESULTAT, mapOf<String, Any>())
            this.putValue(PacketKeys.SATS_RESULTAT, mapOf<String, Any>())
        }

        with(subsumsjonStore, packet) {
            verifyAll {
                subsumsjonStore.behovStatus("behovId")
            }
        }
    }

    @Test
    fun `Handle error gracefull when unable to map from Packet to Subsumsjon `() {
        val subsumsjonStore = mockk<SubsumsjonStore>().apply {
            every { this@apply.behovStatus("behovId") } returns Status.Pending
        }


        val packet = Packet("{}").apply {
            this.putValue(PacketKeys.BEHOV_ID, "behovId")
            this.putValue(PacketKeys.MANUELT_GRUNNLAG, 2)
            this.putValue(PacketKeys.GRUNNLAG_RESULTAT, mapOf<String, Any>())
            this.putValue(PacketKeys.SATS_RESULTAT, mapOf<String, Any>())
        }

        with(subsumsjonStore, packet) {
            verifyAll {
                subsumsjonStore.behovStatus("behovId")
            }
        }
    }

    @Test
    fun `Packet which has a full result set is processed`() {
        val subsumsjonStore = mockk<SubsumsjonStore>().apply {
            every { this@apply.insertSubsumsjon(any()) } returns 1
            every { this@apply.behovStatus("behovId") } returns Status.Pending
        }

        val packet = Behov("behovId", "aktorId", 1, LocalDate.now()).toPacket().apply {
            this.putValue(PacketKeys.GRUNNLAG_RESULTAT, mapOf<String, Any>())
            this.putValue(PacketKeys.SATS_RESULTAT, mapOf<String, Any>())
            this.putValue(PacketKeys.MINSTEINNTEKT_RESULTAT, mapOf<String, Any>())
            this.putValue(PacketKeys.PERIODE_RESULTAT, mapOf<String, Any>())
            this.putValue(PacketKeys.GRUNNLAG_INNTEKTSPERIODER, mapOf<String, Any>())
            this.putValue(PacketKeys.MINSTEINNTEKT_INNTEKTSPERIODER, mapOf<String, Any>())
        }

        with(subsumsjonStore, packet) {
            verifyAll {
                subsumsjonStore.insertSubsumsjon(any())
                subsumsjonStore.behovStatus("behovId")
            }
        }
    }

    private fun with(subsumsjonStore: SubsumsjonStore, packet: Packet, testBlock: () -> Unit) {
        SumsumsjonPond(subsumsjonStore, "test").let {
            TopologyTestDriver(it.buildTopology(), config).use { topologyTestDriver ->
                topologyTestDriver.pipeInput(factory.create(packet))
                testBlock()
            }
        }
    }
}