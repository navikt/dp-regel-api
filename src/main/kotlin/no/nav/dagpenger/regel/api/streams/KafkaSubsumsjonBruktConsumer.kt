package no.nav.dagpenger.regel.api.streams

import io.prometheus.client.Summary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.nav.dagpenger.plain.consumerConfig
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.Vaktmester
import no.nav.dagpenger.regel.api.db.BruktSubsumsjonStore
import no.nav.dagpenger.regel.api.db.EksternSubsumsjonBrukt
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import no.nav.dagpenger.streams.KafkaCredential
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.RetriableException
import org.apache.kafka.common.serialization.StringDeserializer
import java.sql.SQLTransientConnectionException
import java.time.Duration
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger { }

private val summary: Summary = Summary.Builder()
    .name("subsumsjon_brukt_consumer_timer")
    .help("Tid brukt til å consumere  privat-dagpenger-subsumsjon-brukt topic")
    .register()

internal object KafkaSubsumsjonBruktConsumer : HealthCheck,
    CoroutineScope {

    const val SERVICE_APP_ID = "dp-regel-api-sub-brukt"

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    lateinit var config: Configuration
    lateinit var bruktSubsumsjonStrategy: BruktSubsumsjonStrategy
    lateinit var job: Job

    fun create(config: Configuration, bruktSubsumsjonStore: BruktSubsumsjonStore, vaktmester: Vaktmester) {
        this.config = config
        this.bruktSubsumsjonStrategy =
            BruktSubsumsjonStrategy(vaktmester = vaktmester, bruktSubsumsjonStore = bruktSubsumsjonStore)
        this.job = Job()
    }

    override fun status(): HealthStatus {
        return when (job.isActive) {
            true -> HealthStatus.UP
            false -> HealthStatus.DOWN
        }
    }

    fun stop() {
        logger.info { "Stopping KafkaSubsumsjonBrukt consumer" }
        job.cancel()
    }

    fun listen() {
        launch {
            val creds = config.kafka.user?.let { u ->
                config.kafka.password?.let { p ->
                    KafkaCredential(username = u, password = p)
                }
            }
            logger.info { "Starting KafkaSubsumsjonBruktConsumer" }
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
                        if (!records.isEmpty) {
                            val timer = summary.startTimer()
                            bruktSubsumsjonStrategy.handle(
                                records.asSequence()
                                    .map { r -> EksternSubsumsjonBrukt.fromJson(r.value()) }
                                    .filterNotNull())
                            logger.info { " Brukte  ${timer.observeDuration()} sekunder på ${records.count()} events" }
                        }
                    }
                } catch (e: Exception) {
                    when (e) {
                        is RetriableException,
                        is SQLTransientConnectionException -> {
                            logger.warn("Retriable exception, looping back", e)
                        }
                        else -> {
                            logger.error("Unexpected exception while consuming messages. Stopping", e)
                            stop()
                        }
                    }
                }
            }
        }
    }
}