package no.nav.dagpenger.regel.api.grunnlag

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
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.Routing
import no.nav.dagpenger.regel.api.GrunnlagBeregningsRequest
import no.nav.dagpenger.regel.api.Regel
import no.nav.dagpenger.regel.api.tasks.Tasks
import no.nav.dagpenger.regel.api.tasks.taskResponseFromTask

@KtorExperimentalLocationsAPI
@Group("Grunnlag")
@Location("/grunnlag")
class PostGrunnlag

@Group("Grunnlag")
@Location("/grunnlag/{id}")
data class GetGrunnlag(val id: String)

fun Routing.grunnlag(grunnlagBeregninger: GrunnlagBeregninger, tasks: Tasks) {
    post<PostGrunnlag, GrunnlagBeregningsRequest>(
        "grunnlagberegning"
            .description("")
            .examples()
            .responds()
    ) { _, request ->

        val taskId = tasks.createTask(Regel.GRUNNLAG)

        // dette skal egentlig bli gjort av kafka-consumer når regelberegning er ferdig
        tasks.updateTask(taskId, "456")

        call.response.header(HttpHeaders.Location, "/task/$taskId")
        call.respond(HttpStatusCode.Accepted, taskResponseFromTask(tasks.getTask(taskId)))
    }

    get<GetGrunnlag>(
        "resultat av grunnlagsberegning".responds(
            ok<GrunnlagBeregningResultat>(
                example(
                    "model",
                    GrunnlagBeregningResultat.exampleGrunnlag
                )
            )
        )
    ) { param ->
        val id = param.id

        call.respond(grunnlagBeregninger.getBeregning(id))
    }
}
