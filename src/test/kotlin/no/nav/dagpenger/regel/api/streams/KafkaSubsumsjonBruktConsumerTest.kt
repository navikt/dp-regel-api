package no.nav.dagpenger.regel.api.streams

import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.db.BruktSubsumsjonStore
import no.nav.dagpenger.regel.api.db.SubsumsjonBrukt
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.testcontainers.containers.KafkaContainer
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

val LOGGER = KotlinLogging.logger { }
class KafkaSubsumsjonBruktConsumerTest {
    private object Kafka {
        val instance by lazy {
            KafkaContainer("5.3.0").apply { this.start() }
        }
    }

    @Test
    fun `should insert brukt subsumsjon`() {
        runBlocking {
            val savedToStore = slot<SubsumsjonBrukt>()
            val storeMock = mockk<BruktSubsumsjonStore>(relaxed = false).apply {
                every { this@apply.insertSubsumsjonBrukt(capture(savedToStore)) } returns 1
            }
            val config = Configuration().run {
                copy(kafka = kafka.copy(brokers = Kafka.instance.bootstrapServers))
            }
            KafkaSubsumsjonBruktConsumer.apply {
                create(config, storeMock)
                listen()
            }

            val producer = KafkaProducer<String, String>(
                producerConfig(
                    appId = "test",
                    bootStapServerUrl = Kafka.instance.bootstrapServers
                ).also {
                    it[ProducerConfig.ACKS_CONFIG] = "all"
                })
            val now = ZonedDateTime.now()
            val bruktSubsumsjon =
                SubsumsjonBrukt(id = "test", eksternId = 1234678L, arenaTs = now, ts = now.toInstant().toEpochMilli())
            val metaData = producer.send(ProducerRecord(config.subsumsjonBruktTopic, "test", bruktSubsumsjon.toJson()))
                .get(5, TimeUnit.SECONDS)
            LOGGER.info("Producer produced $bruktSubsumsjon with meta $metaData")
            assertThat(metaData.topic()).isEqualTo(config.subsumsjonBruktTopic)
            Thread.sleep(200)
            assertThat(savedToStore.isCaptured).isTrue()
            assertThat(savedToStore.captured.eksternId).isEqualTo(1234678L)
            assertThat(savedToStore.captured.arenaTs).isEqualTo(now)
        }
    }

    @Test
    fun `Should be able to convert scientific notation back to long`() {
        val science = "1.2345678E7"
        12345678L shouldBe science.toDouble().toLong()
    }
}