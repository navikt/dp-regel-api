package no.nav.dagpenger.regel.api.streams

import io.kotlintest.matchers.numerics.shouldBeGreaterThan
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import no.nav.dagpenger.plain.producerConfig
import no.nav.dagpenger.regel.api.models.Behov
import org.junit.jupiter.api.Test
import org.testcontainers.containers.KafkaContainer
import java.time.LocalDate
import java.util.concurrent.TimeUnit

private object Kafka {
    val instance by lazy {
        // See https://docs.confluent.io/current/installation/versions-interoperability.html#cp-and-apache-kafka-compatibility
        KafkaContainer("5.0.1").apply { this.start() }
    }
}

internal class KafkaDagpengerBehovProducerTest {

    @Test
    fun `Produce packet should success`() {
        KafkaDagpengerBehovProducer(producerConfig(
            clientId = "APP",
            bootstrapServers = Kafka.instance.bootstrapServers
        ))
            .apply {
                val metadata = produceEvent(Behov("behovId", "aktorId", 1, LocalDate.now())).get(5, TimeUnit.SECONDS)

                metadata shouldNotBe null
                metadata.hasOffset() shouldBe true
                metadata.serializedKeySize() shouldBeGreaterThan -1
                metadata.serializedValueSize() shouldBeGreaterThan -1
            }
    }
}