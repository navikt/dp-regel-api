package no.nav.dagpenger.regel.api.streams

import mu.KotlinLogging
import no.nav.dagpenger.regel.api.PacketSerializer
import no.nav.dagpenger.regel.api.models.InternBehov
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties
import java.util.concurrent.Future

private val LOGGER = KotlinLogging.logger {}

internal interface DagpengerBehovProducer {
    fun produceEvent(behov: InternBehov): Future<RecordMetadata>
}

internal class KafkaDagpengerBehovProducer(
    kafkaProps: Properties,
    private val regelTopic: String,
) : DagpengerBehovProducer, HealthCheck {
    private val kafkaProducer =
        KafkaProducer(kafkaProps, StringSerializer(), PacketSerializer())

    init {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                LOGGER.info("Closing dp-regel-api Kafka producer")
                kafkaProducer.flush()
                kafkaProducer.close()
                LOGGER.info("done! ")
            },
        )
    }

    override fun status(): HealthStatus {
        try {
            kafkaProducer.partitionsFor(regelTopic)
        } catch (e: KafkaException) {
            LOGGER.error(e) { "Failed Kafka health check getting partion info for $regelTopic" }
            return HealthStatus.DOWN
        }
        return HealthStatus.UP
    }

    override fun produceEvent(behov: InternBehov): Future<RecordMetadata> {
        return kafkaProducer.send(
            ProducerRecord(
                regelTopic,
                behov.behovId.id,
                // TODO: Use intern id as partition key instead, as it is unique per ektern id + kontekst
                InternBehov.toPacket(behov),
            ),
        ) { metadata, exception ->
            exception?.let { LOGGER.error { "Failed to produce dagpenger behov" } }
            metadata?.let {
                LOGGER.info {
                    "Produced dagpenger behov on topic ${metadata.topic()} to offset ${metadata.offset()}" +
                        " with the key ${behov.behovId}"
                }
            }
        }
    }
}
