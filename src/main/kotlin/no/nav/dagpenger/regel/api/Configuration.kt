package no.nav.dagpenger.regel.api

import com.natpryce.konfig.Configuration
import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.streams.PacketDeserializer
import no.nav.dagpenger.streams.PacketSerializer
import no.nav.dagpenger.streams.Topic
import org.apache.kafka.common.serialization.Serdes

private val defaultProperties = ConfigurationMap(
    mapOf(
        "KAFKA_BROKERS" to "localhost:9092",
        "kafka.subsumsjon.topic" to "teamdagpenger.subsumsjonbrukt.v1",
        "azure.app.well.known.url" to "http://localhost/",
        "azure.app.client.id" to "default"
    )
)

internal object Configuration {
    val config: Configuration = systemProperties() overriding EnvironmentVariables overriding defaultProperties

    val azureAppClientId: String = config[Key("azure.app.client.id", stringType)]
    val azureAppWellKnownUrl: String = config[Key("azure.app.well.known.url", stringType)]

    val aivenBrokers: String = config[Key("KAFKA_BROKERS", stringType)]
    val subsumsjonBruktTopic: String = "teamdagpenger.subsumsjonbrukt.v1"
    val inntektBruktTopic: String = "teamdagpenger.inntektbrukt.v1"

    val regelTopic: Topic<String, Packet> = Topic(
        name = "teamdagpenger.regel.v1",
        keySerde = Serdes.String(),
        valueSerde = Serdes.serdeFrom(PacketSerializer(), PacketDeserializer())
    )

    val id: String = "dp-regel-api"
    val httpPort: Int = 8092
}
