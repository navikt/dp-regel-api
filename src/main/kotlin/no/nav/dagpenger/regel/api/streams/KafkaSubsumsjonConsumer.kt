package no.nav.dagpenger.regel.api.streams

import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.regel.api.APPLICATION_NAME
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.PacketKeys
import no.nav.dagpenger.regel.api.models.Status
import no.nav.dagpenger.regel.api.models.behovId
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import no.nav.dagpenger.streams.Pond
import no.nav.dagpenger.streams.streamConfig
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.kstream.Predicate
import java.util.concurrent.TimeUnit

private val LOGGER = KotlinLogging.logger {}

internal class KafkaSubsumsjonConsumer(
    private val config: Configuration,
    private val subsumsjonStore: SubsumsjonStore
) : HealthCheck {

    private val streams: KafkaStreams by lazy {
        KafkaStreams(SubsumsjonPond(subsumsjonStore, subsumsjonPacketStrategies(subsumsjonStore)).buildTopology(), this.getConfig()).apply {
            setUncaughtExceptionHandler { _, _ -> System.exit(0) }
        }
    }

    fun start() = streams.start().also { LOGGER.info { "Starting up $APPLICATION_NAME kafca consumer" } }

    fun stop() = with(streams) {
        close(3, TimeUnit.SECONDS)
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

internal class SubsumsjonPond(private val subsumsjonStore: SubsumsjonStore, private val packetStrategies: List<SubsumsjonPacketStrategy>) : Pond() {
    override val SERVICE_APP_ID: String = APPLICATION_NAME

    override fun filterPredicates(): List<Predicate<String, Packet>> =
            listOf(Predicate { _, packet -> packet.hasField(PacketKeys.BEHOV_ID) && behovPending(packet.behovId) })

    override fun onPacket(packet: Packet) = packetStrategies.forEach { it.run(packet) }

    private fun behovPending(behovId: String) = runCatching { subsumsjonStore.behovStatus(behovId) }
            .onFailure { LOGGER.error(it) { "Failed to get status of behov: $behovId" } }
            .map { it == Status.Pending }
            .getOrDefault(false)
}
