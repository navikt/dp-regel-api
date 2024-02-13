package no.nav.dagpenger.regel.api.streams

import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.models.PacketKeys
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import no.nav.dagpenger.streams.KafkaAivenCredentials
import no.nav.dagpenger.streams.Pond
import no.nav.dagpenger.streams.Topic
import no.nav.dagpenger.streams.streamConfigAiven
import org.apache.kafka.common.errors.TopicAuthorizationException
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.kstream.Predicate
import java.time.Duration

private val LOGGER = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

internal class AivenKafkaSubsumsjonConsumer(
    private val config: Configuration,
    private val subsumsjonPond: SubsumsjonPond
) : HealthCheck {
    private val SERVICE_APP_ID = "dp-regel-api-sub-brukt-v1"

    private val streams: KafkaStreams by lazy {
        KafkaStreams(subsumsjonPond.buildTopology(), this.getConfig()).apply {
            setUncaughtExceptionHandler { exc ->
                logUnexpectedError(exc)
                stop()
                StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_CLIENT
            }
        }
    }

    fun start() = streams.start().also { LOGGER.info { "Starting up $SERVICE_APP_ID kafka consumer" } }

    fun stop() = with(streams) {
        close(Duration.ofSeconds(3))
        cleanUp()
    }.also {
        LOGGER.info { "Shutting down  $SERVICE_APP_ID kafka consumer" }
    }

    override fun status(): HealthStatus =
        when (streams.state()) {
            KafkaStreams.State.ERROR -> HealthStatus.DOWN
            KafkaStreams.State.PENDING_SHUTDOWN -> HealthStatus.DOWN
            else -> HealthStatus.UP
        }

    private fun getConfig() = streamConfigAiven(
        appId = SERVICE_APP_ID,
        bootStapServerUrl = config.kafka.aivenBrokers,
        aivenCredentials = KafkaAivenCredentials()
    )

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

internal class SubsumsjonPond(
    private val packetStrategies: List<SubsumsjonPacketStrategy>,
    config: Configuration,
    topic: Topic<String, Packet>
) : Pond(topic) {
    override val SERVICE_APP_ID: String = config.application.id

    override fun filterPredicates(): List<Predicate<String, Packet>> =
        listOf(
            Predicate { _, packet -> packet.hasField(PacketKeys.BEHOV_ID) },
            Predicate { _, packet -> !packet.hasProblem() }
        )

    override fun onPacket(packet: Packet) {
        sikkerlogg.info { "Mottok packet: ${packet.toJson()} Men gjør ingenting med den" }
//        packetStrategies.forEach { it.run(packet) }
    }
}
