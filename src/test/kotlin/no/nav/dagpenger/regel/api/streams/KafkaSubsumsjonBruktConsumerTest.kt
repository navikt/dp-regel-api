@file:Suppress("ktlint:standard:max-line-length")

package no.nav.dagpenger.regel.api.streams

import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.Vaktmester
import no.nav.dagpenger.regel.api.db.BruktSubsumsjonStore
import no.nav.dagpenger.regel.api.db.EksternSubsumsjonBrukt
import no.nav.dagpenger.regel.api.db.InternSubsumsjonBrukt
import no.nav.dagpenger.regel.api.db.SubsumsjonBruktNotFoundException
import no.nav.dagpenger.regel.api.db.SubsumsjonNotFoundException
import no.nav.dagpenger.regel.api.models.BehovId
import no.nav.dagpenger.regel.api.models.Faktum
import no.nav.dagpenger.regel.api.models.Kontekst
import no.nav.dagpenger.regel.api.models.RegelKontekst
import no.nav.dagpenger.regel.api.models.Subsumsjon
import no.nav.dagpenger.regel.api.models.ulidGenerator
import no.nav.dagpenger.regel.api.serder.jacksonObjectMapper
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Properties

class KafkaSubsumsjonBruktConsumerTest {
    val streamsConfig =
        Properties().apply {
            this[StreamsConfig.APPLICATION_ID_CONFIG] = "test"
            this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
        }

    private val subsumsjon =
        Subsumsjon(
            behovId = BehovId("01DSFT4J9SW8XDZ2ZJZMXD5XV7"),
            faktum = Faktum("aktorId", RegelKontekst("1", Kontekst.vedtak), LocalDate.now(), inntektsId = "test"),
            grunnlagResultat = emptyMap(),
            minsteinntektResultat =
                mapOf(
                    "subsumsjonsId" to ulidGenerator.nextULID(),
                ),
            periodeResultat = emptyMap(),
            satsResultat = emptyMap(),
            problem = Problem(title = "problem"),
        )

    val now = ZonedDateTime.now()

    @Test
    fun `should insert brukt subsumsjon`() {
        val lagretTilDb = slot<InternSubsumsjonBrukt>()
        val markertSomBrukt = slot<InternSubsumsjonBrukt>()

        val storeMock =
            mockk<BruktSubsumsjonStore>(relaxed = false).apply {
                every { this@apply.eksternTilInternSubsumsjon(any()) } returns
                    InternSubsumsjonBrukt(
                        id = ulidGenerator.nextULID(),
                        behandlingsId = "b",
                        arenaTs = now.minusMinutes(5),
                    )
                every { this@apply.insertSubsumsjonBrukt(capture(lagretTilDb)) } returns 1
                every { this@apply.getSubsumsjonByResult(any()) } returns subsumsjon
            }
        val vaktmester =
            mockk<Vaktmester>(relaxed = true).apply {
                every { this@apply.markerSomBrukt(capture(markertSomBrukt)) } just Runs
            }
        val config = Configuration

        val subsumsjonBruktConsumer =
            KafkaSubsumsjonBruktConsumer(config, BruktSubsumsjonStrategy(vaktmester, storeMock))

        val bruktSubsumsjon =
            EksternSubsumsjonBrukt(
                id = ulidGenerator.nextULID(),
                eksternId = 1234678L,
                arenaTs = now,
                ts = now.toInstant().toEpochMilli(),
            )
        TopologyTestDriver(subsumsjonBruktConsumer.buildTopology(), streamsConfig).use {
            val topic =
                it.createInputTopic(
                    subsumsjonBruktConsumer.subsumsjonBruktTopic.name,
                    subsumsjonBruktConsumer.subsumsjonBruktTopic.keySerde.serializer(),
                    subsumsjonBruktConsumer.subsumsjonBruktTopic.valueSerde.serializer(),
                )
            topic.pipeInput(bruktSubsumsjon.toJson())

            val outTopic =
                it.createOutputTopic(
                    "teamdagpenger.inntektbrukt.v1",
                    Serdes.StringSerde().deserializer(),
                    Serdes.StringSerde().deserializer(),
                )

            val out = jacksonObjectMapper.readTree(outTopic.readValue())

            out["@event_name"].asText() shouldBe "brukt_inntekt"
            out["inntektsId"].asText() shouldBe "test"
            out["aktorId"].asText() shouldBe "aktorId"
            out["kontekst"].let { json ->
                RegelKontekst(
                    json["id"].asText(),
                    Kontekst.valueOf(json["type"].asText()),
                )
            } shouldBe RegelKontekst("1", Kontekst.vedtak)

            lagretTilDb.isCaptured shouldBe true
            markertSomBrukt.isCaptured shouldBe true
            lagretTilDb.captured.arenaTs shouldBe now.minusMinutes(5L)
            lagretTilDb.captured shouldBe markertSomBrukt.captured
        }
    }

    @Test
    fun `skal håndtere tomme eller nullverdier for inntektsid`() {
        val lagretTilDb = slot<InternSubsumsjonBrukt>()
        val markertSomBrukt = slot<InternSubsumsjonBrukt>()

        val storeMock =
            mockk<BruktSubsumsjonStore>(relaxed = false).apply {
                every { this@apply.eksternTilInternSubsumsjon(any()) } returns
                    InternSubsumsjonBrukt(
                        id = ulidGenerator.nextULID(),
                        behandlingsId = "b",
                        arenaTs = now.minusMinutes(5),
                    )
                every { this@apply.insertSubsumsjonBrukt(capture(lagretTilDb)) } returns 1
                every {
                    this@apply.getSubsumsjonByResult(
                        any(),
                    )
                } returns subsumsjon.copy(faktum = subsumsjon.faktum.copy(inntektsId = null))
            }
        val vaktmester =
            mockk<Vaktmester>(relaxed = true).apply {
                every { this@apply.markerSomBrukt(capture(markertSomBrukt)) } just Runs
            }
        val config = Configuration

        val subsumsjonBruktConsumer =
            KafkaSubsumsjonBruktConsumer(config, BruktSubsumsjonStrategy(vaktmester, storeMock))

        val bruktSubsumsjon =
            EksternSubsumsjonBrukt(
                id = ulidGenerator.nextULID(),
                eksternId = 1234678L,
                arenaTs = now,
                ts = now.toInstant().toEpochMilli(),
            )
        TopologyTestDriver(subsumsjonBruktConsumer.buildTopology(), streamsConfig).use {
            val topic =
                it.createInputTopic(
                    subsumsjonBruktConsumer.subsumsjonBruktTopic.name,
                    subsumsjonBruktConsumer.subsumsjonBruktTopic.keySerde.serializer(),
                    subsumsjonBruktConsumer.subsumsjonBruktTopic.valueSerde.serializer(),
                )
            topic.pipeInput(bruktSubsumsjon.toJson())

            val outTopic =
                it.createOutputTopic(
                    "teamdagpenger.inntektbrukt.v1",
                    Serdes.StringSerde().deserializer(),
                    Serdes.StringSerde().deserializer(),
                )

            val out = jacksonObjectMapper.readTree(outTopic.readValue())

            out["@event_name"].asText() shouldBe "brukt_inntekt"
            out.has("inntektsId") shouldBe false
            out["aktorId"].asText() shouldBe "aktorId"
            out["kontekst"].let { json ->
                RegelKontekst(
                    json["id"].asText(),
                    Kontekst.valueOf(json["type"].asText()),
                )
            } shouldBe RegelKontekst("1", Kontekst.vedtak)

            lagretTilDb.isCaptured shouldBe true
            markertSomBrukt.isCaptured shouldBe true
            lagretTilDb.captured.arenaTs shouldBe now.minusMinutes(5L)
            lagretTilDb.captured shouldBe markertSomBrukt.captured
        }
    }

    @Test
    fun `skal håndtere nullverdier for subsumsjonsresultater`() {
        val lagretTilDb = slot<InternSubsumsjonBrukt>()
        val markertSomBrukt = slot<InternSubsumsjonBrukt>()

        val storeMock =
            mockk<BruktSubsumsjonStore>(relaxed = false).apply {
                every { this@apply.eksternTilInternSubsumsjon(any()) } returns
                    InternSubsumsjonBrukt(
                        id = ulidGenerator.nextULID(),
                        behandlingsId = "b",
                        arenaTs = now.minusMinutes(5),
                    )
                every { this@apply.insertSubsumsjonBrukt(capture(lagretTilDb)) } returns 1
                every { this@apply.getSubsumsjonByResult(any()) } throws SubsumsjonNotFoundException("Fant ikke id")
            }
        val vaktmester =
            mockk<Vaktmester>(relaxed = true).apply {
                every { this@apply.markerSomBrukt(capture(markertSomBrukt)) } just Runs
            }
        val config = Configuration

        val subsumsjonBruktConsumer =
            KafkaSubsumsjonBruktConsumer(config, BruktSubsumsjonStrategy(vaktmester, storeMock))

        val bruktSubsumsjon =
            EksternSubsumsjonBrukt(
                id = ulidGenerator.nextULID(),
                eksternId = 1234678L,
                arenaTs = now,
                ts = now.toInstant().toEpochMilli(),
            )
        TopologyTestDriver(subsumsjonBruktConsumer.buildTopology(), streamsConfig).use {
            val topic =
                it.createInputTopic(
                    subsumsjonBruktConsumer.subsumsjonBruktTopic.name,
                    subsumsjonBruktConsumer.subsumsjonBruktTopic.keySerde.serializer(),
                    subsumsjonBruktConsumer.subsumsjonBruktTopic.valueSerde.serializer(),
                )
            topic.pipeInput(bruktSubsumsjon.toJson())

            val outTopic =
                it.createOutputTopic(
                    "teamdagpenger.inntektbrukt.v1",
                    Serdes.StringSerde().deserializer(),
                    Serdes.StringSerde().deserializer(),
                )

            outTopic.isEmpty shouldBe true

            lagretTilDb.isCaptured shouldBe true
            markertSomBrukt.isCaptured shouldBe true
            lagretTilDb.captured.arenaTs shouldBe now.minusMinutes(5L)
            lagretTilDb.captured shouldBe markertSomBrukt.captured
        }
    }

    @Test
    fun `skal filtrere ut avsluttede og avbrutte vedtak`() {
        val now = ZonedDateTime.now()

        val config = Configuration

        val bruktSubsumsjonStrategy = mockk<BruktSubsumsjonStrategy>(relaxed = true)

        val subsumsjonBruktConsumer = KafkaSubsumsjonBruktConsumer(config, bruktSubsumsjonStrategy)

        val brukteSubsumsjoner =
            sequenceOf(
                EksternSubsumsjonBrukt(
                    id = ulidGenerator.nextULID(),
                    eksternId = 1234678L,
                    arenaTs = now,
                    ts = now.toInstant().toEpochMilli(),
                    utfall = "AVBRUTT",
                    vedtakStatus = "AVSLU",
                ),
                EksternSubsumsjonBrukt(
                    id = ulidGenerator.nextULID(),
                    eksternId = 1234678L,
                    arenaTs = now,
                    ts = now.toInstant().toEpochMilli(),
                    utfall = "NEI",
                    vedtakStatus = "AVSLU",
                ),
                EksternSubsumsjonBrukt(
                    id = ulidGenerator.nextULID(),
                    eksternId = 1234678L,
                    arenaTs = now,
                    ts = now.toInstant().toEpochMilli(),
                    utfall = "JA",
                    vedtakStatus = "IVERK",
                ),
            )

        TopologyTestDriver(subsumsjonBruktConsumer.buildTopology(), streamsConfig).use {
            val topic =
                it.createInputTopic(
                    subsumsjonBruktConsumer.subsumsjonBruktTopic.name,
                    subsumsjonBruktConsumer.subsumsjonBruktTopic.keySerde.serializer(),
                    subsumsjonBruktConsumer.subsumsjonBruktTopic.valueSerde.serializer(),
                )
            brukteSubsumsjoner.forEach { eksternSubsumsjonBrukt ->
                topic.pipeInput(eksternSubsumsjonBrukt.toJson())
            }
        }

        verify(exactly = 2) { bruktSubsumsjonStrategy.handle(any()) }
    }

    @Test
    fun `Håndtere der ekstern id ikke finnes`() {
        val storeMock =
            mockk<BruktSubsumsjonStore>(relaxed = false).apply {
                every { this@apply.eksternTilInternSubsumsjon(any()) } throws SubsumsjonBruktNotFoundException("fant ikke")
            }

        val config = Configuration

        val subsumsjonBruktConsumer =
            KafkaSubsumsjonBruktConsumer(config, BruktSubsumsjonStrategy(mockk(relaxed = true), storeMock))

        val bruktSubsumsjon =
            EksternSubsumsjonBrukt(
                id = ulidGenerator.nextULID(),
                eksternId = 1234678L,
                arenaTs = now,
                ts = now.toInstant().toEpochMilli(),
            )
        TopologyTestDriver(subsumsjonBruktConsumer.buildTopology(), streamsConfig).use {
            val topic =
                it.createInputTopic(
                    subsumsjonBruktConsumer.subsumsjonBruktTopic.name,
                    subsumsjonBruktConsumer.subsumsjonBruktTopic.keySerde.serializer(),
                    subsumsjonBruktConsumer.subsumsjonBruktTopic.valueSerde.serializer(),
                )
            topic.pipeInput(bruktSubsumsjon.toJson())

            val outTopic =
                it.createOutputTopic(
                    "teamdagpenger.inntektbrukt.v1",
                    Serdes.StringSerde().deserializer(),
                    Serdes.StringSerde().deserializer(),
                )

            outTopic.isEmpty shouldBe true
        }
    }

    @Test
    fun `Should be able to convert scientific notation back to long`() {
        val science = "1.2345678E7"
        12345678L shouldBe science.toDouble().toLong()
    }
}
