package no.nav.dagpenger.regel.api.minsteinntekt

import io.ktor.application.call
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.dagpenger.regel.api.BruktInntektsPeriode
import no.nav.dagpenger.regel.api.DagpengerBehovProducer
import no.nav.dagpenger.regel.api.Regel
import no.nav.dagpenger.regel.api.SubsumsjonsBehov
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.InntektsPeriode
import no.nav.dagpenger.regel.api.routes.getStatus
import no.nav.dagpenger.regel.api.routes.getSubsumsjon
import no.nav.dagpenger.regel.api.senesteInntektsmåned
import no.nav.dagpenger.regel.api.tasks.taskPending
import no.nav.dagpenger.regel.api.ulidGenerator
import java.time.LocalDate

fun Routing.minsteinntekt(
    store: SubsumsjonStore,
    kafkaProducer: DagpengerBehovProducer
) {

    val regel = Regel.MINSTEINNTEKT

    route("/minsteinntekt") {
        post {
            mapRequestToBehov(call.receive()).apply {
                store.insertBehov(this, regel)
                kafkaProducer.produceEvent(this)
            }.also {
                call.response.header(HttpHeaders.Location, "/minsteinntekt/status/${it.behovId}")
                call.respond(HttpStatusCode.Accepted, taskPending(regel))
            }
        }

        getSubsumsjon(regel, store)

        getStatus(regel, store)
    }
}

private fun mapRequestToBehov(request: MinsteinntektRequestParametere) =
    SubsumsjonsBehov(
        ulidGenerator.nextULID(),
        request.aktorId,
        request.vedtakId,
        request.beregningsdato,
        request.harAvtjentVerneplikt,
        request.oppfyllerKravTilFangstOgFisk,
        senesteInntektsmåned = senesteInntektsmåned(request.beregningsdato),
        bruktInntektsPeriode = request.bruktInntektsPeriode?.let { BruktInntektsPeriode(it.førsteMåned, it.sisteMåned) }
    )

data class MinsteinntektRequestParametere(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: LocalDate,
    val harAvtjentVerneplikt: Boolean? = false,
    val oppfyllerKravTilFangstOgFisk: Boolean? = false,
    val bruktInntektsPeriode: InntektsPeriode? = null
)
