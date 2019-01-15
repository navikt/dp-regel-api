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
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.KafkaProducer
import no.nav.dagpenger.regel.api.Regel
import no.nav.dagpenger.regel.api.minsteinntekt.InntektsPeriode
import no.nav.dagpenger.regel.api.tasks.Tasks
import no.nav.dagpenger.regel.api.tasks.taskResponseFromTask

@KtorExperimentalLocationsAPI
@Group("Grunnlag")
@Location("/dagpengegrunnlag")
class PostGrunnlag

@Group("Grunnlag")
@Location("/dagpengegrunnlag")
data class GetGrunnlagWithAktorId(val aktorId: String)

@Group("Grunnlag")
@Location("/dagpengegrunnlag/{beregningsId}")
data class GetGrunnlag(val beregningsId: String)

private val LOGGER = KotlinLogging.logger {}

fun Routing.grunnlag(grunnlagBeregninger: GrunnlagBeregninger, tasks: Tasks, kafkaProducer: KafkaProducer) {
    post<PostGrunnlag, GrunnlagBeregningsRequest>(
        "grunnlagberegning"
            .description("")
            .examples()
            .responds()
    ) { _, request ->

        val taskId = tasks.createTask(Regel.DAGPENGEGRUNNLAG)
        kafkaProducer.processRegel(request)
        // dette skal egentlig bli gjort av kafka-consumer når regelberegning er ferdig
        tasks.updateTask(taskId, "456")

        call.response.header(HttpHeaders.Location, "/task/$taskId")
        call.respond(HttpStatusCode.Accepted, taskResponseFromTask(tasks.getTask(taskId)))
    }

    get<GetGrunnlagWithAktorId>(
        "hent alle minsteinntektsberegninger for aktør"
            .description("??")
            .examples()
            .responds()
    ) { param ->
        LOGGER.info { param.aktorId }
        call.respond(grunnlagBeregninger.getBeregningForAktorId(param.aktorId))
    }
    get<GetGrunnlag>(
        "resultat av grunnlagsberegning".responds(
            ok<GrunnlagBeregningsResultat>(
                example(
                    "model"
                )
            )
        )
    ) { param ->
        val id = param.beregningsId

        LOGGER.info { param.beregningsId }

        call.respond(grunnlagBeregninger.getBeregning(id))
    }
}

data class GrunnlagBeregningsRequest(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: String,
    val inntektsId: String,
    val bruktinntektsPeriode: InntektsPeriode,
    val harAvtjentVerneplikt: Boolean,
    val oppfyllerKravTilFangstOgFisk: Boolean,
    val harArbeidsperiodeEosSiste12Maaneder: Boolean
)

data class GrunnlagBeregningsResultat(
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

data class Utfall(
    val oppfyllerKravtilMinsteArbeidsinntekt: Boolean,
    val periodeAntallUker: Int
)

data class Parametere(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: String,
    val inntektsId: String,
    val bruktinntektsPeriode: InntektsPeriode
)
