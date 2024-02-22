package no.nav.dagpenger.regel.api.streams

import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.regel.api.models.InternBehov
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import no.nav.dagpenger.streams.KafkaAivenCredentials
import no.nav.dagpenger.streams.Topic
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties
import java.util.concurrent.Future

private val LOGGER = KotlinLogging.logger {}

internal fun producerConfig(
    appId: String,
    bootStapServerUrl: String,
    aivenCredentials: KafkaAivenCredentials? = null,
    keySerializer: String = StringSerializer::class.java.name,
    valueSerializer: String = StringSerializer::class.java.name,
): Properties {
    return Properties().apply {
        putAll(
            listOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootStapServerUrl,
                ProducerConfig.CLIENT_ID_CONFIG to appId,
                ProducerConfig.ACKS_CONFIG to "all",
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
                ProducerConfig.RETRIES_CONFIG to Int.MAX_VALUE.toString(),
                // kafka 2.0 >= 1.1 so we can keep this as 5 instead of 1
                ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to "5",
                ProducerConfig.COMPRESSION_TYPE_CONFIG to "snappy",
                ProducerConfig.LINGER_MS_CONFIG to "20",
                // 32Kb (default is 16 Kb)
                ProducerConfig.BATCH_SIZE_CONFIG to 32.times(1024).toString(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to keySerializer,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to valueSerializer,
            ),
        )

        aivenCredentials?.let {
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, it.securityProtocolConfig)
            put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, it.sslEndpointIdentificationAlgorithmConfig)
            put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, it.sslTruststoreTypeConfig)
            put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, it.sslKeystoreTypeConfig)
            put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, it.sslTruststoreLocationConfig)
            put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, it.sslTruststorePasswordConfig)
            put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, it.sslKeystoreLocationConfig)
            put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, it.sslKeystorePasswordConfig)
        }
    }
}

internal interface DagpengerBehovProducer {
    fun produceEvent(behov: InternBehov): Future<RecordMetadata>
}

internal class KafkaDagpengerBehovProducer(
    private val kafkaProps: Properties,
    private val regelTopic: Topic<String, Packet>,
) : DagpengerBehovProducer, HealthCheck {
    private val kafkaProducer =
        KafkaProducer<String, Packet>(kafkaProps, regelTopic.keySerde.serializer(), regelTopic.valueSerde.serializer())

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
            kafkaProducer.partitionsFor(regelTopic.name)
        } catch (e: KafkaException) {
            LOGGER.error(e) { "Failed Kafka health check getting partion info for ${regelTopic.name}" }
            return HealthStatus.DOWN
        }
        return HealthStatus.UP
    }

    override fun produceEvent(behov: InternBehov): Future<RecordMetadata> {
        return kafkaProducer.send(
            ProducerRecord(
                regelTopic.name,
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
