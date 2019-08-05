package no.nav.dagpenger.regel.api

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.listType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.dagpenger.regel.api.auth.AuthApiKeyVerifier
import no.nav.dagpenger.streams.KafkaCredential

private val localProperties = ConfigurationMap(
    mapOf(
        "database.host" to "localhost",
        "database.port" to "5432",
        "database.name" to "dp-regel-api",
        "database.user" to "postgres",
        "database.password" to "postgres",
        "vault.mountpath" to "postgresql/dev/",
        "kafka.bootstrap.servers" to "localhost:9092",
        "application.profile" to "LOCAL",
        "application.httpPort" to "8092",
        "auth.secret" to "secret",
        "auth.allowedKeys" to "secret1, secret2",
        "kafka.subsumsjon.topic" to "privat-dagpenger-subsumsjon-brukt"
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
        "kafka.subsumsjon.topic" to "privat-dagpenger-subsumsjon-brukt"

    )
)
private val prodProperties = ConfigurationMap(
    mapOf(
        "database.host" to "fsspgdb.adeo.no",
        "database.port" to "5432",
        "database.name" to "dp-regel-api",
        "vault.mountpath" to "postgresql/prod-fss/",
        "kafka.bootstrap.servers" to "a01apvl00145.adeo.no:8443,a01apvl00146.adeo.no:8443,a01apvl00147.adeo.no:8443,a01apvl00148.adeo.no:8443,a01apvl00149.adeo.no:8443,a01apvl150.adeo.no:8443",
        "application.profile" to "PROD",
        "application.httpPort" to "8092",
        "kafka.subsumsjon.topic" to "privat-dagpenger-subsumsjon-brukt"
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
    val subsumsjonBruktTopic: String = config()[Key("kafka.subsumsjon.topic", stringType)]
) {

    data class Auth(
        val secret: String = config()[Key("auth.secret", stringType)],
        val allowedKeys: List<String> = config()[Key("auth.allowedKeys", listType(stringType))],
        val authApiKeyVerifier: AuthApiKeyVerifier = AuthApiKeyVerifier(secret, allowedKeys)
    )

    data class Database(
        val host: String = config()[Key("database.host", stringType)],
        val port: String = config()[Key("database.port", stringType)],
        val name: String = config()[Key("database.name", stringType)],
        val user: String? = config().getOrNull(Key("database.user", stringType)),
        val password: String? = config().getOrNull(Key("database.password", stringType))

    )

    data class Vault(
        val mountPath: String = config()[Key("vault.mountpath", stringType)]
    )

    data class Kafka(
        val brokers: String = config()[Key("kafka.bootstrap.servers", stringType)],
        val user: String? = config().getOrNull(Key("srvdp.regel.api.username", stringType)),
        val password: String? = config().getOrNull(Key("srvdp.regel.api.password", stringType))
    ) {
        fun credential(): KafkaCredential? {
            return if (user != null && password != null) {
                KafkaCredential(user, password)
            } else null
        }
    }

    data class Application(
        val profile: Profile = config()[Key("application.profile", stringType)].let { Profile.valueOf(it) },
        val httpPort: Int = config()[Key("application.httpPort", intType)]
    )
}

enum class Profile {
    LOCAL, DEV, PROD
}
