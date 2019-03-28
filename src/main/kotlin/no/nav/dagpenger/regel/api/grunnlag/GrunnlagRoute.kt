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
import no.nav.dagpenger.regel.api.models.common.InntektsPeriode
import no.nav.dagpenger.regel.api.tasks.Tasks
import no.nav.dagpenger.regel.api.tasks.taskResponseFromTask
import java.time.LocalDate

private val LOGGER = KotlinLogging.logger {}

fun Routing.grunnlag(grunnlagsubsumsjoner: GrunnlagSubsumsjoner, tasks: Tasks, kafkaProducer: DagpengerBehovProducer) {
    route("/grunnlag") {
        post {
            val parametere = call.receive<GrunnlagRequestParametere>()

            // todo: what if this call or next fails? either way?
            val behov = kafkaProducer.produceGrunnlagEvent(parametere)
            val task = tasks.createTask(Regel.GRUNNLAG, behov.behovId)

            call.response.header(HttpHeaders.Location, "/task/${task.taskId}")
            call.respond(HttpStatusCode.Accepted, taskResponseFromTask(task))
        }

        get("/{subsumsjonsid}") {
            val subsumsjonsId = call.parameters["subsumsjonsid"] ?: throw BadRequestException()

            val grunnlagSubsumsjon = grunnlagsubsumsjoner.getGrunnlagSubsumsjon(subsumsjonsId)

            call.respond(HttpStatusCode.OK, grunnlagSubsumsjon)
        }
    }
}

data class GrunnlagRequestParametere(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: LocalDate,
    val harAvtjentVerneplikt: Boolean? = false,
    val oppfyllerKravTilFangstOgFisk: Boolean = false,
    val bruktInntektsPeriode: InntektsPeriode? = null,
    val manueltGrunnlag: Int? = null
)
