package no.nav.dagpenger.regel.api.monitoring

interface HealthCheck {
    fun status(): HealthStatus
}

enum class HealthStatus {
    UP,
    DOWN,
}
