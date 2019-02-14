package no.nav.dagpenger.regel.api

import no.nav.dagpenger.regel.api.grunnlag.GrunnlagSubsumsjonerDummy
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektSubsumsjonerDummy
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
import java.time.LocalDate
import java.util.Properties
import java.util.Random

class KafkaDagpengerBehovConsumerTest {
    val jsonAdapter = moshiInstance.adapter(SubsumsjonsBehov::class.java)

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
            inntekt = Inntekt("", 0),
            minsteinntektResultat = minsteinntektResultat
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
            inntekt = Inntekt("", 0),
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
            inntekt = Inntekt("", 0),
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
            0)

        val behov = SubsumsjonsBehov(
            TasksDummy.satsPendingBehovId,
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

        assertNotNull(satsSubsumsjonerDummy.storedSatsSubsumsjon)
        assertEquals(
            "satsSubsumsjon",
            satsSubsumsjonerDummy.storedSatsSubsumsjon!!.subsumsjonsId)
    }
}