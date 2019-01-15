package no.nav.dagpenger.regel.api

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

data class Environment(val map: Map<String, String> = System.getenv()) {
    val username: String by lazyEnvVar("SRVDP_REGEL_API_USERNAME")
    val password: String by lazyEnvVar("SRVDP_REGEL_API_PASSWORD")
    val bootstrapServersUrl: String by lazyEnvVar("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
    val schemaRegistryUrl: String by lazyEnvVar("KAFKA_SCHEMA_REGISTRY_URL", "http://localhost:8081")
    val apiHttpPort: Int = 8092
    val kafkaApiPort: Int = 8080

    private fun lazyEnvVar(key: String, defaultValue: String? = null): ReadOnlyProperty<Environment, String> {
        return lazyEnvVar(key, null) { value -> value }
    }

    private fun <R> lazyEnvVar(
        key: String,
        defaultValue: String? = null,
        mapper: ((String) -> R)
    ): ReadOnlyProperty<Environment, R> {
        return object : ReadOnlyProperty<Environment, R> {
            override operator fun getValue(thisRef: Environment, property: KProperty<*>) =
                mapper(envVar(key, defaultValue))
        }
    }

    private fun envVar(key: String, defaultValue: String? = null): String {
        return map[key] ?: defaultValue ?: throw RuntimeException("Missing required variable \"$key\"")
    }
}
