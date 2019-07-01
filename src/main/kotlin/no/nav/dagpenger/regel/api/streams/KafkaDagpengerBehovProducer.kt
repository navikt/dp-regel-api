package no.nav.dagpenger.regel.api.streams

import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.regel.api.APPLICATION_NAME
import no.nav.dagpenger.regel.api.models.Behov
import no.nav.dagpenger.streams.Topics.DAGPENGER_BEHOV_PACKET_EVENT
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import java.util.Properties
import java.util.concurrent.Future

private val LOGGER = KotlinLogging.logger {}

internal interface DagpengerBehovProducer {
    fun produceEvent(behov: Behov): Future<RecordMetadata>
}

internal class KafkaDagpengerBehovProducer(kafkaProps: Properties) : DagpengerBehovProducer {

    private val kafkaProducer = KafkaProducer<String, Packet>(kafkaProps)

    fun stop() = with(kafkaProducer) {
        LOGGER.info("Closing $APPLICATION_NAME Kafka producer")
        flush()
        close()
        LOGGER.info("done! ")
    }

    override fun produceEvent(behov: Behov): Future<RecordMetadata> = kafkaProducer.send(
        ProducerRecord(DAGPENGER_BEHOV_PACKET_EVENT.name, behov.behovId, Behov.toPacket(behov))
    ) { metadata, exception ->
        exception?.let { LOGGER.error { "Failed to produce dagpenger behov" } }
        metadata?.let { LOGGER.info { "Produced dagpenger behov on topic ${metadata.topic()} to offset ${metadata.offset()} with the key ${behov.behovId}" } }
    }
}
