package no.nav.dagpenger.regel.api.grunnlag

import io.ktor.application.call
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.BadRequestException
import no.nav.dagpenger.regel.api.Regel
import no.nav.dagpenger.regel.api.DagpengerBehovProducer
import no.nav.dagpenger.regel.api.tasks.Tasks
import no.nav.dagpenger.regel.api.tasks.taskResponseFromTask
import java.time.LocalDate

private val LOGGER = KotlinLogging.logger {}

fun Routing.grunnlag(grunnlagBeregninger: GrunnlagBeregninger, tasks: Tasks, kafkaProducer: DagpengerBehovProducer) {
    route("/dagpengegrunnlag") {
        post {
            val parametere = call.receive<DagpengegrunnlagParametere>()

            val task = tasks.createTask(Regel.DAGPENGEGRUNNLAG, "temp")

            call.response.header(HttpHeaders.Location, "/task/${task.taskId}")
            call.respond(HttpStatusCode.Accepted, taskResponseFromTask(task))
        }

        get("/{subsumsjonsid}") {
            val subsumsjonsId = call.parameters["subsumsjonsid"] ?: throw BadRequestException()

            call.respond(HttpStatusCode.OK)
        }
    }
}

data class DagpengegrunnlagParametere(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: LocalDate
)
