package no.nav.dagpenger.regel.api.minsteinntekt

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
import no.nav.dagpenger.regel.api.VilkårProducer
import no.nav.dagpenger.regel.api.tasks.Tasks
import no.nav.dagpenger.regel.api.tasks.taskResponseFromTask
import java.time.LocalDate

private val LOGGER = KotlinLogging.logger {}

fun Routing.minsteinntekt(minsteinntektBeregninger: MinsteinntektBeregninger, tasks: Tasks, kafkaProducer: VilkårProducer) {

    route("/minsteinntekt") {
        post {
            val parametere = call.receive<MinsteinntektParametere>()

            val taskId = tasks.createTask(Regel.MINSTEINNTEKT)

            kafkaProducer.produceMinsteInntektEvent(parametere)

            tasks.updateTask(taskId, "123")

            call.response.header(HttpHeaders.Location, "/task/$taskId")
            call.respond(HttpStatusCode.Accepted, taskResponseFromTask(tasks.getTask(taskId)))
        }

        get("/{subsumsjonsid}") {
            val subsumsjonsId = call.parameters["subsumsjonsid"] ?: throw BadRequestException()

            call.respond(HttpStatusCode.OK)
        }
    }
}

data class MinsteinntektParametere(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: LocalDate
)