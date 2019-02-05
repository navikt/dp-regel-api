package no.nav.dagpenger.regel.api

import mu.KotlinLogging
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektBeregning
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektBeregninger
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektResultat
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektResultatParametere
import no.nav.dagpenger.regel.api.tasks.Tasks
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.Topics
import no.nav.dagpenger.streams.streamConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import java.time.LocalDateTime
import java.util.Properties
import java.util.concurrent.TimeUnit

private val LOGGER = KotlinLogging.logger {}

class KafkaDagpengerBehovConsumer(
    val env: Environment,
    val tasks: Tasks,
    val minsteinntektBeregninger: MinsteinntektBeregninger
) {

    val SERVICE_APP_ID = "dp-regel-api"
    private lateinit var streams: KafkaStreams
    fun start() {
        LOGGER.info { "Starting up $SERVICE_APP_ID kafca consumer" }
        streams = KafkaStreams(buildTopology(), this.getConfig())
        streams.setUncaughtExceptionHandler { t, e -> System.exit(0) }
        streams.start()
    }

    fun stop() {
        LOGGER.info { "Shutting down $SERVICE_APP_ID kafka consumer" }
        streams.close(3, TimeUnit.SECONDS)
        streams.cleanUp()
    }

    internal fun buildTopology(): Topology {
        val builder = StreamsBuilder()

        val stream = builder.stream(
            Topics.DAGPENGER_BEHOV_EVENT.name,
            Consumed.with(Serdes.StringSerde(), Serdes.serdeFrom(JsonSerializer(), JsonDeserializer()))
        )

        stream
            .peek { key, value -> LOGGER.info("Processing behov with id ${value.behovId}") }
            .foreach { _, behov -> storeResult(behov) }

        return builder.build()
    }

    fun getConfig(): Properties {
        val props = streamConfig(
            appId = SERVICE_APP_ID,
            bootStapServerUrl = env.bootstrapServersUrl,
            credential = KafkaCredential(env.username, env.password))
        return props
    }

    fun hasNeededMinsteinntektResult(behov: SubsumsjonsBehov): Boolean {
        return behov.minsteinntektSubsumsjon != null && tasks.getTask(Regel.MINSTEINNTEKT, behov.behovId) != null
    }

    fun storeResult(behov: SubsumsjonsBehov) {
        when {
            hasNeededMinsteinntektResult(behov) -> storeMinsteinntektBeregning(behov)
            else -> LOGGER.info("Ignoring behov with id ${behov.behovId}")
        }
    }

    fun storeMinsteinntektBeregning(behov: SubsumsjonsBehov) {
        val minsteinntektBeregning = mapToMinsteinntektBeregning(behov)

        minsteinntektBeregninger.setMinsteinntektBeregning(minsteinntektBeregning)

        val task = tasks.updateTask(Regel.MINSTEINNTEKT, behov.behovId, minsteinntektBeregning.subsumsjonsId)

        LOGGER.info("Updated task with id ${task.taskId}")
    }

    fun mapToMinsteinntektBeregning(behov: SubsumsjonsBehov): MinsteinntektBeregning {
        val minsteinntektSubsumsjon = behov.minsteinntektSubsumsjon!!
        return MinsteinntektBeregning(
            minsteinntektSubsumsjon.subsumsjonsId,
            LocalDateTime.now(),
            LocalDateTime.now(),
            MinsteinntektResultatParametere(behov.aktørId, behov.vedtakId, behov.beregningsDato),
            MinsteinntektResultat(minsteinntektSubsumsjon.oppfyllerMinsteinntektKrav),
            setOf(
                Inntekt(
                    inntekt = 0,
                    periode = 1,
                    inneholderNaeringsinntekter = false,
                    andel = 39982
                ),
                Inntekt(
                    inntekt = 0,
                    periode = 2,
                    inneholderNaeringsinntekter = false,
                    andel = 39982
                ),
                Inntekt(
                    inntekt = 0,
                    periode = 3,
                    inneholderNaeringsinntekter = false,
                    andel = 39982
                )

            )
        )
    }
}