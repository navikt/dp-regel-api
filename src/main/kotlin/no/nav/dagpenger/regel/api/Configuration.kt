package no.nav.dagpenger.regel.api

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.listType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.inntekt.ApiKeyVerifier
import no.nav.dagpenger.streams.PacketDeserializer
import no.nav.dagpenger.streams.PacketSerializer
import no.nav.dagpenger.streams.Topic
import org.apache.kafka.common.serialization.Serdes

private val localProperties = ConfigurationMap(
    mapOf(
        "database.host" to "localhost",
        "database.port" to "5432",
        "database.name" to "dp-regel-api",
        "database.user" to "postgres",
        "database.password" to "postgres",
        "vault.mountpath" to "postgresql/dev/",
        "kafka.bootstrap.servers" to "localhost:9092",
        "KAFKA_BROKERS" to "localhost:9092",
        "application.profile" to "LOCAL",
        "application.httpPort" to "8092",
        "auth.secret" to "secret",
        "auth.allowedKeys" to "secret1, secret2",
        "kafka.subsumsjon.topic" to "teamdagpenger.subsumsjonbrukt.v1",
        "unleash.url" to "https://localhost",
        "azure.app.well.known.url" to "http://localhost/",
        "azure.app.client.id" to "default"
    )
)
private val devProperties = ConfigurationMap(
    mapOf(
        "database.host" to "b27dbvl007.preprod.local",
        "database.port" to "5432",
        "database.name" to "dp-regel-api-preprod",
        "vault.mountpath" to "postgresql/preprod-fss/",
        "kafka.bootstrap.servers" to "b27apvl00045.preprod.local:8443,b27apvl00046.preprod.local:8443,b27apvl00047.preprod.local:8443",
        "application.profile" to "DEV",
        "application.httpPort" to "8092",
        "kafka.subsumsjon.topic" to "teamdagpenger.subsumsjonbrukt.v1",
        "unleash.url" to "https://unleash.nais.io/api/",
    )
)
private val prodProperties = ConfigurationMap(
    mapOf(
        "database.host" to "fsspgdb.adeo.no",
        "database.port" to "5432",
        "database.name" to "dp-regel-api",
        "vault.mountpath" to "postgresql/prod-fss/",
        "kafka.bootstrap.servers" to "a01apvl00145.adeo.no:8443,a01apvl00146.adeo.no:8443,a01apvl00147.adeo.no:8443,a01apvl00148.adeo.no:8443,a01apvl00149.adeo.no:8443,a01apvl00150.adeo.no:8443",
        "application.profile" to "PROD",
        "application.httpPort" to "8092",
        "kafka.subsumsjon.topic" to "teamdagpenger.subsumsjonbrukt.v1",
        "unleash.url" to "https://unleash.nais.io/api/",
    )
)

private fun config() = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
    "dev-fss" -> systemProperties() overriding EnvironmentVariables overriding devProperties
    "prod-fss" -> systemProperties() overriding EnvironmentVariables overriding prodProperties
    else -> {
        systemProperties() overriding EnvironmentVariables overriding localProperties
    }
}

internal data class Configuration(
    val auth: Auth = Auth(),
    val database: Database = Database(),
    val vault: Vault = Vault(),
    val kafka: Kafka = Kafka(),
    val application: Application = Application(),
    val subsumsjonBruktTopic: String = config()[Key("kafka.subsumsjon.topic", stringType)],
    val regelTopic: Topic<String, Packet> = Topic(
        name = "teamdagpenger.regel.v1",
        keySerde = Serdes.String(),
        valueSerde = Serdes.serdeFrom(PacketSerializer(), PacketDeserializer())
    ),
    val inntektBruktTopic: String = "teamdagpenger.inntektbrukt.v1",
) {

    data class Auth(
        val secret: String = config()[Key("auth.secret", stringType)],
        val allowedKeys: List<String> = config()[Key("auth.allowedKeys", listType(stringType))],
        val authApiKeyVerifier: AuthApiKeyVerifier = AuthApiKeyVerifier(
            apiKeyVerifier = ApiKeyVerifier(secret),
            clients = allowedKeys
        ),
        val azureAppClientId: String = config()[Key("azure.app.client.id", stringType)],
        val azureAppWellKnownUrl: String = config()[Key("azure.app.well.known.url", stringType)],
    )

    data class Database(
        val host: String = config()[Key("database.host", stringType)],
        val port: String = config()[Key("database.port", stringType)],
        val name: String = config()[Key("database.name", stringType)],
        val user: String? = config().getOrNull(Key("database.user", stringType)),
        val password: String? = config().getOrNull(Key("database.password", stringType)),

    )

    data class Vault(
        val mountPath: String = config()[Key("vault.mountpath", stringType)],
    )

    data class Kafka(
        val brokers: String = config()[Key("kafka.bootstrap.servers", stringType)],
        val aivenBrokers: String = config()[Key("KAFKA_BROKERS", stringType)],
        val user: String? = config().getOrNull(Key("srvdp.regel.api.username", stringType)),
        val password: String? = config().getOrNull(Key("srvdp.regel.api.password", stringType)),
    )

    data class Application(
        val id: String = config().getOrElse(Key("application.id", stringType), "dp-regel-api"),
        val profile: Profile = config()[Key("application.profile", stringType)].let { Profile.valueOf(it) },
        val httpPort: Int = config()[Key("application.httpPort", intType)],
        val unleashUrl: String = config()[Key("unleash.url", stringType)],
    )
}

enum class Profile {
    LOCAL, DEV, PROD
}
