package no.nav.dagpenger.regel.api.streams

import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.regel.api.APPLICATION_NAME
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.models.PacketKeys
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import no.nav.dagpenger.streams.Pond
import no.nav.dagpenger.streams.Topics
import no.nav.dagpenger.streams.streamConfig
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.kstream.Predicate
import java.time.Duration

private val LOGGER = KotlinLogging.logger {}

internal class KafkaSubsumsjonConsumer(
    private val config: Configuration,
    private val subsumsjonPond: SubsumsjonPond

) : HealthCheck {

    private val streams: KafkaStreams by lazy {
        KafkaStreams(subsumsjonPond.buildTopology(), this.getConfig()).apply {
            setUncaughtExceptionHandler { _, _ -> System.exit(0) }
        }
    }

    fun start() = streams.start().also { LOGGER.info { "Starting up $APPLICATION_NAME kafca consumer" } }

    fun stop() = with(streams) {
        close(Duration.ofSeconds(3))
        cleanUp()
    }.also {
        LOGGER.info { "Shutting down $APPLICATION_NAME kafka consumer" }
    }

    override fun status(): HealthStatus =
        when (streams.state()) {
            KafkaStreams.State.ERROR -> HealthStatus.DOWN
            KafkaStreams.State.PENDING_SHUTDOWN -> HealthStatus.DOWN
            else -> HealthStatus.UP
        }

    private fun getConfig() = streamConfig(
        appId = APPLICATION_NAME,
        bootStapServerUrl = config.kafka.brokers,
        credential = config.kafka.credential()
    )
}

internal class SubsumsjonPond(private val packetStrategies: List<SubsumsjonPacketStrategy>) : Pond(Topics.DAGPENGER_BEHOV_PACKET_EVENT) {
    override val SERVICE_APP_ID: String = APPLICATION_NAME

    override fun filterPredicates(): List<Predicate<String, Packet>> =
        listOf(Predicate { _, packet -> packet.hasField(PacketKeys.BEHOV_ID) })

    override fun onPacket(packet: Packet) = packetStrategies.forEach { it.run(packet) }
}
