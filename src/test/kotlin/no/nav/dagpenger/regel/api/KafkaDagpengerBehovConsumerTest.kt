package no.nav.dagpenger.regel.api

import io.mockk.mockk
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.regel.api.models.InntektResponse
import no.nav.dagpenger.regel.api.models.InntektsPeriode
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
        val consumer = KafkaDagpengerBehovConsumer(
            Configuration(),
            mockk()
        )

        val inntektsPerioder = listOf(
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
            "", //todo
            "12345",
            Random().nextInt(),
            LocalDate.now(),
            inntektV1 = inntektAdapter.toJson(Inntekt("", emptyList())),
            minsteinntektResultat = minsteinntektResultat,
            minsteinntektInntektsPerioder = inntektsPerioder
        )
        val behovJson = jsonAdapter.toJson(behov)

        TopologyTestDriver(consumer.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(behovJson)
            topologyTestDriver.pipeInput(inputRecord)
        }
/*
        assertNotNull(minsteinntektSubsumsjonerDummy.storedMinsteinntektSubsumsjon)
        assertEquals(
            "minsteinntektSubsumsjon",
            minsteinntektSubsumsjonerDummy.storedMinsteinntektSubsumsjon!!.subsumsjonsId)
        assertEquals(3, minsteinntektSubsumsjonerDummy.storedMinsteinntektSubsumsjon!!.inntekt.size)
        */
    }

    @Test
    fun ` Should store received grunnlagSubsumsjon `() {
        val consumer = KafkaDagpengerBehovConsumer(
            Configuration(),
            mockk()
        )

        val grunnlagResultat = GrunnlagResultat(
            "123",
            "grunnlagSubsumsjon",
            "regel",
            1000,
            1500)

        val behov = SubsumsjonsBehov(
            "",
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
        /*

        assertNotNull(grunnlagSubsumsjonerDummy.storedGrunnlagSubsumsjon)
        assertEquals(
            "grunnlagSubsumsjon",
            grunnlagSubsumsjonerDummy.storedGrunnlagSubsumsjon!!.subsumsjonsId)
            */
    }

    @Test
    fun ` Should store received periodeSubsumsjon `() {
        val consumer = KafkaDagpengerBehovConsumer(
            Configuration(),
            mockk()
        )

        val periodeResultat = PeriodeResultat(
            "123",
            "periodeSubsumsjon",
            "regel",
            52)

        val behov = SubsumsjonsBehov(
            "",
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

        /*

        assertNotNull(periodeSubsumsjonerDummy.storedPeriodeSubsumsjon)
        assertEquals(
            "periodeSubsumsjon",
            periodeSubsumsjonerDummy.storedPeriodeSubsumsjon!!.subsumsjonsId)

            */
    }

    @Test
    fun ` Should store received satsSubsumsjon `() {
        val consumer = KafkaDagpengerBehovConsumer(
            Configuration(),
            mockk()
        )

        val satsResultat = SatsResultat(
            "123",
            "satsSubsumsjon",
            "regel",
            0,
            0)

        val behov = SubsumsjonsBehov(
            "",
            "12345",
            Random().nextInt(),
            LocalDate.now(),
            grunnlag = 1000,
            satsResultat = satsResultat
        )
        val behovJson = jsonAdapter.toJson(behov)

        TopologyTestDriver(consumer.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(behovJson)
            topologyTestDriver.pipeInput(inputRecord)
        }
/*
        assertNotNull(satsSubsumsjonerDummy.storedSatsSubsumsjon)
        assertEquals(
            "satsSubsumsjon",
            satsSubsumsjonerDummy.storedSatsSubsumsjon!!.subsumsjonsId)
            */
    }
}