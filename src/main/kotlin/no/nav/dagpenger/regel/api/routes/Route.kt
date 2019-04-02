package no.nav.dagpenger.regel.api.routes

import io.ktor.application.call
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import no.nav.dagpenger.regel.api.BadRequestException
import no.nav.dagpenger.regel.api.Regel
import no.nav.dagpenger.regel.api.Status
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.tasks.taskPending

fun Route.getStatus(regel: Regel, store: SubsumsjonStore) {
    route("/status") {
        get("/{behovid}") {
            val behovId = call.parameters["behovid"] ?: throw BadRequestException()

            val status = store.behovStatus(behovId)
            when (status) {
                is Status.Done -> {
                    call.response.header(HttpHeaders.Location, "/${regel.name.toLowerCase()}/${status.subsumsjonsId}")
                    call.respond(HttpStatusCode.SeeOther)
                }
                is Status.Pending -> {
                    call.respond(taskPending(regel))
                }
            }
        }
    }
}

fun Route.getSubsumsjon(store: SubsumsjonStore) {
    get("/{subsumsjonsid}") {
        val subsumsjonsId = call.parameters["subsumsjonsid"] ?: throw BadRequestException()

        store.getSubsumsjon(subsumsjonsId).also {
            call.respond(HttpStatusCode.OK, it)
        }
    }
}
