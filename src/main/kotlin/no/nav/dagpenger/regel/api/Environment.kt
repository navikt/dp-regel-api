package no.nav.dagpenger.regel.api

data class Environment(
    val username: String = getEnvVar("SRVDP_REGEL_API_USERNAME"),
    val password: String = getEnvVar("SRVDP_REGEL_API_PASSWORD"),
    val bootstrapServersUrl: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
    val fasitEnvironmentName: String = getEnvVar(
        "FASIT_ENVIRONMENT_NAME",
        ""
    ),
    val redisHost: String = getEnvVar("REDIS_HOST", "localhost"),
    val httpPort: Int = 8092
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
