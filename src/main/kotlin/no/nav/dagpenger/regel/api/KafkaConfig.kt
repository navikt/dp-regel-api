package no.nav.dagpenger.regel.api

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.StreamsConfig.AT_LEAST_ONCE
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler
import java.lang.System.getenv
import java.time.Duration
import java.util.Properties

private val maxPollRecords = 50
private val maxPollIntervalMs = Duration.ofSeconds(60 + maxPollRecords * 2.toLong()).toMillis()

data class KafkaAivenCredentials(
    val securityProtocolConfig: String = SecurityProtocol.SSL.name,
    val sslEndpointIdentificationAlgorithmConfig: String = "",
    val sslTruststoreTypeConfig: String = "jks",
    val sslKeystoreTypeConfig: String = "PKCS12",
    val sslTruststoreLocationConfig: String = "/var/run/secrets/nais.io/kafka/client.truststore.jks",
    val sslTruststorePasswordConfig: String = System.getenv("KAFKA_CREDSTORE_PASSWORD"),
    val sslKeystoreLocationConfig: String = "/var/run/secrets/nais.io/kafka/client.keystore.p12",
    val sslKeystorePasswordConfig: String = sslTruststorePasswordConfig,
)

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

        this.credentials(aivenCredentials)
    }
}

fun streamConfigAiven(
    appId: String,
    bootStapServerUrl: String,
    stateDir: String? = null,
    aivenCredentials: KafkaAivenCredentials? = null,
    environmentConfiguration: EnvironmentConfiguration = EnvironmentConfiguration(),
): Properties {
    return Properties().apply {
        putAll(
            commonProperties(bootStapServerUrl, appId),
        )

        if (Profile.LOCAL != environmentConfiguration.profile) {
            put(StreamsConfig.REPLICATION_FACTOR_CONFIG, "2")
        }

        stateDir?.let { put(StreamsConfig.STATE_DIR_CONFIG, stateDir) }

        this.credentials(aivenCredentials)
    }
}

private fun Properties.credentials(aivenCredentials: KafkaAivenCredentials?) {
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

private fun commonProperties(
    bootStapServerUrl: String,
    appId: String,
) = listOf(
    CommonClientConfigs.RETRY_BACKOFF_MS_CONFIG to 1000,
    CommonClientConfigs.RECONNECT_BACKOFF_MS_CONFIG to 5000,
    CommonClientConfigs.MAX_POLL_INTERVAL_MS_CONFIG to "$maxPollIntervalMs",
    StreamsConfig.consumerPrefix(MAX_POLL_RECORDS_CONFIG) to maxPollRecords,
    StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to bootStapServerUrl,
    StreamsConfig.APPLICATION_ID_CONFIG to appId,
    StreamsConfig.COMMIT_INTERVAL_MS_CONFIG to 1,
    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
    StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG to LogAndFailExceptionHandler::class.java,
    StreamsConfig.producerPrefix(ProducerConfig.COMPRESSION_TYPE_CONFIG) to "snappy",
    StreamsConfig.producerPrefix(ProducerConfig.BATCH_SIZE_CONFIG) to
        32.times(1024)
            .toString(),
    // 32Kb (default is 16 Kb)
    // Increase max.request.size to 3 MB (default is 1MB )), messages should be compressed but there are currently a bug
    // in kafka-clients ref https://stackoverflow.com/questions/47696396/kafka-broker-is-not-gzipping-my-bigger-size-message-even-though-i-specified-co/48304851#48304851
    StreamsConfig.producerPrefix(ProducerConfig.MAX_REQUEST_SIZE_CONFIG) to 5.times(1024).times(1000).toString(),
    StreamsConfig.PROCESSING_GUARANTEE_CONFIG to AT_LEAST_ONCE,
)

private val localProperties =
    ConfigurationMap(
        mapOf(
            "application.profile" to "LOCAL",
        ),
    )
private val devProperties =
    ConfigurationMap(
        mapOf(
            "application.profile" to "DEV",
        ),
    )
private val prodProperties =
    ConfigurationMap(
        mapOf(
            "application.profile" to "PROD",
        ),
    )

data class EnvironmentConfiguration(
    val profile: Profile = config()[Key("application.profile", stringType)].let { Profile.valueOf(it) },
)

enum class Profile {
    LOCAL,
    DEV,
    PROD,
}

private fun config() =
    when (getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
        "dev-gcp" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding devProperties
        "prod-gcp" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding prodProperties
        else -> {
            ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding localProperties
        }
    }
