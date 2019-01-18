package no.nav.dagpenger.regel.api

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import mu.KotlinLogging
import no.nav.dagpenger.events.avro.BruktInntektsperiode
import no.nav.dagpenger.events.avro.MinsteinntektParametere
import no.nav.dagpenger.events.avro.Regel
import no.nav.dagpenger.events.avro.RegelType
import no.nav.dagpenger.events.avro.Vilkår
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektBeregningsRequest
import no.nav.dagpenger.streams.Topics
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.Callback
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import java.io.File
import java.util.UUID

private val LOGGER = KotlinLogging.logger {}

class KafkaVilkårProducer(env: Environment) : VilkårProducer {

    val clientId = "dp-regel-api"

    val kafkaConfig = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to env.bootstrapServersUrl,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.name,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java.name,
            ProducerConfig.CLIENT_ID_CONFIG to clientId,
            SaslConfigs.SASL_MECHANISM to "PLAIN",
            SaslConfigs.SASL_JAAS_CONFIG to "org.apache.kafka.common.security.plain.PlainLoginModule required username=${env.username} password=${env.password};",
            AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to env.schemaRegistryUrl,
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SASL_SSL",
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to File(env.trustStorePath).absolutePath,
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to env.trustStorePassword
    )

    val kafkaProducer = KafkaProducer<String, Vilkår>(kafkaConfig)

    override fun produceMinsteInntektEvent(request: MinsteinntektBeregningsRequest) {
        val parametere = mapRequestToParametere(request)

        val vilkårId = UUID.randomUUID().toString()

        val vilkår = Vilkår(
                vilkårId,
                request.aktorId,
                request.vedtakId.toString(),
                listOf(Regel(RegelType.FIRE_FIRE, null, null, parametere)),
                null
        )
        produceEvent(vilkår, vilkårId)
    }

    fun produceEvent(vilkår: Vilkår, key: String) {
        LOGGER.info { "Producing Vilkårevent" }
        kafkaProducer.send(
                ProducerRecord(Topics.VILKÅR_EVENT.name, key, vilkår)
        ) { metadata, exception ->
            exception?.let { LOGGER.error { "Failed to produce vilkår" } }
            metadata?.let { LOGGER.info { "Produced -> ${metadata.topic()}  to offset ${metadata.offset()}" } }
        }
    }

    //LOGGER.info {  }
    fun close() = kafkaProducer.close()

    fun mapRequestToParametere(request: MinsteinntektBeregningsRequest): MinsteinntektParametere =
            MinsteinntektParametere(
                    request.aktorId,
                    request.vedtakId.toString(),
                    request.beregningsdato,
                    request.inntektsId,
                    BruktInntektsperiode(request.bruktinntektsPeriode.foersteMaaned, request.bruktinntektsPeriode.sisteMaaned),
                    request.harAvtjentVerneplikt,
                    request.oppfyllerKravTilFangstOgFisk,
                    request.harArbeidsperiodeEosSiste12Maaneder
            )
}
