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
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.Regel
import no.nav.dagpenger.regel.api.grunnlag.Parametere
import no.nav.dagpenger.regel.api.grunnlag.Utfall
import no.nav.dagpenger.regel.api.tasks.Tasks
import no.nav.dagpenger.regel.api.tasks.taskResponseFromTask

@Group("Minsteinntekt")
@Location("/minsteinntekt")
class PostMinsteinntekt

@Group("Minsteinntekt")
@Location("/minsteinntekt")
data class GetMinsteinntektWithAktorId(val aktorId: String)

@Group("Minsteinntekt")
@Location("/minsteinntekt/{beregningsId}")
data class GetMinsteinntekt(val beregningsId: String)

private val LOGGER = KotlinLogging.logger {}

fun Routing.minsteinntekt(minsteinntektBeregninger: MinsteinntektBeregninger, tasks: Tasks) {
    post<PostMinsteinntekt, MinsteinntektBeregningsRequest>(
        "minsteinntektsberegning"
            .description("Kjør en beregning av minsteinntekt")
            .examples()
            .responds()
    ) { _, payload ->
        val taskId = tasks.createTask(Regel.MINSTEINNTEKT)

        // dette skal egentlig bli gjort av kafka-consumer når regelberegning er ferdig
        tasks.updateTask(taskId, "123")

        call.response.header(HttpHeaders.Location, "/task/$taskId")
        call.respond(HttpStatusCode.Accepted, taskResponseFromTask(tasks.getTask(taskId)))
    }

    get<GetMinsteinntektWithAktorId>(
        "hent alle minsteinntektsberegninger for aktør"
            .description("??")
            .examples()
            .responds()
    ) { param ->
        LOGGER.info { param.aktorId }
        call.respond(minsteinntektBeregninger.getBeregningForAktorId(param.aktorId))
    }

    get<GetMinsteinntekt>(
        "resultat av minsteinntektsberegning"
            .responds(
                ok<MinsteinntektBeregningsResult>(
                    example(
                        "model",
                        MinsteinntektBeregningsResult.exampleInntektBeregning
                    )
                )
            )
    ) { param ->
        LOGGER.info { param.beregningsId }
        val id = param.beregningsId

        call.respond(minsteinntektBeregninger.getBeregningForBeregningsId(id))
    }
}

data class MinsteinntektBeregningsRequest(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: String,
    val inntektsId: String,
    val bruktinntektsPeriode: InntektsPeriode,
    val harAvtjentVerneplikt: Boolean,
    val oppfyllerKravTilFangstOgFisk: Boolean,
    val harArbeidsperiodeEosSiste12Maaneder: Boolean
)

data class MinsteinntektBeregningsResult(
    val beregningsId: String,
    val utfall: Utfall,
    val opprettet: String,
    val utfort: String,
    val parametere: Parametere,
    val harAvtjentVerneplikt: Boolean,
    val oppfyllerKravTilFangstOgFisk: Boolean,
    val harArbeidsperiodeEosSiste12Maaneder: Boolean
) {
    companion object {
        val exampleInntektBeregning = mapOf(
            "oppfyllerMinsteinntekt" to true,
            "status" to 1
        )
    }
}

data class InntektsPeriode(
    val foersteMaaned: String,
    val sisteMaaned: String
)

class InvalidInputException(override val message: String) : RuntimeException(message)
