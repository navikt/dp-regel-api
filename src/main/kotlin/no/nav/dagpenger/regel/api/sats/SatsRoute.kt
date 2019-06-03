package no.nav.dagpenger.regel.api.sats

import io.ktor.application.call
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.dagpenger.regel.api.DagpengerBehovProducer
import no.nav.dagpenger.regel.api.Regel
import no.nav.dagpenger.regel.api.SubsumsjonsBehov
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.routes.getStatus
import no.nav.dagpenger.regel.api.routes.getSubsumsjon
import no.nav.dagpenger.regel.api.tasks.taskPending
import no.nav.dagpenger.regel.api.ulidGenerator
import java.time.LocalDate

fun Routing.sats(
    store: SubsumsjonStore,
    kafkaProducer: DagpengerBehovProducer
) {
    val regel = Regel.SATS

    route("/sats") {
        post {
            mapRequestToBehov(call.receive()).apply {
                store.insertBehov(this, regel)
                kafkaProducer.produceEvent(this)
            }.also {
                call.response.header(HttpHeaders.Location, "/sats/status/${it.behovId}")
                call.respond(HttpStatusCode.Accepted, taskPending(regel))
            }
        }

        getSubsumsjon(regel, store)

        getStatus(regel, store)
    }
}

fun mapRequestToBehov(request: SatsRequestParametere) = SubsumsjonsBehov(
    ulidGenerator.nextULID(),
    request.aktorId,
    request.vedtakId,
    request.beregningsdato,
    antallBarn = request.antallBarn,
    manueltGrunnlag = request.manueltGrunnlag,
    harAvtjentVerneplikt = request.harAvtjentVerneplikt,
    oppfyllerKravTilFangstOgFisk = request.oppfyllerKravTilFangstOgFisk
)

data class SatsRequestParametere(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: LocalDate,
    val manueltGrunnlag: Int? = null,
    val antallBarn: Int? = 0,
    val harAvtjentVerneplikt: Boolean = false,
    val oppfyllerKravTilFangstOgFisk: Boolean = false
)