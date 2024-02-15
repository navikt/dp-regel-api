package no.nav.dagpenger.regel.api.streams

import mu.KotlinLogging
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.db.EksternSubsumsjonBrukt
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import no.nav.dagpenger.regel.api.serder.jacksonObjectMapper
import no.nav.dagpenger.streams.KafkaAivenCredentials
import no.nav.dagpenger.streams.Topic
import no.nav.dagpenger.streams.consumeTopic
import no.nav.dagpenger.streams.streamConfigAiven
import org.apache.kafka.common.errors.TopicAuthorizationException
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.kstream.Produced
import java.time.Duration

private val LOGGER = KotlinLogging.logger {}

internal class KafkaSubsumsjonBruktConsumer(
    private val config: Configuration,
    private val bruktSubsumsjonStrategy: BruktSubsumsjonStrategy
) : HealthCheck {
    private val SERVICE_APP_ID = "kafka-subsumsjonbrukt-v1"
    val subsumsjonBruktTopic = Topic(
        config.subsumsjonBruktTopic,
        keySerde = Serdes.StringSerde(),
        valueSerde = Serdes.StringSerde()
    )

    fun start() = streams.start().also { LOGGER.info { "Starting up $SERVICE_APP_ID kafka consumer" } }

    fun stop() = with(streams) {
        close(Duration.ofSeconds(3))
        cleanUp()
    }.also {
        LOGGER.info { "Shutting down $SERVICE_APP_ID kafka consumer" }
    }

    override fun status(): HealthStatus =
        when (streams.state()) {
            KafkaStreams.State.ERROR -> HealthStatus.DOWN
            KafkaStreams.State.PENDING_SHUTDOWN -> HealthStatus.DOWN
            else -> HealthStatus.UP
        }

    private val streams: KafkaStreams by lazy {
        KafkaStreams(buildTopology(), getConfig()).apply {
            setUncaughtExceptionHandler { exc ->
                logUnexpectedError(exc)
                stop()
                StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_CLIENT
            }
        }
    }

    private fun getConfig() = streamConfigAiven(
        appId = SERVICE_APP_ID,
        bootStapServerUrl = config.aivenBrokers,
        aivenCredentials = KafkaAivenCredentials()
    )

    internal fun buildTopology(): Topology {
        val builder = StreamsBuilder()
        val stream = builder.consumeTopic(
            Topic(
                config.subsumsjonBruktTopic,
                keySerde = Serdes.StringSerde(),
                valueSerde = Serdes.StringSerde()
            )
        )
        stream
            .mapValues { _, value -> EksternSubsumsjonBrukt.fromJson(value) }
            .filterNot { _, bruktSubsumsjon ->
                "AVSLU" == bruktSubsumsjon.vedtakStatus && "AVBRUTT" == bruktSubsumsjon.utfall
            }
            .mapValues { _, bruktSubsumsjon -> bruktSubsumsjonStrategy.handle(bruktSubsumsjon) }
            .filterNot { _, value -> value == null }
            .mapValues { _, faktum ->
                jacksonObjectMapper.writeValueAsString(
                    mapOf(
                        "@event_name" to "brukt_inntekt",
                        "inntektsId" to faktum?.inntektsId,
                        "aktorId" to faktum?.aktorId,
                        "kontekst" to faktum?.regelkontekst,
                    )
                )
            }
            .to(config.inntektBruktTopic, Produced.with(Serdes.StringSerde(), Serdes.StringSerde()))
        return builder.build()
    }

    private fun logUnexpectedError(e: Throwable) {
        when (e) {
            is TopicAuthorizationException -> LOGGER.warn(
                "TopicAuthorizationException in $SERVICE_APP_ID stream, stopping app"
            )
            else -> LOGGER.error(
                "Uncaught exception in $SERVICE_APP_ID stream, thread: ${Thread.currentThread()} message:  ${e.message}",
                e
            )
        }
    }
}
