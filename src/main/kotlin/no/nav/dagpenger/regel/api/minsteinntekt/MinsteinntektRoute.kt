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
import no.nav.dagpenger.regel.api.DagpengerBehovProducer
import no.nav.dagpenger.regel.api.models.common.InntektsPeriode
import no.nav.dagpenger.regel.api.tasks.Tasks
import no.nav.dagpenger.regel.api.tasks.taskResponseFromTask
import java.time.LocalDate

private val LOGGER = KotlinLogging.logger {}

fun Routing.minsteinntekt(
    minsteinntektSubsumsjoner: MinsteinntektSubsumsjoner,
    tasks: Tasks,
    kafkaProducer: DagpengerBehovProducer
) {

    route("/minsteinntekt") {
        post {
            val parametere = call.receive<MinsteinntektRequestParametere>()

            // todo: what if this call or next fails? either way?
            val behov = kafkaProducer.produceMinsteInntektEvent(parametere)
            val task = tasks.createTask(Regel.MINSTEINNTEKT, behov.behovId)

            call.response.header(HttpHeaders.Location, "/task/${task.taskId}")
            call.respond(HttpStatusCode.Accepted, taskResponseFromTask(task))
        }

        get("/{subsumsjonsid}") {
            val subsumsjonsId = call.parameters["subsumsjonsid"] ?: throw BadRequestException()

            val minsteinntektSubsumsjon = minsteinntektSubsumsjoner.getMinsteinntektSubsumsjon(subsumsjonsId)

            call.respond(HttpStatusCode.OK, minsteinntektSubsumsjon)
        }
    }
}

data class MinsteinntektRequestParametere(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: LocalDate,
    val harAvtjentVerneplikt: Boolean? = false,
    val oppfyllerKravTilFangstOgFisk: Boolean? = false,
    val bruktInntektsPeriode: InntektsPeriode? = null
)

