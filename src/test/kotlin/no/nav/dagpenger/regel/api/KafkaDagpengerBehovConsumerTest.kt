package no.nav.dagpenger.regel.api

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import io.kotlintest.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verifyAll
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.GrunnlagSubsumsjon
import no.nav.dagpenger.regel.api.models.InntektResponse
import no.nav.dagpenger.regel.api.models.InntektsPeriode
import no.nav.dagpenger.regel.api.models.MinsteinntektSubsumsjon
import no.nav.dagpenger.regel.api.models.PeriodeSubsumsjon
import no.nav.dagpenger.regel.api.models.SatsSubsumsjon
import no.nav.dagpenger.streams.Topics.DAGPENGER_BEHOV_PACKET_EVENT
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.Properties
import java.util.Random

class KafkaDagpengerBehovConsumerTest {
    val jsonAdapter = moshiInstance.adapter(SubsumsjonsBehov::class.java)
    val inntektAdapter = moshiInstance.adapter<Inntekt>(Inntekt::class.java)

    val inntektsPerioderAdapter: JsonAdapter<Set<InntektResponse>> =
        moshiInstance.adapter(Types.newParameterizedType(Set::class.java, InntektResponse::class.java))

    companion object {
        val factory = ConsumerRecordFactory<String, String>(
            DAGPENGER_BEHOV_PACKET_EVENT.name,
            Serdes.String().serializer(),
            Serdes.String().serializer()
        )

        val config = Properties().apply {
            this[StreamsConfig.APPLICATION_ID_CONFIG] = "test"
            this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
        }
    }

    @Test
    fun ` Should store received minsteinntektSubsumsjon `() {

        val behovJson = jsonAdapter.toJson(SubsumsjonsBehov(
            "minsteinntektSubsumsjon",
            "12345",
            Random().nextInt(),
            LocalDate.now(),
            inntektV1 = Inntekt("", emptyList()),
            minsteinntektResultat = MinsteinntektResultat(
                "123",
                "minsteinntektSubsumsjon",
                "regel",
                true),
            minsteinntektInntektsPerioder = setOf(
                InntektResponse(
                    inntekt = BigDecimal.ZERO,
                    periode = 1,
                    inntektsPeriode = InntektsPeriode(YearMonth.of(2018, 2), YearMonth.of(2019, 1)),
                    inneholderFangstOgFisk = false,
                    andel = BigDecimal.ZERO
                ),
                InntektResponse(
                    inntekt = BigDecimal.ZERO,
                    periode = 2,
                    inntektsPeriode = InntektsPeriode(YearMonth.of(2017, 2), YearMonth.of(2018, 1)),
                    inneholderFangstOgFisk = false,
                    andel = BigDecimal.ZERO
                ),
                InntektResponse(
                    inntekt = BigDecimal.ZERO,
                    periode = 3,
                    inntektsPeriode = InntektsPeriode(YearMonth.of(2016, 2), YearMonth.of(2017, 1)),
                    inneholderFangstOgFisk = false,
                    andel = BigDecimal.ZERO
                )
            )
        ))

        val slot = slot<MinsteinntektSubsumsjon>()
        val subsumsjonStoreMock = mockk<SubsumsjonStore>().apply {
            every {
                this@apply.insertSubsumsjon(
                    subsumsjon = capture(slot)
                )
            } just Runs
        }

        val consumer = KafkaDagpengerBehovConsumer(
            Configuration(),
            subsumsjonStoreMock
        )

        TopologyTestDriver(consumer.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(behovJson)
            topologyTestDriver.pipeInput(inputRecord)
        }

        verifyAll { subsumsjonStoreMock.insertSubsumsjon(any()) }

        with(slot.captured) {
            subsumsjonsId shouldBe "minsteinntektSubsumsjon"
            inntekt.size shouldBe 3
        }
    }

    @Test
    fun ` Should store received grunnlagSubsumsjon `() {

        val behovJson = jsonAdapter.toJson(SubsumsjonsBehov(
            "grunnlagSubsumsjon",
            "12345",
            Random().nextInt(),
            LocalDate.now(),
            inntektV1 = Inntekt("", emptyList()),
            grunnlagResultat = GrunnlagResultat(
                "123",
                "grunnlagSubsumsjon",
                "regel",
                1000,
                1500,
                "ArbeidsinntektSiste12")
        ))

        val slot = slot<GrunnlagSubsumsjon>()
        val subsumsjonStoreMock = mockk<SubsumsjonStore>().apply {
            every {
                this@apply.insertSubsumsjon(
                    subsumsjon = capture(slot)
                )
            } just Runs
        }

        val consumer = KafkaDagpengerBehovConsumer(
            Configuration(),
            subsumsjonStoreMock
        )

        TopologyTestDriver(consumer.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(behovJson)
            topologyTestDriver.pipeInput(inputRecord)
        }

        verifyAll { subsumsjonStoreMock.insertSubsumsjon(any()) }

        with(slot.captured) {
            subsumsjonsId shouldBe "grunnlagSubsumsjon"
            resultat.beregningsregel shouldBe "ArbeidsinntektSiste12"
        }
    }

    @Test
    fun ` Should store received grunnlagSubsumsjon without inntekt `() {

        val behovJson = jsonAdapter.toJson(SubsumsjonsBehov(
            "grunnlagSubsumsjon",
            "12345",
            Random().nextInt(),
            LocalDate.now(),
            grunnlagResultat = GrunnlagResultat(
                "123",
                "grunnlagSubsumsjon",
                "regel",
                1000,
                1500,
                "Manuell under 6G")
        ))

        val slot = slot<GrunnlagSubsumsjon>()
        val subsumsjonStoreMock = mockk<SubsumsjonStore>().apply {
            every {
                this@apply.insertSubsumsjon(
                    subsumsjon = capture(slot)
                )
            } just Runs
        }

        val consumer = KafkaDagpengerBehovConsumer(
            Configuration(),
            subsumsjonStoreMock
        )
        TopologyTestDriver(consumer.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(behovJson)
            topologyTestDriver.pipeInput(inputRecord)
        }

        verifyAll { subsumsjonStoreMock.insertSubsumsjon(any()) }
        with(slot.captured) {
            subsumsjonsId shouldBe "grunnlagSubsumsjon"
            resultat.beregningsregel shouldBe "Manuell under 6G"
            faktum.inntektsId shouldBe "MANUELT_GRUNNLAG"
        }
    }

    @Test
    fun ` Should store received periodeSubsumsjon `() {

        val behovJson = jsonAdapter.toJson(SubsumsjonsBehov(
            "",
            "12345",
            Random().nextInt(),
            LocalDate.now(),
            inntektV1 = Inntekt("", emptyList()),
            periodeResultat = PeriodeResultat(
                "123",
                "periodeSubsumsjon",
                "regel",
                52)
        ))

        val slot = slot<PeriodeSubsumsjon>()
        val subsumsjonStoreMock = mockk<SubsumsjonStore>().apply {
            every {
                this@apply.insertSubsumsjon(
                    subsumsjon = capture(slot)
                )
            } just Runs
        }

        val consumer = KafkaDagpengerBehovConsumer(
            Configuration(),
            subsumsjonStoreMock
        )
        TopologyTestDriver(consumer.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(behovJson)
            topologyTestDriver.pipeInput(inputRecord)
        }

        verifyAll { subsumsjonStoreMock.insertSubsumsjon(any()) }
        with(slot.captured) {
            subsumsjonsId shouldBe "periodeSubsumsjon"
        }
    }

    @Test
    fun ` Should store received satsSubsumsjon `() {

        val behovJson = jsonAdapter.toJson(SubsumsjonsBehov(
            "",
            "12345",
            Random().nextInt(),
            LocalDate.now(),
            satsResultat = SatsResultat(
                "123",
                "satsSubsumsjon",
                "regel",
                0,
                0,
                false)
        ))

        val slot = slot<SatsSubsumsjon>()
        val subsumsjonStoreMock = mockk<SubsumsjonStore>().apply {
            every {
                this@apply.insertSubsumsjon(
                    subsumsjon = capture(slot)
                )
            } just Runs
        }

        val consumer = KafkaDagpengerBehovConsumer(
            Configuration(),
            subsumsjonStoreMock
        )

        TopologyTestDriver(consumer.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(behovJson)
            topologyTestDriver.pipeInput(inputRecord)
        }

        verifyAll { subsumsjonStoreMock.insertSubsumsjon(any()) }
        with(slot.captured) {
            subsumsjonsId shouldBe "satsSubsumsjon"
        }
    }
}
