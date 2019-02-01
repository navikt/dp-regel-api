package no.nav.dagpenger.regel.api

data class Environment(
    val username: String = getEnvVar("SRVDP_REGEL_API_USERNAME", "itest"),
    val password: String = getEnvVar("SRVDP_REGEL_API_PASSWORD", "igroup"),
    val bootstrapServersUrl: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
    val fasitEnvironmentName: String = getEnvVar(
        "FASIT_ENVIRONMENT_NAME",
        ""
    ),
    val kafkaApiPort: Int = 8080,
    val redisHost: String = getEnvVar("REDIS_HOST", "localhost"),
    val httpPort: Int? = null
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
