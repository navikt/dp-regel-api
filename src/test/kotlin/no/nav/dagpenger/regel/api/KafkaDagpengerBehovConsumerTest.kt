package no.nav.dagpenger.regel.api

import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektSubsumsjonerDummy
import no.nav.dagpenger.regel.api.tasks.TasksDummy
import no.nav.dagpenger.streams.Topics
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.junit.jupiter.api.Assertions.assertEquals
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
        val minsteinntektSubsumsjoner = MinsteinntektSubsumsjonerDummy()
        val tasks = TasksDummy()
        val consumer = KafkaDagpengerBehovConsumer(
            Environment(
                username = "bogus",
                password = "bogus"
            ),
            tasks,
            minsteinntektSubsumsjoner
        )

        val minsteinntektResultat = MinsteinntektResultat(
            "123",
            "id",
            "regel",
            true)

        val behov = SubsumsjonsBehov(
            TasksDummy.minsteinntektPendingTaskId,
            "12345",
            Random().nextInt(),
            LocalDate.now(),
            minsteinntektResultat = minsteinntektResultat
        )
        val behovJson = jsonAdapter.toJson(behov)

        TopologyTestDriver(consumer.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(behovJson)
            topologyTestDriver.pipeInput(inputRecord)
        }

        assertEquals("id", minsteinntektResultat.subsumsjonsId)
    }
}