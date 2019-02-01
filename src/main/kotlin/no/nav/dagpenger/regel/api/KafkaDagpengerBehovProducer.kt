package no.nav.dagpenger.regel.api

import de.huxhorn.sulky.ulid.ULID
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektParametere
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.Topics
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import java.io.File
import java.util.Properties

private val LOGGER = KotlinLogging.logger {}

class KafkaDagpengerBehovProducer(env: Environment) : VilkårProducer {

    val jsonAdapter = moshiInstance.adapter(SubsumsjonsBehov::class.java)

    val clientId = "dp-regel-api"

    val kafkaConfig = Properties().apply {
        putAll(
            listOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to env.bootstrapServersUrl,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.name,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.name,
                ProducerConfig.CLIENT_ID_CONFIG to clientId
            )
        )

        val credential = KafkaCredential(env.username, env.password)

        credential.let { credential ->
            LOGGER.info { "Using user name ${credential.username} to authenticate against Kafka brokers " }
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${credential.username}\" password=\"${credential.password}\";")

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

    val kafkaProducer = KafkaProducer<String, String>(kafkaConfig)

    override fun produceMinsteInntektEvent(request: MinsteinntektParametere) {
        val behov = mapRequestToBehov(request)
        val behovId = ULID()

        produceEvent(behov, behovId.toString())
    }

    fun produceEvent(behov: SubsumsjonsBehov, key: String) {
        val behovJson = jsonAdapter.toJson(behov)
        LOGGER.info { "Producing Vilkårevent" }
        kafkaProducer.send(
            ProducerRecord(Topics.DAGPENGER_BEHOV_EVENT.name, key, behovJson)
        ) { metadata, exception ->
            exception?.let { LOGGER.error { "Failed to produce vilkår" } }
            metadata?.let { LOGGER.info { "Produced -> ${metadata.topic()}  to offset ${metadata.offset()}" } }
        }
    }

    fun close() = kafkaProducer.close()

    fun mapRequestToBehov(request: MinsteinntektParametere): SubsumsjonsBehov =
        SubsumsjonsBehov(
            request.aktorId,
            request.vedtakId,
            request.beregningsdato)
}
