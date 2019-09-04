package no.nav.dagpenger.regel.api.streams

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
import kotlin.coroutines.CoroutineContext

private val LOGGER = KotlinLogging.logger { }

internal object KafkaSubsumsjonBruktConsumer : HealthCheck,
    CoroutineScope {
    val SERVICE_APP_ID = "dp-regel-api-sub-brukt"

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    lateinit var config: Configuration
    lateinit var bruktSubsumsjonStore: BruktSubsumsjonStore
    lateinit var job: Job

    fun create(config: Configuration, bruktSubsumsjonStore: BruktSubsumsjonStore) {
        this.config = config
        this.bruktSubsumsjonStore = bruktSubsumsjonStore
        this.job = Job()
    }

    override fun status(): HealthStatus {
        return when (job.isActive) {
            true -> HealthStatus.UP
            false -> HealthStatus.DOWN
        }
    }

    fun stop() {
        LOGGER.info { "Stopping KafkaSubsumsjonBrukt consumer" }
        job.cancel()
    }

    suspend fun listen() {
        launch {
            val creds = config.kafka.user?.let { u ->
                config.kafka.password?.let { p ->
                    KafkaCredential(username = u, password = p)
                }
            }
            LOGGER.info { "Starting KafkaSubsumsjonBruktConsumer" }
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
                try {
                    consumer.subscribe(listOf(config.subsumsjonBruktTopic))
                    while (job.isActive) {
                        val records = consumer.poll(Duration.ofMillis(100))
                        records.asSequence()
                            .map { r -> SubsumsjonBrukt.fromJson(r.value()) }
                            .filterNotNull()
                            .onEach { b -> LOGGER.info("Saving $b to database") }
                            .forEach { bruktSubsumsjonStore.insertSubsumsjonBruktV2(bruktSubsumsjonStore.v1TilV2(it)) }
                    }
                } catch (e: RetriableException) {
                    LOGGER.warn("Kafka threw a retriable exception, looping back", e)
                } catch (e: Exception) {
                    LOGGER.error("Unexpected exception while consuming messages. Stopping", e)
                }
            }
        }
    }
}