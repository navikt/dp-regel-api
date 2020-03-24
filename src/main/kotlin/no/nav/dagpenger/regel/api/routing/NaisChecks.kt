package no.nav.dagpenger.regel.api.routing

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respondText
import io.ktor.routing.Routing
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.monitoring.HealthStatus

private val LOGGER = KotlinLogging.logger {}

@Location("/isAlive")
class IsAlive

@Location("/isReady")
class IsReady

fun Routing.naischecks(healthChecks: List<HealthCheck>) {
    get<IsAlive> {
        val failingHealthChecks = healthChecks.filter { it.status() == HealthStatus.DOWN }

        when {
            failingHealthChecks.isEmpty() -> call.respondText("ALIVE", ContentType.Text.Plain)
            else -> {
                LOGGER.error("The health check(s) is failing ${failingHealthChecks.joinToString(", ")}")
                call.response.status(HttpStatusCode.ServiceUnavailable)
            }
        }
    }

    get<IsReady> {
        call.respondText("READY", ContentType.Text.Plain)
    }
}
