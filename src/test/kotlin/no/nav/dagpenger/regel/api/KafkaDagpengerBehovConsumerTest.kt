package no.nav.dagpenger.regel.api

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.regel.api.grunnlag.GrunnlagSubsumsjonerDummy
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektSubsumsjonerDummy
import no.nav.dagpenger.regel.api.models.common.InntektResponse
import no.nav.dagpenger.regel.api.models.common.InntektsPeriode
import no.nav.dagpenger.regel.api.periode.PeriodeSubsumsjonerDummy
import no.nav.dagpenger.regel.api.sats.SatsSubsumsjonerDummy
import no.nav.dagpenger.regel.api.tasks.TasksDummy
import no.nav.dagpenger.streams.Topics
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
                Topics.DAGPENGER_BEHOV_EVENT.name,
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
        val minsteinntektSubsumsjonerDummy = MinsteinntektSubsumsjonerDummy()
        val tasks = TasksDummy()
        val consumer = KafkaDagpengerBehovConsumer(
                Environment(
                        username = "bogus",
                        password = "bogus"
                ),
                tasks,
                minsteinntektSubsumsjonerDummy,
                PeriodeSubsumsjonerDummy(),
                GrunnlagSubsumsjonerDummy(),
                SatsSubsumsjonerDummy()
        )

        val inntektsPerioder = setOf(
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

        val minsteinntektResultat = MinsteinntektResultat(
                "123",
                "minsteinntektSubsumsjon",
                "regel",
                true)

        val behov = SubsumsjonsBehov(
                TasksDummy.minsteinntektPendingBehovId,
                "12345",
                Random().nextInt(),
                LocalDate.now(),
                inntektV1 = inntektAdapter.toJson(Inntekt("", emptyList())),
                minsteinntektResultat = minsteinntektResultat,
                minsteinntektInntektsPerioder = inntektsPerioderAdapter.toJson(inntektsPerioder)
        )
        val behovJson = jsonAdapter.toJson(behov)

        TopologyTestDriver(consumer.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(behovJson)
            topologyTestDriver.pipeInput(inputRecord)
        }

        assertNotNull(minsteinntektSubsumsjonerDummy.storedMinsteinntektSubsumsjon)
        assertEquals(
                "minsteinntektSubsumsjon",
                minsteinntektSubsumsjonerDummy.storedMinsteinntektSubsumsjon!!.subsumsjonsId)
        assertEquals(3, minsteinntektSubsumsjonerDummy.storedMinsteinntektSubsumsjon!!.inntekt.size)
    }

    @Test
    fun ` Should store received grunnlagSubsumsjon `() {
        val grunnlagSubsumsjonerDummy = GrunnlagSubsumsjonerDummy()
        val tasks = TasksDummy()
        val consumer = KafkaDagpengerBehovConsumer(
                Environment(
                        username = "bogus",
                        password = "bogus"
                ),
                tasks,
                MinsteinntektSubsumsjonerDummy(),
                PeriodeSubsumsjonerDummy(),
                grunnlagSubsumsjonerDummy,
                SatsSubsumsjonerDummy()
        )

        val grunnlagResultat = GrunnlagResultat(
                "123",
                "grunnlagSubsumsjon",
                "regel",
                1000,
                1500)

        val behov = SubsumsjonsBehov(
                TasksDummy.grunnlagPendingBehovId,
                "12345",
                Random().nextInt(),
                LocalDate.now(),
                inntektV1 = inntektAdapter.toJson(Inntekt("", emptyList())),
                grunnlagResultat = grunnlagResultat
        )
        val behovJson = jsonAdapter.toJson(behov)

        TopologyTestDriver(consumer.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(behovJson)
            topologyTestDriver.pipeInput(inputRecord)
        }

        assertNotNull(grunnlagSubsumsjonerDummy.storedGrunnlagSubsumsjon)
        assertEquals(
                "grunnlagSubsumsjon",
                grunnlagSubsumsjonerDummy.storedGrunnlagSubsumsjon!!.subsumsjonsId)
    }

    @Test
    fun ` Should store received periodeSubsumsjon `() {
        val tasks = TasksDummy()
        val periodeSubsumsjonerDummy = PeriodeSubsumsjonerDummy()
        val consumer = KafkaDagpengerBehovConsumer(
                Environment(
                        username = "bogus",
                        password = "bogus"
                ),
                tasks,
                MinsteinntektSubsumsjonerDummy(),
                periodeSubsumsjonerDummy,
                GrunnlagSubsumsjonerDummy(),
                SatsSubsumsjonerDummy()
        )

        val periodeResultat = PeriodeResultat(
                "123",
                "periodeSubsumsjon",
                "regel",
                52)

        val behov = SubsumsjonsBehov(
                TasksDummy.periodePendingBehovId,
                "12345",
                Random().nextInt(),
                LocalDate.now(),
                inntektV1 = inntektAdapter.toJson(Inntekt("", emptyList())),
                periodeResultat = periodeResultat
        )
        val behovJson = jsonAdapter.toJson(behov)

        TopologyTestDriver(consumer.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(behovJson)
            topologyTestDriver.pipeInput(inputRecord)
        }

        assertNotNull(periodeSubsumsjonerDummy.storedPeriodeSubsumsjon)
        assertEquals(
                "periodeSubsumsjon",
                periodeSubsumsjonerDummy.storedPeriodeSubsumsjon!!.subsumsjonsId)
    }

    @Test
    fun ` Should store received satsSubsumsjon `() {
        val tasks = TasksDummy()
        val satsSubsumsjonerDummy = SatsSubsumsjonerDummy()
        val consumer = KafkaDagpengerBehovConsumer(
                Environment(
                        username = "bogus",
                        password = "bogus"
                ),
                tasks,
                MinsteinntektSubsumsjonerDummy(),
                PeriodeSubsumsjonerDummy(),
                GrunnlagSubsumsjonerDummy(),
                satsSubsumsjonerDummy
        )

        val satsResultat = SatsResultat(
                "123",
                "satsSubsumsjon",
                "regel",
                0,
                0,
                false)

        val behov = SubsumsjonsBehov(
                TasksDummy.satsPendingBehovId,
                "12345",
                Random().nextInt(),
                LocalDate.now(),
                satsResultat = satsResultat
        )
        val behovJson = jsonAdapter.toJson(behov)

        TopologyTestDriver(consumer.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(behovJson)
            topologyTestDriver.pipeInput(inputRecord)
        }

        assertNotNull(satsSubsumsjonerDummy.storedSatsSubsumsjon)
        assertEquals(
                "satsSubsumsjon",
                satsSubsumsjonerDummy.storedSatsSubsumsjon!!.subsumsjonsId)
    }
}