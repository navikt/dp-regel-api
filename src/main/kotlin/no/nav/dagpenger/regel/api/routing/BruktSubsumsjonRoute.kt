package no.nav.dagpenger.regel.api.routing

import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.dagpenger.regel.api.db.BruktSubsumsjonStore
import no.nav.dagpenger.regel.api.db.SubsumsjonBrukt

internal fun Route.bruktSubsumsjonRoute(store: BruktSubsumsjonStore) {

    authenticate {
        route("subsumsjonbrukt/") {
            post("/") {
                val subsumsjonBrukt = call.receive<SubsumsjonBrukt>()
                runCatching {
                    store.insertSubsumsjonBrukt(subsumsjonBrukt)
                    call.respond(HttpStatusCode.Accepted)
                }.recover { call.respond(HttpStatusCode.InternalServerError) }
            }
        }
    }
}