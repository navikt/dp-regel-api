package no.nav.dagpenger.regel.api.streams

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.Vaktmester
import no.nav.dagpenger.regel.api.db.BruktSubsumsjonStore
import no.nav.dagpenger.regel.api.db.EksternSubsumsjonBrukt
import no.nav.dagpenger.regel.api.db.InternSubsumsjonBrukt
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Test
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

val LOGGER = KotlinLogging.logger { }

class KafkaEksternSubsumsjonBruktConsumerTest {
    private object Kafka {
        val instance by lazy {
            KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.3.1")).apply { this.start() }
        }
    }

    @Test
    fun `should insert brukt subsumsjon`() {
        val now = ZonedDateTime.now()
        runBlocking {
            val lagretTilDb = slot<InternSubsumsjonBrukt>()
            val markertSomBrukt = slot<InternSubsumsjonBrukt>()
            val storeMock = mockk<BruktSubsumsjonStore>(relaxed = false).apply {
                every { this@apply.eksternTilInternSubsumsjon(any()) } returns InternSubsumsjonBrukt(
                    id = "test",
                    behandlingsId = "b",
                    arenaTs = now.minusMinutes(5)
                )
                every { this@apply.insertSubsumsjonBrukt(capture(lagretTilDb)) } returns 1
            }
            val vaktmester = mockk<Vaktmester>(relaxed = true).apply {
                every { this@apply.markerSomBrukt(capture(markertSomBrukt)) }
            }
            val config = Configuration().run {
                copy(kafka = kafka.copy(brokers = Kafka.instance.bootstrapServers))
            }
            KafkaSubsumsjonBruktConsumer.apply {
                create(config, storeMock, vaktmester)
                listen()
            }

            val producer = KafkaProducer<String, String>(
                producerConfig(
                    appId = "test",
                    bootStapServerUrl = Kafka.instance.bootstrapServers
                ).also {
                    it[ProducerConfig.ACKS_CONFIG] = "all"
                }
            )
            val bruktSubsumsjon =
                EksternSubsumsjonBrukt(
                    id = "test",
                    eksternId = 1234678L,
                    arenaTs = now,
                    ts = now.toInstant().toEpochMilli()
                )
            val metaData = producer.send(ProducerRecord(config.subsumsjonBruktTopic, "test", bruktSubsumsjon.toJson()))
                .get(5, TimeUnit.SECONDS)
            LOGGER.info("Producer produced $bruktSubsumsjon with meta $metaData")
            metaData.topic() shouldBe config.subsumsjonBruktTopic
            Thread.sleep(500)

            lagretTilDb.isCaptured shouldBe true
            markertSomBrukt.isCaptured shouldBe true
            lagretTilDb.captured.arenaTs shouldBe now.minusMinutes(5L)
            lagretTilDb.captured shouldBe markertSomBrukt.captured
        }
    }

    @Test
    fun `Should be able to convert scientific notation back to long`() {
        val science = "1.2345678E7"
        12345678L shouldBe science.toDouble().toLong()
    }
}
