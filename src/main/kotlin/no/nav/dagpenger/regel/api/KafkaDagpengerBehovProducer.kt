package no.nav.dagpenger.regel.api

import de.huxhorn.sulky.ulid.ULID
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.grunnlag.GrunnlagRequestParametere
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektRequestParametere
import no.nav.dagpenger.regel.api.periode.PeriodeRequestParametere
import no.nav.dagpenger.regel.api.sats.SatsRequestParametere
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
import java.time.YearMonth
import java.util.Properties

private val LOGGER = KotlinLogging.logger {}

class KafkaDagpengerBehovProducer(env: Environment) : DagpengerBehovProducer {
    private val jsonAdapter = moshiInstance.adapter(SubsumsjonsBehov::class.java)
    private val clientId = "dp-regel-api"
    private val ulidGenerator = ULID()

    private val kafkaConfig = Properties().apply {
        putAll(
            listOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to env.bootstrapServersUrl,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.name,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.name,
                ProducerConfig.CLIENT_ID_CONFIG to clientId,
                ProducerConfig.ACKS_CONFIG to "all",
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
                ProducerConfig.RETRIES_CONFIG to Int.MAX_VALUE.toString(),
                ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to "5", // kafka 2.0 >= 1.1 so we can keep this as 5 instead of 1
                ProducerConfig.COMPRESSION_TYPE_CONFIG to "snappy",
                ProducerConfig.LINGER_MS_CONFIG to "20",
                ProducerConfig.BATCH_SIZE_CONFIG to 32.times(1024).toString() // 32Kb (default is 16 Kb)
            )
        )

        val kafkaCredential = KafkaCredential(env.username, env.password)

        kafkaCredential.let { credential ->
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

    private val kafkaProducer = KafkaProducer<String, String>(kafkaConfig)

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            LOGGER.info("Closing $clientId Kafka producer")
            kafkaProducer.flush()
            kafkaProducer.close()
            LOGGER.info("done! ")
        })
    }

    override fun produceMinsteInntektEvent(request: MinsteinntektRequestParametere): SubsumsjonsBehov {
        val behovId = ulidGenerator.nextULID()
        val senesteInntektsmåned = YearMonth.of(request.beregningsdato.year, request.beregningsdato.month)
        val behov = mapRequestToBehov(request, behovId, senesteInntektsmåned)
        produceEvent(behov, behovId)

        return behov
    }

    override fun producePeriodeEvent(request: PeriodeRequestParametere): SubsumsjonsBehov {
        val behovId = ulidGenerator.nextULID()
        val behov = mapRequestToBehov(request, behovId)
        produceEvent(behov, behovId)

        return behov
    }

    override fun produceGrunnlagEvent(request: GrunnlagRequestParametere): SubsumsjonsBehov {
        val behovId = ulidGenerator.nextULID()
        val behov = mapRequestToBehov(request, behovId)
        produceEvent(behov, behovId)

        return behov
    }

    override fun produceSatsEvent(request: SatsRequestParametere): SubsumsjonsBehov {
        val behovId = ulidGenerator.nextULID()
        val behov = mapRequestToBehov(request, behovId)
        produceEvent(behov, behovId)

        return behov
    }

    fun produceEvent(behov: SubsumsjonsBehov, key: String) {
        val behovJson = jsonAdapter.toJson(behov)
        LOGGER.info { "Producing dagpenger behov $behovJson" }
        kafkaProducer.send(
            ProducerRecord(Topics.DAGPENGER_BEHOV_EVENT.name, key, behovJson)
        ) { metadata, exception ->
            exception?.let { LOGGER.error { "Failed to produce dagpenger behov" } }
            metadata?.let { LOGGER.info { "Produced -> ${metadata.topic()}  to offset ${metadata.offset()}" } }
        }
    }

    fun mapRequestToBehov(
        request: MinsteinntektRequestParametere,
        behovId: String,
        senesteInntektsmåned: YearMonth): SubsumsjonsBehov {

        val bruktInntektsPeriode =
            if (request.bruktInntektsPeriode != null)
                BruktInntektsPeriode(request.bruktInntektsPeriode.førsteMåned, request.bruktInntektsPeriode.sisteMåned)
            else null

        return SubsumsjonsBehov(
            behovId,
            request.aktorId,
            request.vedtakId,
            request.beregningsdato,
            request.harAvtjentVerneplikt,
            senesteInntektsmåned = senesteInntektsmåned,
            bruktInntektsPeriode = bruktInntektsPeriode
        )
    }


    fun mapRequestToBehov(request: PeriodeRequestParametere, behovId: String): SubsumsjonsBehov {

        val bruktInntektsPeriode =
            if (request.bruktInntektsPeriode != null)
                BruktInntektsPeriode(request.bruktInntektsPeriode.førsteMåned, request.bruktInntektsPeriode.sisteMåned)
            else null


        return SubsumsjonsBehov(
            behovId,
            request.aktorId,
            request.vedtakId,
            request.beregningsdato,
            request.harAvtjentVerneplikt,
            bruktInntektsPeriode = bruktInntektsPeriode
        )
    }

    fun mapRequestToBehov(request: GrunnlagRequestParametere, behovId: String): SubsumsjonsBehov {

        val bruktInntektsPeriode =
            if (request.bruktInntektsPeriode != null)
                BruktInntektsPeriode(request.bruktInntektsPeriode.førsteMåned, request.bruktInntektsPeriode.sisteMåned)
            else null


        return SubsumsjonsBehov(
            behovId,
            request.aktorId,
            request.vedtakId,
            request.beregningsdato,
            request.harAvtjentVerneplikt,
            bruktInntektsPeriode = bruktInntektsPeriode
        )
    }

    fun mapRequestToBehov(request: SatsRequestParametere, behovId: String): SubsumsjonsBehov =
        SubsumsjonsBehov(
            behovId,
            request.aktorId,
            request.vedtakId,
            request.beregningsdato,
            antallBarn = request.antallBarn,
            grunnlag = request.grunnlag
        )
}
