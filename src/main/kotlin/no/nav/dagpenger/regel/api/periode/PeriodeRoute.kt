package no.nav.dagpenger.regel.api.periode

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
import no.nav.dagpenger.regel.api.models.common.InntektsPeriode
import no.nav.dagpenger.regel.api.tasks.Tasks
import no.nav.dagpenger.regel.api.tasks.taskResponseFromTask
import java.time.LocalDate

private val LOGGER = KotlinLogging.logger {}

fun Routing.periode(
    periodeSubsumsjoner: PeriodeSubsumsjoner,
    tasks: Tasks,
    kafkaProducer: DagpengerBehovProducer
) {

    route("/periode") {
        post {
            val parametere = call.receive<PeriodeRequestParametere>()

            // todo: what if this call or next fails? either way?
            val behovId = kafkaProducer.producePeriodeEvent(parametere)
            val task = tasks.createTask(Regel.PERIODE, behovId)

            call.response.header(HttpHeaders.Location, "/task/${task.taskId}")
            call.respond(HttpStatusCode.Accepted, taskResponseFromTask(task))
        }

        get("/{subsumsjonsid}") {
            val subsumsjonsId = call.parameters["subsumsjonsid"] ?: throw BadRequestException()

            val periodeSubsumsjon = periodeSubsumsjoner.getPeriodeSubsumsjon(subsumsjonsId)

            call.respond(HttpStatusCode.OK, periodeSubsumsjon)
        }
    }
}

data class PeriodeRequestParametere(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: LocalDate,
    val harAvtjentVerneplikt: Boolean = false,
    val bruktInntektsPeriode: InntektsPeriode? = null
)