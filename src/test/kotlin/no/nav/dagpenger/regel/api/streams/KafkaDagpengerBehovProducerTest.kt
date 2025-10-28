package no.nav.dagpenger.regel.api.streams

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.models.BehandlingsId
import no.nav.dagpenger.regel.api.models.Behov
import no.nav.dagpenger.regel.api.models.InternBehov
import no.nav.dagpenger.regel.api.models.Kontekst
import no.nav.dagpenger.regel.api.models.RegelKontekst
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import no.nav.dagpenger.regel.api.producerConfig
import org.junit.jupiter.api.Test
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.LocalDate
import java.util.concurrent.TimeUnit

private object Kafka {
    val instance by lazy {
        // See https://docs.confluent.io/current/installation/versions-interoperability.html#cp-and-apache-kafka-compatibility
        KafkaContainer(DockerImageName.parse("apache/kafka-native:4.0.1")).apply { this.start() }
    }
}

internal class KafkaDagpengerBehovProducerTest {
    @Test
    fun `Produce packet should success`() {
        KafkaDagpengerBehovProducer(
            producerConfig("APP", Kafka.instance.bootstrapServers, null),
            Configuration.regelTopicName,
        ).apply {
            val metadata =
                produceEvent(
                    InternBehov.fromBehov(
                        behov =
                            Behov(
                                aktørId = "aktorId",
                                regelkontekst = RegelKontekst("1", Kontekst.vedtak),
                                beregningsDato = LocalDate.now(),
                            ),
                        behandlingsId = BehandlingsId.nyBehandlingsIdFraEksternId(RegelKontekst("123", Kontekst.vedtak)),
                    ),
                ).get(5, TimeUnit.SECONDS)

            metadata shouldNotBe null
            metadata.hasOffset() shouldBe true
            metadata.serializedKeySize() shouldBeGreaterThan -1
            metadata.serializedValueSize() shouldBeGreaterThan -1
        }
    }

    @Test
    fun `Test kafka health`() {
        KafkaDagpengerBehovProducer(
            producerConfig("APP", Kafka.instance.bootstrapServers, null),
            Configuration.regelTopicName,
        ).apply {
            this.status() shouldBe HealthStatus.UP
        }
    }
}
