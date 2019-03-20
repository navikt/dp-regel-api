package no.nav.dagpenger.regel.api

import com.natpryce.konfig.*
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties

private val localProperties = ConfigurationMap(
        mapOf(
                "database.host" to "localhost",
                "database.port" to "5432",
                "database.name" to "dp-regel-api",
                "vault.mountpath" to "postgresql/dev/",
                "application.profile" to "LOCAL"
        )
)
private val devProperties = ConfigurationMap(
        mapOf(
                "database.host" to "b27dbvl007.preprod.local",
                "database.port" to "5432",
                "database.name" to "dp-regel-api-preprod",
                "vault.mountpath" to "postgresql/preprod-fss/",
                "application.profile" to "DEV"
        )
)
private val prodProperties = ConfigurationMap(
        mapOf(
                "database.host" to "fsspgdb.adeo.no",
                "database.port" to "5432",
                "database.name" to "dp-regel-api",
                "vault.mountpath" to "postgresql/prod-fss/",
                "application.profile" to "PROD"
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
        val database: Database = Database(),
        val vault: Vault = Vault(),
        val application: Application = Application()

) {
    data class Database(
            val host: String = config()[Key("database.host", stringType)],
            val port: String = config()[Key("database.port", stringType)],
            val name: String = config()[Key("database.name", stringType)]
    )

    data class Vault(
            val mountPath: String = config()[Key("vault.mountpath", stringType)]
    )

    data class Application(
            val profile: Profile = config()[Key("application.profile", stringType)].let { Profile.valueOf(it) }
    )
}

internal enum class Profile {
    LOCAL, DEV, PROD
}