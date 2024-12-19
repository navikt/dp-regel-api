package no.nav.dagpenger.regel.api.routing

import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.prometheus.metrics.model.registry.PrometheusRegistry

fun Routing.metrics(collectorRegistry: PrometheusRegistry = PrometheusRegistry.defaultRegistry) {
    route("/metrics") {
        get {
            call.respondText {
                collectorRegistry.scrape().toString()
            }
        }
    }
}
