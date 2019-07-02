package no.nav.dagpenger.regel.api.streams

import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.plain.defaultProducerConfig
import no.nav.dagpenger.plain.producerConfig
import no.nav.dagpenger.regel.api.APPLICATION_NAME
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.models.Behov
import no.nav.dagpenger.streams.Topics.DAGPENGER_BEHOV_PACKET_EVENT
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import java.util.Properties
import java.util.concurrent.Future

private val LOGGER = KotlinLogging.logger {}

internal interface DagpengerBehovProducer {
    fun produceEvent(behov: Behov): Future<RecordMetadata>
}

internal fun kafkaProperties(config: Configuration) =
    producerConfig(
        clientId = APPLICATION_NAME,
        bootstrapServers = config.kafka.brokers,
        credential = config.kafka.credential(),
        properties = defaultProducerConfig.apply {
            putAll(listOf(
                ProducerConfig.ACKS_CONFIG to "all",
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
                ProducerConfig.RETRIES_CONFIG to Int.MAX_VALUE.toString(),
                ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to "5", // kafka 2.0 >= 1.1 so we can keep this as 5 instead of 1
                ProducerConfig.COMPRESSION_TYPE_CONFIG to "snappy",
                ProducerConfig.LINGER_MS_CONFIG to "20",
                ProducerConfig.BATCH_SIZE_CONFIG to 32.times(1024).toString() // 32Kb (default is 16 Kb)
            ))
        }
    )

internal class KafkaDagpengerBehovProducer(producerProperties: Properties) : DagpengerBehovProducer {

    private val kafkaProducer = KafkaProducer<String, Packet>(producerProperties)

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
