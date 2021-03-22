package no.nav.dagpenger.regel.api.streams

import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.Vaktmester
import no.nav.dagpenger.regel.api.db.BruktSubsumsjonStore
import no.nav.dagpenger.regel.api.db.EksternSubsumsjonBrukt
import no.nav.dagpenger.regel.api.db.InternSubsumsjonBrukt
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.Properties

class KafkaEksternSubsumsjonBruktConsumerTest {
    val streamsConfig = Properties().apply {
        this[StreamsConfig.APPLICATION_ID_CONFIG] = "test"
        this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
    }

    @Test
    fun `should insert brukt subsumsjon`() {
        val now = ZonedDateTime.now()
        runBlocking {
            val lagretTilDb = slot<InternSubsumsjonBrukt>()
            val markertSomBrukt = slot<InternSubsumsjonBrukt>()
            val storeMock = mockk<BruktSubsumsjonStore>(relaxed = false).apply {
                every { this@apply.eksternTilInternSubsumsjon(any()) } returns InternSubsumsjonBrukt(
                    id = "test",
                    behandlingsId = "b",
                    arenaTs = now.minusMinutes(5)
                )
                every { this@apply.insertSubsumsjonBrukt(capture(lagretTilDb)) } returns 1
            }
            val vaktmester = mockk<Vaktmester>(relaxed = true).apply {
                every { this@apply.markerSomBrukt(capture(markertSomBrukt)) } just Runs
            }
            val config = Configuration()

            val subsumsjonBruktConsumer =
                KafkaSubsumsjonBruktConsumer(config, BruktSubsumsjonStrategy(vaktmester, storeMock))

            val bruktSubsumsjon =
                EksternSubsumsjonBrukt(
                    id = "test",
                    eksternId = 1234678L,
                    arenaTs = now,
                    ts = now.toInstant().toEpochMilli()
                )
            TopologyTestDriver(subsumsjonBruktConsumer.buildTopology(), streamsConfig).use {
                val topic = it.createInputTopic(
                    subsumsjonBruktConsumer.subsumsjonBruktTopic.name,
                    subsumsjonBruktConsumer.subsumsjonBruktTopic.keySerde.serializer(),
                    subsumsjonBruktConsumer.subsumsjonBruktTopic.valueSerde.serializer()
                )
                topic.pipeInput(bruktSubsumsjon.toJson())
            }

            lagretTilDb.isCaptured shouldBe true
            markertSomBrukt.isCaptured shouldBe true
            lagretTilDb.captured.arenaTs shouldBe now.minusMinutes(5L)
            lagretTilDb.captured shouldBe markertSomBrukt.captured
        }
    }

    @Test
    fun `skal filtrere ut avsluttede og avbrutte vedtak`() {
        val now = ZonedDateTime.now()

        val config = Configuration()

        val bruktSubsumsjonStrategy = mockk<BruktSubsumsjonStrategy>(relaxed = true)

        val subsumsjonBruktConsumer = KafkaSubsumsjonBruktConsumer(config, bruktSubsumsjonStrategy)

        val brukteSubsumsjoner = sequenceOf(
            EksternSubsumsjonBrukt(
                id = "test",
                eksternId = 1234678L,
                arenaTs = now,
                ts = now.toInstant().toEpochMilli(),
                utfall = "AVBRUTT",
                vedtakStatus = "AVSLU"
            ),
            EksternSubsumsjonBrukt(
                id = "test",
                eksternId = 1234678L,
                arenaTs = now,
                ts = now.toInstant().toEpochMilli(),
                utfall = "NEI",
                vedtakStatus = "AVSLU"
            ),
            EksternSubsumsjonBrukt(
                id = "test",
                eksternId = 1234678L,
                arenaTs = now,
                ts = now.toInstant().toEpochMilli(),
                utfall = "JA",
                vedtakStatus = "IVERK"
            )
        )

        TopologyTestDriver(subsumsjonBruktConsumer.buildTopology(), streamsConfig).use {
            val topic = it.createInputTopic(
                subsumsjonBruktConsumer.subsumsjonBruktTopic.name,
                subsumsjonBruktConsumer.subsumsjonBruktTopic.keySerde.serializer(),
                subsumsjonBruktConsumer.subsumsjonBruktTopic.valueSerde.serializer()
            )
            brukteSubsumsjoner.forEach { eksternSubsumsjonBrukt ->
                topic.pipeInput(eksternSubsumsjonBrukt.toJson())
            }
        }

        verify(exactly = 2) { bruktSubsumsjonStrategy.handle(any()) }
    }

    @Test
    fun `Should be able to convert scientific notation back to long`() {
        val science = "1.2345678E7"
        12345678L shouldBe science.toDouble().toLong()
    }
}
