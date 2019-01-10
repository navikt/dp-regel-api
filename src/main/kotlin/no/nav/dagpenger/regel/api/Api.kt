package no.nav.dagpenger.regel.api

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.http.HttpHeaders
import io.ktor.request.receive
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class Beregning(
    val aktorId: String,
    val verneplikt: Boolean,
    val fangstOgFisk: Boolean
)

data class RegelBeregning(
    val regel: String,
    val status: String, // todo: enum?
    val expires: String // todo: real date
)

data class MinsteinntektBeregningResultat(
    val oppfyllerMinsteinntekt: Boolean,
    val periode: Int,
    val status: String // todo: enum?
) {
    companion object {
        val exampleInntektBeregning = mapOf(
                "oppfyllerMinsteinntekt" to true,
                "status" to 1,
                "status" to "string"
        )
    }
}

fun Application.main() {
    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }

    routing {

        route("minsteinntekt") {
            post {
                val beregning = call.receive<Beregning>()
                val taskId = UUID.randomUUID().toString()
                call.response.header(HttpHeaders.Location, "/task/$taskId")
                call.respond(
                        RegelBeregning(
                                regel = "minsteinntekt",
                                status = "pending",
                                expires = ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(2).format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
                        )
                )
            }
            get("/{id}") {
                val id = call.parameters["id"]

                call.respond(
                        MinsteinntektBeregningResultat(
                                oppfyllerMinsteinntekt = true,
                                periode = 52,
                                status = "done"
                        )
                )
            }
        }
        route("task") {
            get("/{id}") {
                val id = call.parameters["id"]
                call.respond(
                        RegelBeregning(
                                regel = "minsteinntekt",
                                status = "pending",
                                expires = ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(2).format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
                        )
                )
            }
        }
    }
}