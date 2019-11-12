package no.nav.dagpenger.regel.api.routing

import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import no.nav.dagpenger.regel.api.BadRequestException
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.UlidId

internal fun Route.subsumsjon(store: SubsumsjonStore) {
    authenticate {
        route("subsumsjon/") {
            get("/{behovid}") {
                val behovid = UlidId(call.parameters["behovid"] ?: throw BadRequestException())
                store.getSubsumsjon(behovid).toJson().let {
                    call.respond(HttpStatusCode.OK, it)
                }
            }
            get("/result/{subsumsjonsid}") {
                val subsumsjonsId = UlidId(call.parameters["subsumsjonsid"] ?: throw BadRequestException())
                store.getSubsumsjonByResult(subsumsjonsId).toJson().let {
                    call.respond(HttpStatusCode.OK, it)
                }
            }
        }
    }
}
