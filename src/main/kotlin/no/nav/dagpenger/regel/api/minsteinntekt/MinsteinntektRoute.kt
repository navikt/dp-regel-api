package no.nav.dagpenger.regel.api.minsteinntekt

import de.nielsfalk.ktor.swagger.description
import de.nielsfalk.ktor.swagger.example
import de.nielsfalk.ktor.swagger.examples
import de.nielsfalk.ktor.swagger.get
import de.nielsfalk.ktor.swagger.ok
import de.nielsfalk.ktor.swagger.post
import de.nielsfalk.ktor.swagger.responds
import de.nielsfalk.ktor.swagger.version.shared.Group
import io.ktor.application.call
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.Routing
import no.nav.dagpenger.regel.api.MinsteInntektBeregningsRequest
import no.nav.dagpenger.regel.api.Regel
import no.nav.dagpenger.regel.api.tasks.Tasks
import no.nav.dagpenger.regel.api.tasks.taskResponseFromTask

@Group("Minsteinntekt")
@Location("/minsteinntekt")
class PostMinsteinntekt

@Group("Minsteinntekt")
@Location("/minsteinntekt/{id}")
data class GetMinsteinntekt(val id: String)

fun Routing.minsteinntekt(minsteinntektBeregninger: MinsteinntektBeregninger, tasks: Tasks) {
    post<PostMinsteinntekt, MinsteInntektBeregningsRequest>(
        "minsteinntektsberegning"
            .description("Start minsteinntektsberegning")
            .examples()
            .responds()
    ) { _, request ->
        val taskId = tasks.createTask(Regel.MINSTEINNTEKT)

        // dette skal egentlig bli gjort av kafka-consumer når regelberegning er ferdig
        tasks.updateTask(taskId, "123")

        call.response.header(HttpHeaders.Location, "/task/$taskId")
        call.respond(HttpStatusCode.Accepted, taskResponseFromTask(tasks.getTask(taskId)))
    }

    get<GetMinsteinntekt>(
        "resultat av minsteinntektsberegning".responds(
            ok<MinsteinntektBeregningResultat>(
                example(
                    "model",
                    MinsteinntektBeregningResultat.exampleInntektBeregning
                )
            )
        )
    ) { param ->
        val id = param.id

        call.respond(minsteinntektBeregninger.getBeregning(id))
    }
}
