package no.nav.dagpenger.regel.api

import de.huxhorn.sulky.ulid.ULID
import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.regel.api.grunnlag.GrunnlagRequestParametere
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektRequestParametere
import no.nav.dagpenger.regel.api.periode.PeriodeRequestParametere
import no.nav.dagpenger.regel.api.sats.SatsRequestParametere
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.PacketSerializer
import no.nav.dagpenger.streams.Topics.DAGPENGER_BEHOV_PACKET_EVENT
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
    private val clientId = "dp-regel-api"
    private val ulidGenerator = ULID()

    private val kafkaConfig = Properties().apply {
        putAll(
            listOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to env.bootstrapServersUrl,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.name,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to PacketSerializer::class.java.name,
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

    private val kafkaProducer = KafkaProducer<String, Packet>(kafkaConfig)

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            LOGGER.info("Closing $clientId Kafka producer")
            kafkaProducer.flush()
            kafkaProducer.close()
            LOGGER.info("done! ")
        })
    }

    override fun produceMinsteInntektEvent(request: MinsteinntektRequestParametere): String {
        val behovId = ulidGenerator.nextULID()
        val senesteInntektsmåned = senesteInntektsmåned(request.beregningsdato)
        val packet = mapRequestToPacket(request, behovId, senesteInntektsmåned)
        produceEvent(packet, behovId)
        return behovId
    }

    override fun producePeriodeEvent(request: PeriodeRequestParametere): String {
        val behovId = ulidGenerator.nextULID()
        val senesteInntektsmåned = senesteInntektsmåned(request.beregningsdato)
        val packet = mapRequestToPacket(request, behovId, senesteInntektsmåned)
        produceEvent(packet, behovId)
        return behovId
    }

    override fun produceGrunnlagEvent(request: GrunnlagRequestParametere): String {
        val behovId = ulidGenerator.nextULID()
        val senesteInntektsmåned = senesteInntektsmåned(request.beregningsdato)
        val packet = mapRequestToPacket(request, behovId, senesteInntektsmåned)
        produceEvent(packet, behovId)
        return behovId
    }

    override fun produceSatsEvent(request: SatsRequestParametere): String {
        val behovId = ulidGenerator.nextULID()
        val senesteInntektsmåned = senesteInntektsmåned(request.beregningsdato)
        val packet = mapRequestToPacket(request, behovId, senesteInntektsmåned)
        produceEvent(packet, behovId)
        return behovId
    }

    private fun produceEvent(packet: Packet, key: String) {
        LOGGER.info { "Producing dagpenger behov ${packet.toJson()}" }
        kafkaProducer.send(
            ProducerRecord(DAGPENGER_BEHOV_PACKET_EVENT.name, key, packet)
        ) { metadata, exception ->
            exception?.let { LOGGER.error { "Failed to produce dagpenger behov" } }
            metadata?.let { LOGGER.info { "Produced -> ${metadata.topic()}  to offset ${metadata.offset()}" } }
        }
    }

    private fun mapRequestToPacket(
        request: MinsteinntektRequestParametere,
        behovId: String,
        senesteInntektsmåned: YearMonth
    ) = Packet("").also {
        it.putValue("behovId", behovId)
        it.putValue("aktørId", request.aktorId)
        it.putValue("vedtakId", request.vedtakId)
        it.putValue("beregningsDato", request.beregningsdato)
        it.putValue("harAvtjentVerneplikt", request.harAvtjentVerneplikt)
        it.putValue("senesteInntektsmåned", senesteInntektsmåned)
        request.bruktInntektsPeriode?.let { bruktInntekt ->
            it.putValue(
                "bruktInntektsPeriode",
                BruktInntektsPeriode(bruktInntekt.førsteMåned, bruktInntekt.sisteMåned)
            )
        }
    }

    private fun mapRequestToPacket(
        request: PeriodeRequestParametere,
        behovId: String,
        senesteInntektsmåned: YearMonth
    ) = Packet("").also {
        it.putValue("behovId", behovId)
        it.putValue("aktørId", request.aktorId)
        it.putValue("vedtakId", request.vedtakId)
        it.putValue("beregningsDato", request.beregningsdato)
        it.putValue("harAvtjentVerneplikt", request.harAvtjentVerneplikt)
        it.putValue("senesteInntektsmåned", senesteInntektsmåned)
        request.bruktInntektsPeriode?.let { bruktInntekt ->
            it.putValue(
                "bruktInntektsPeriode",
                BruktInntektsPeriode(bruktInntekt.førsteMåned, bruktInntekt.sisteMåned)
            )
        }
    }

    private fun mapRequestToPacket(
        request: GrunnlagRequestParametere,
        behovId: String,
        senesteInntektsmåned: YearMonth
    ) = Packet("").also {
        it.putValue("behovId", behovId)
        it.putValue("aktørId", request.aktorId)
        it.putValue("vedtakId", request.vedtakId)
        it.putValue("beregningsDato", request.beregningsdato)
        it.putValue("harAvtjentVerneplikt", request.harAvtjentVerneplikt)
        it.putValue("senesteInntektsmåned", senesteInntektsmåned)
        request.manueltGrunnlag?.let { manueltGrunnlag -> it.putValue("manueltGrunnlag", manueltGrunnlag) }
    }

    private fun mapRequestToPacket(
        request: SatsRequestParametere,
        behovId: String,
        senesteInntektsmåned: YearMonth
    ) = Packet("").also {
        it.putValue("behovId", behovId)
        it.putValue("aktørId", request.aktorId)
        it.putValue("vedtakId", request.vedtakId)
        it.putValue("beregningsDato", request.beregningsdato)
        it.putValue("senesteInntektsmåned", senesteInntektsmåned)
        it.putValue("antallBarn", request.antallBarn)
        request.manueltGrunnlag?.let { manueltGrunnlag -> it.putValue("manueltGrunnlag", manueltGrunnlag) }
    }
}
