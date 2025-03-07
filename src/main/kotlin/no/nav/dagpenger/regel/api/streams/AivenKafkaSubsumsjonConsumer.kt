package no.nav.dagpenger.regel.api.streams

import io.prometheus.metrics.core.metrics.Summary
import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.KafkaAivenCredentials
import no.nav.dagpenger.regel.api.PacketDeserializer
import no.nav.dagpenger.regel.api.PacketSerializer
import no.nav.dagpenger.regel.api.models.PacketKeys
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import no.nav.dagpenger.regel.api.streamConfigAiven
import no.nav.dagpenger.regel.api.streams.SubsumsjonPond.CorrelationId.X_CORRELATION_ID
import org.apache.kafka.common.errors.TopicAuthorizationException
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serdes.StringSerde
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Predicate
import org.apache.logging.log4j.ThreadContext
import java.time.Duration

private val LOGGER = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")
private val processTimeLatency: Summary =
    Summary.builder()
        .name("process_time_seconds")
        .quantile(0.5, 0.05) // Add 50th percentile (= median) with 5% tolerated error
        .quantile(0.9, 0.01) // Add 90th percentile with 1% tolerated error
        .quantile(0.99, 0.001) // Add 99th percentile with 0.1% tolerated error
        .help("Process time for a single packet")
        .register()

internal class AivenKafkaSubsumsjonConsumer(
    private val config: Configuration,
    private val subsumsjonPond: SubsumsjonPond,
) : HealthCheck {
    private val serviceAppId = "dp-regel-api-sub-brukt"

    private val streams: KafkaStreams by lazy {
        KafkaStreams(subsumsjonPond.buildTopology(), this.getConfig()).apply {
            setUncaughtExceptionHandler { exc ->
                logUnexpectedError(exc)
                stop()
                StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_CLIENT
            }
        }
    }

    fun start() = streams.start().also { LOGGER.info { "Starting up $serviceAppId kafka consumer" } }

    fun stop() =
        with(streams) {
            close(Duration.ofSeconds(3))
            cleanUp()
        }.also {
            LOGGER.info { "Shutting down  $serviceAppId kafka consumer" }
        }

    override fun status(): HealthStatus =
        when (streams.state()) {
            KafkaStreams.State.ERROR -> HealthStatus.DOWN
            KafkaStreams.State.PENDING_SHUTDOWN -> HealthStatus.DOWN
            else -> HealthStatus.UP
        }

    private fun getConfig() =
        streamConfigAiven(
            appId = serviceAppId,
            bootStapServerUrl = config.aivenBrokers,
            aivenCredentials = KafkaAivenCredentials(),
        )

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

internal class SubsumsjonPond(
    private val packetStrategies: List<SubsumsjonPacketStrategy>,
    private val topic: String,
) {
    object CorrelationId {
        const val X_CORRELATION_ID = "x_correlation_id"
    }

    fun buildTopology(): Topology {
        val builder = StreamsBuilder()
        val stream =
            builder.stream(
                topic,
                Consumed.with(
                    StringSerde(),
                    Serdes.serdeFrom(PacketSerializer(), PacketDeserializer()),
                ),
            )
        stream
            .peek { _, packet -> ThreadContext.put(X_CORRELATION_ID, packet.getCorrelationId()) }
            .peek { key, _ -> LOGGER.debug { "Pond recieved packet with key $key and will test it against filters." } }
            .filter { key, packet -> filterPredicates().all { it.test(key, packet) } }
            .foreach { key, packet ->
                LOGGER.debug { "Packet with key $key passed filters and now calling onPacket() for: $packet" }

                val timer = processTimeLatency.startTimer()
                onPacket(packet)
                timer.observeDuration()
                ThreadContext.remove(X_CORRELATION_ID)
            }
        return builder.build()
    }

    fun filterPredicates(): List<Predicate<String, Packet>> =
        listOf(
            Predicate { _, packet -> packet.hasField(PacketKeys.BEHOV_ID) },
            Predicate { _, packet -> !packet.hasProblem() },
        )

    fun onPacket(packet: Packet) {
        sikkerlogg.info { "Mottok packet: ${packet.toJson()}" }
        packetStrategies.forEach { it.run(packet) }
    }
}
