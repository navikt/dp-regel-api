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
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringSerializer

private val LOGGER = KotlinLogging.logger {}

class KafkaVilkårProducer(env: Environment) {

    val clientId = "dp-regel-api"

    val kafkaConfig = mapOf(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to env.bootstrapServersUrl,
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.name,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java.name,
        ProducerConfig.CLIENT_ID_CONFIG to clientId,
        SaslConfigs.SASL_MECHANISM to "PLAIN",
        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SASL_PLAINTEXT",
        SaslConfigs.SASL_JAAS_CONFIG to "org.apache.kafka.common.security.plain.PlainLoginModule required username=igroup password=itest;",
        AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to "http://localhost:8081"
    )

    val kafkaProducer = KafkaProducer<String, Vilkår>(kafkaConfig)

    fun produceMinsteInntektEvent(request: MinsteinntektBeregningsRequest) {
        val parametere = mapRequestToParametere(request)

        val vilkår = Vilkår(
            "id",
            request.aktorId,
            request.vedtakId.toString(),
            listOf(Regel(RegelType.FIRE_FIRE, null, parametere)),
            null
        )
        produceEvent(vilkår)
    }

    fun produceEvent(vilkår: Vilkår) {
        LOGGER.info { "Producing Vilkårevent" }
        val record: RecordMetadata = kafkaProducer.send(
            ProducerRecord("dagpenger", "key", vilkår)
        ).get()
        LOGGER.info { "Produced -> ${record.topic()}  to offset ${record.offset()}" }
    }

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
