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
import no.nav.dagpenger.regel.api.DagpengerBehovProducer
import no.nav.dagpenger.regel.api.Regel
import no.nav.dagpenger.regel.api.tasks.Tasks
import no.nav.dagpenger.regel.api.tasks.taskResponseFromTask
import java.time.LocalDate

private val LOGGER = KotlinLogging.logger {}

fun Routing.grunnlag(dagpengegrunnlagBeregninger: DagpengegrunnlagBeregninger, tasks: Tasks, kafkaProducer: DagpengerBehovProducer) {
    route("/dagpengegrunnlag") {
        post {
            val parametere = call.receive<DagpengegrunnlagParametere>()

            // todo: what if this call or next fails? either way?
            val behov = kafkaProducer.produceDagpengegrunnlagEvent(parametere)
            val task = tasks.createTask(Regel.DAGPENGEGRUNNLAG, behov.behovId)

            call.response.header(HttpHeaders.Location, "/task/${task.taskId}")
            call.respond(HttpStatusCode.Accepted, taskResponseFromTask(task))
        }

        get("/{subsumsjonsid}") {
            val subsumsjonsId = call.parameters["subsumsjonsid"] ?: throw BadRequestException()

            val dagpengegrunnlagBeregning = dagpengegrunnlagBeregninger.getGrunnlagBeregning(subsumsjonsId)

            call.respond(HttpStatusCode.OK, dagpengegrunnlagBeregning)
        }
    }
}

data class DagpengegrunnlagParametere(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: LocalDate
)
