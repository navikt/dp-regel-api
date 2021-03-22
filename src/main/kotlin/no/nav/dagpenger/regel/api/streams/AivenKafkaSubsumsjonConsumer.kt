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
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.kstream.Predicate
import java.time.Duration
import kotlin.system.exitProcess

private val LOGGER = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

internal class AivenKafkaSubsumsjonConsumer(
    private val config: Configuration,
    private val subsumsjonPond: SubsumsjonPond
) : HealthCheck {
    private val SERVICE_APP_ID = "dp-regel-api-sub-brukt"

    private val streams: KafkaStreams by lazy {
        KafkaStreams(subsumsjonPond.buildTopology(), this.getConfig()).apply {
            setUncaughtExceptionHandler { _, _ -> exitProcess(0) }
        }
    }

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

    private fun getConfig() = streamConfigAiven(
        appId = SERVICE_APP_ID,
        bootStapServerUrl = config.kafka.aivenBrokers,
        aivenCredentials = KafkaAivenCredentials()
    )
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
        sikkerlogg.info { "Mottok packet: ${packet.toJson()}" }
        packetStrategies.forEach { it.run(packet) }
    }
}
