package no.nav.dagpenger.regel.api.routing

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.MissingRequestParameterException
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.BehovId
import no.nav.dagpenger.regel.api.models.SubsumsjonId

internal fun Route.subsumsjon(store: SubsumsjonStore) {
    route("subsumsjon/") {
        get("/{behovid}") {
            withContext(Dispatchers.IO) {
                val behovid = BehovId(call.parameters["behovid"] ?: throw MissingRequestParameterException("behovid"))
                store.getSubsumsjon(behovid).toJson().let {
                    call.respond(HttpStatusCode.OK, it)
                }
            }
        }
        get("/result/{subsumsjonsid}") {
            withContext(Dispatchers.IO) {
                val subsumsjonsId = SubsumsjonId(call.parameters["subsumsjonsid"] ?: throw MissingRequestParameterException("behovid"))
                store.getSubsumsjonByResult(subsumsjonsId).toJson().let {
                    call.respond(HttpStatusCode.OK, it)
                }
            }
        }
    }
}
