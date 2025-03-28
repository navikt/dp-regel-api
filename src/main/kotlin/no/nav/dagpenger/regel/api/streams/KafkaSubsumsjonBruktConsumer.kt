package no.nav.dagpenger.regel.api.streams

import de.huxhorn.sulky.ulid.ULID
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.KafkaAivenCredentials
import no.nav.dagpenger.regel.api.db.EksternSubsumsjonBrukt
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import no.nav.dagpenger.regel.api.serder.jacksonObjectMapper
import no.nav.dagpenger.regel.api.streamConfigAiven
import org.apache.kafka.common.errors.TopicAuthorizationException
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import java.time.Duration

private val LOGGER = KotlinLogging.logger {}

internal class KafkaSubsumsjonBruktConsumer(
    private val config: Configuration,
    private val bruktSubsumsjonStrategy: BruktSubsumsjonStrategy,
) : HealthCheck {
    private val serviceAppId = "kafka-subsumsjonbrukt-v1"

    fun start() = streams.start().also { LOGGER.info { "Starting up $serviceAppId kafka consumer" } }

    fun stop() =
        with(streams) {
            close(Duration.ofSeconds(3))
            cleanUp()
        }.also {
            LOGGER.info { "Shutting down $serviceAppId kafka consumer" }
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

    private fun getConfig() =
        streamConfigAiven(
            appId = serviceAppId,
            bootStapServerUrl = config.aivenBrokers,
            aivenCredentials = KafkaAivenCredentials(),
        )

    internal fun buildTopology(): Topology {
        val builder = StreamsBuilder()

        val stream =
            builder.stream(
                config.subsumsjonBruktTopic,
                Consumed.with(
                    Serdes.StringSerde(),
                    Serdes.StringSerde(),
                ),
            )
        stream
            .mapValues { _, value -> EksternSubsumsjonBrukt.fromJson(value) }
            .filterNot { _, bruktSubsumsjon ->
                "AVSLU" == bruktSubsumsjon.vedtakStatus && "AVBRUTT" == bruktSubsumsjon.utfall
            }
            .filter { _, bruktSubsumsjon ->
                kotlin.runCatching { ULID.parseULID(bruktSubsumsjon.id) }.isSuccess.also {
                    if (!it) {
                        LOGGER.warn { "Kunne ikke lese $bruktSubsumsjon -> ID er ikke ULID og ikke laget av dp-regel" }
                    }
                }
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
                    ),
                )
            }
            .to(config.inntektBruktTopic, Produced.with(Serdes.StringSerde(), Serdes.StringSerde()))
        return builder.build()
    }

    private fun logUnexpectedError(e: Throwable) {
        when (e) {
            is TopicAuthorizationException ->
                LOGGER.warn(
                    "TopicAuthorizationException in $serviceAppId stream, stopping app",
                )
            else ->
                LOGGER.error(
                    "Uncaught exception in $serviceAppId stream, thread: ${Thread.currentThread()} message:  ${e.message}",
                    e,
                )
        }
    }
}
