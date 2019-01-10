package no.nav.dagpenger.regel.api

import de.nielsfalk.ktor.swagger.SwaggerSupport
import de.nielsfalk.ktor.swagger.example
import de.nielsfalk.ktor.swagger.get
import de.nielsfalk.ktor.swagger.ok
import de.nielsfalk.ktor.swagger.responds
import de.nielsfalk.ktor.swagger.version.shared.Group
import de.nielsfalk.ktor.swagger.version.shared.Information
import de.nielsfalk.ktor.swagger.version.v2.Swagger
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.http.HttpHeaders
import io.ktor.locations.Location
import io.ktor.locations.Locations
import io.ktor.request.receive
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
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

@Group("API")
@Location("/{id}")
data class GetMinsteinntekt(val id: String)

class Oppslag {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            embeddedServer(Netty, port = 8092, module = Application::main).start(wait = true)
        }
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
    install(Locations)

    install(SwaggerSupport) {
        forwardRoot = true
        val information = Information(
                version = "0.1",
                title = "sample api implemented in ktor",
                description = "This is a sample which combines [ktor](https://github.com/Kotlin/ktor) with [swaggerUi](https://swagger.io/). You find the sources on [github](https://github.com/nielsfalk/ktor-swagger)"
        )
        swagger = Swagger().apply {
            info = information
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

            get<GetMinsteinntekt>("/{id}".responds(
                    ok<MinsteinntektBeregningResultat>(
                            example("model", MinsteinntektBeregningResultat.exampleInntektBeregning)
                    ))) { param ->
                val id = param.id

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