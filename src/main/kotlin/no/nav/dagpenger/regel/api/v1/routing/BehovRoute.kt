package no.nav.dagpenger.regel.api.v1.routing

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
import no.nav.dagpenger.regel.api.BadRequestException
import no.nav.dagpenger.regel.api.v1.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.v1.models.Behov
import no.nav.dagpenger.regel.api.v1.models.InntektsPeriode
import no.nav.dagpenger.regel.api.v1.models.Status
import no.nav.dagpenger.regel.api.v1.streams.DagpengerBehovProducer
import java.time.LocalDate

internal fun Routing.behov(store: SubsumsjonStore, producer: DagpengerBehovProducer) {
    route("/behov") {
        post {
            mapRequestToBehov(call.receive()).apply {
                store.insertBehov(this)
                producer.produceEvent(this)
            }.also {
                call.response.header(HttpHeaders.Location, "/behov/status/${it.behovId}")
                call.respond(HttpStatusCode.Accepted, StatusResponse("PENDING"))
            }
        }

        route("/status") {
            get("/{behovId}") {
                val behovId = call.parameters["behovid"] ?: throw BadRequestException()

                when (val status = store.behovStatus(behovId)) {
                    is Status.Done -> {
                        call.response.header(HttpHeaders.Location, "/subsumsjon/${status.subsumsjonsId}")
                        call.respond(HttpStatusCode.SeeOther)
                    }
                    is Status.Pending -> {
                        call.respond(StatusResponse("PENDING"))
                    }
                }
            }
        }
    }
}

private data class StatusResponse(val status: String)

private fun mapRequestToBehov(request: BehovRequest): Behov = Behov(
    aktørId = request.aktorId,
    vedtakId = request.vedtakId,
    beregningsDato = request.beregningsdato,
    harAvtjentVerneplikt = request.harAvtjentVerneplikt,
    oppfyllerKravTilFangstOgFisk = request.oppfyllerKravTilFangstOgFisk,
    bruktInntektsPeriode = request.bruktInntektsPeriode,
    manueltGrunnlag = request.manueltGrunnlag,
    antallBarn = request.antallBarn
)

private data class BehovRequest(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: LocalDate,
    val harAvtjentVerneplikt: Boolean?,
    val oppfyllerKravTilFangstOgFisk: Boolean?,
    val bruktInntektsPeriode: InntektsPeriode?,
    val manueltGrunnlag: Int?,
    val antallBarn: Int?
)
