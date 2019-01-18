package no.nav.dagpenger.regel.api

data class Environment(
    val username: String = getEnvVar("SRVDP_REGEL_API_USERNAME", "itest"),
    val password: String = getEnvVar("SRVDP_REGEL_API_PASSWORD", "igroup"),
    val bootstrapServersUrl: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
    val schemaRegistryUrl: String = getEnvVar("KAFKA_SCHEMA_REGISTRY_URL", "http://localhost:8081"),
    val trustStorePath: String = getEnvVar("NAV_TRUSTSTORE_PATH"),
    val trustStorePassword: String = getEnvVar("NAV_TRUSTSTORE_PASSWORD"),
    val fasitEnvironmentName: String = getEnvVar(
        "FASIT_ENVIRONMENT_NAME",
        ""
    ),
    val apiHttpPort: Int = 8092,
    val kafkaApiPort: Int = 8080
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
