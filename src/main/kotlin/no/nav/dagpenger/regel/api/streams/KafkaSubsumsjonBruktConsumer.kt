package no.nav.dagpenger.regel.api.streams

import mu.KotlinLogging
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.db.EksternSubsumsjonBrukt
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import no.nav.dagpenger.streams.KafkaAivenCredentials
import no.nav.dagpenger.streams.Topic
import no.nav.dagpenger.streams.consumeTopic
import no.nav.dagpenger.streams.streamConfigAiven
import org.apache.kafka.common.errors.TopicAuthorizationException
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
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

    fun start() = streams.start().also { LOGGER.info { "Starting up ${config.application.id} kafka consumer" } }

    fun stop() = with(streams) {
        close(Duration.ofSeconds(3))
        cleanUp()
    }.also {
        LOGGER.info { "Shutting down ${config.application.id} kafka consumer" }
    }

    override fun status(): HealthStatus =
        when (streams.state()) {
            KafkaStreams.State.ERROR -> HealthStatus.DOWN
            KafkaStreams.State.PENDING_SHUTDOWN -> HealthStatus.DOWN
            else -> HealthStatus.UP
        }

    private val streams: KafkaStreams by lazy {
        KafkaStreams(buildTopology(), getConfig()).apply {
            setUncaughtExceptionHandler { t, e ->
                logUnexpectedError(t, e)
                stop()
            }
        }
    }

    private fun getConfig() = streamConfigAiven(
        appId = SERVICE_APP_ID,
        bootStapServerUrl = config.kafka.aivenBrokers,
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
            .foreach { _, bruktSubsumsjon -> bruktSubsumsjonStrategy.handle(bruktSubsumsjon) }
        return builder.build()
    }

    private fun logUnexpectedError(t: Thread?, e: Throwable) {
        when (e) {
            is TopicAuthorizationException -> LOGGER.warn(
                "TopicAuthorizationException in $SERVICE_APP_ID stream, stopping app"
            )
            else -> LOGGER.error(
                "Uncaught exception in $SERVICE_APP_ID stream, thread: $t message:  ${e.message}",
                e
            )
        }
    }
}
