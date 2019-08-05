package no.nav.dagpenger.regel.api.streams

import mu.KotlinLogging
import no.nav.dagpenger.plain.consumerConfig
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.db.BruktSubsumsjonStore
import no.nav.dagpenger.regel.api.db.SubsumsjonBrukt
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import no.nav.dagpenger.streams.KafkaCredential
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.RetriableException
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration

private val LOGGER = KotlinLogging.logger { }

internal class KafkaSubsumsjonBruktConsumer(val config: Configuration, val bruktSubsumsjonStore: BruktSubsumsjonStore) :
    HealthCheck {
    val SERVICE_APP_ID = "dp-regel-api-sub-brukt"
    var poll = false

    override fun status(): HealthStatus {
        return if (poll) {
            HealthStatus.UP
        } else {
            HealthStatus.DOWN
        }
    }

    suspend fun start() {
        LOGGER.info { "Starting KafkaSubsumsjonBrukt consumer" }
        listen()
    }

    fun stop() {
        LOGGER.info { "Stopping KafkaSubsumsjonBrukt consumer" }
        poll = false
    }

    private fun listen() {
        val creds = config.kafka.user?.let { u ->
            config.kafka.password?.let { p ->
                KafkaCredential(username = u, password = p)
            }
        }
        KafkaConsumer<String, String>(
            consumerConfig(
                groupId = SERVICE_APP_ID,
                bootstrapServerUrl = config.kafka.brokers,
                credential = creds
            ).also {
                it[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
                it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            }
        ).use { consumer ->
            poll = true
            try {
                consumer.subscribe(listOf(config.subsumsjonBruktTopic))
                while (poll) {
                    val records = consumer.poll(Duration.ofMillis(1000))
                    records.asSequence()
                        .map { r -> SubsumsjonBrukt.fromJson(r.value()) }
                        .filterNotNull()
                        .onEach { b -> LOGGER.info("Saving $b to database") }
                        .forEach { bruktSubsumsjonStore.insertSubsumsjonBrukt(it) }
                }
            } catch (e: RetriableException) {
                LOGGER.warn("Kafka threw a retriable exception, looping back", e)
            } catch (e: Exception) {
                poll = false
                LOGGER.error("Unexpected exception while consuming messages. Stopping", e)
            }
        }
    }
}