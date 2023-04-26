package no.nav.dagpenger.regel.api.routing

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.monitoring.HealthStatus

private val LOGGER = KotlinLogging.logger {}

fun Routing.naischecks(healthChecks: List<HealthCheck>) {
    get("isAlive") {
        val failingHealthChecks = healthChecks.filter { it.status() == HealthStatus.DOWN }

        when {
            failingHealthChecks.isEmpty() -> call.respondText("ALIVE", ContentType.Text.Plain)
            else -> {
                LOGGER.error("The health check(s) is failing ${failingHealthChecks.joinToString(", ")}")
                call.response.status(HttpStatusCode.ServiceUnavailable)
            }
        }
    }

    get("isReady") {
        call.respondText("READY", ContentType.Text.Plain)
    }
}
