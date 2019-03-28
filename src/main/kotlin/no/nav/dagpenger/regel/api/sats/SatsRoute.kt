package no.nav.dagpenger.regel.api.sats

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

fun Routing.sats(
    satsSubsumsjoner: SatsSubsumsjoner,
    tasks: Tasks,
    kafkaProducer: DagpengerBehovProducer
) {

    route("/sats") {
        post {
            val parametere = call.receive<SatsRequestParametere>()

            // todo: what if this call or next fails? either way?
            val behov = kafkaProducer.produceSatsEvent(parametere)
            val task = tasks.createTask(Regel.SATS, behov.behovId)

            call.response.header(HttpHeaders.Location, "/task/${task.taskId}")
            call.respond(HttpStatusCode.Accepted, taskResponseFromTask(task))
        }

        get("/{subsumsjonsid}") {
            val subsumsjonsId = call.parameters["subsumsjonsid"] ?: throw BadRequestException()

            val satsSubsumsjon = satsSubsumsjoner.getSatsSubsumsjon(subsumsjonsId)

            call.respond(HttpStatusCode.OK, satsSubsumsjon)
        }
    }
}

data class SatsRequestParametere(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: LocalDate,
    val bruktInntektsPeriode: InntektsPeriode? = null,
    val antallBarn: Int? = 0
)