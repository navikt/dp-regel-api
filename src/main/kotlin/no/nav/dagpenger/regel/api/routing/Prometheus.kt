package no.nav.dagpenger.regel.api.routing

import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun Routing.metrics(collectorRegistry: PrometheusMeterRegistry) {
    route("/metrics") {
        get {
            call.respondText(
                collectorRegistry.scrape(),
                ContentType.parse("text/plain; version=0.0.4; charset=utf-8"),
            )
        }
    }
}
