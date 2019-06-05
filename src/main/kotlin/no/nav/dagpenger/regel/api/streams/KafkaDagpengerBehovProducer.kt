package no.nav.dagpenger.regel.api.streams

import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.regel.api.APPLICATION_NAME
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import no.nav.dagpenger.regel.api.models.Behov
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.Topics
import no.nav.dagpenger.streams.Topics.DAGPENGER_BEHOV_PACKET_EVENT
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import java.io.File
import java.util.Properties
import java.util.concurrent.Future

private val LOGGER = KotlinLogging.logger {}

internal fun producerConfig(
    appId: String,
    bootStapServerUrl: String,
    credential: KafkaCredential? = null
): Properties {
    return Properties().apply {
        putAll(
            listOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootStapServerUrl,
                ProducerConfig.CLIENT_ID_CONFIG to appId,
                ProducerConfig.ACKS_CONFIG to "all",
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
                ProducerConfig.RETRIES_CONFIG to Int.MAX_VALUE.toString(),
                ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to "5", // kafka 2.0 >= 1.1 so we can keep this as 5 instead of 1
                ProducerConfig.COMPRESSION_TYPE_CONFIG to "snappy",
                ProducerConfig.LINGER_MS_CONFIG to "20",
                ProducerConfig.BATCH_SIZE_CONFIG to 32.times(1024).toString() // 32Kb (default is 16 Kb)
            )
        )

        credential?.let { credential ->
            LOGGER.info { "Using user name ${credential.username} to authenticate against Kafka brokers " }
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(
                SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${credential.username}\" password=\"${credential.password}\";"
            )

            val trustStoreLocation = System.getenv("NAV_TRUSTSTORE_PATH")
            trustStoreLocation?.let {
                try {
                    put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
                    put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, File(it).absolutePath)
                    put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, System.getenv("NAV_TRUSTSTORE_PASSWORD"))
                    LOGGER.info { "Configured '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location " }
                } catch (e: Exception) {
                    LOGGER.error { "Failed to set '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location " }
                }
            }
        }
    }
}

internal interface DagpengerBehovProducer {
    fun produceEvent(behov: Behov): Future<RecordMetadata>
}

internal class KafkaDagpengerBehovProducer(kafkaProps: Properties) : DagpengerBehovProducer, HealthCheck {

    private val kafkaProducer = KafkaProducer<String, Packet>(kafkaProps, Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.serializer(), Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.serializer())

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            LOGGER.info("Closing $APPLICATION_NAME Kafka producer")
            kafkaProducer.flush()
            kafkaProducer.close()
            LOGGER.info("done! ")
        })
    }

    override fun status(): HealthStatus {
        try {
            kafkaProducer.partitionsFor(DAGPENGER_BEHOV_PACKET_EVENT.name)
        } catch (e: KafkaException) {
            LOGGER.error(e) { "Failed Kafka health check getting partion info for ${DAGPENGER_BEHOV_PACKET_EVENT.name}" }
            return HealthStatus.DOWN
        }
        return HealthStatus.UP
    }

    override fun produceEvent(behov: Behov): Future<RecordMetadata> {
        return kafkaProducer.send(
            ProducerRecord(DAGPENGER_BEHOV_PACKET_EVENT.name, behov.behovId, Behov.toPacket(behov))
        ) { metadata, exception ->
            exception?.let { LOGGER.error { "Failed to produce dagpenger behov" } }
            metadata?.let { LOGGER.info { "Produced -> ${metadata.topic()}  to offset ${metadata.offset()}" } }
        }
    }
}
