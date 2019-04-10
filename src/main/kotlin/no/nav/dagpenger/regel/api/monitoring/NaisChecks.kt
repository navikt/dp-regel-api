package no.nav.dagpenger.regel.api.monitoring

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respondText
import io.ktor.routing.Routing

@Location("/isAlive")
class IsAlive

@Location("/isReady")
class IsReady

fun Routing.naischecks(healthChecks: List<HealthCheck>) {
    get<IsAlive> {
        if (healthChecks.all { it.status() == HealthStatus.UP }) call.respondText("ALIVE", ContentType.Text.Plain) else
            call.response.status(HttpStatusCode.ServiceUnavailable)
    }

    get<IsReady> {
        call.respondText("READY", ContentType.Text.Plain)
    }
}
