package no.nav.dagpenger.regel.api.routing

import io.ktor.application.call
import io.ktor.auth.authenticate
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
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.Behov
import no.nav.dagpenger.regel.api.models.BehovId
import no.nav.dagpenger.regel.api.models.InntektsPeriode
import no.nav.dagpenger.regel.api.models.Status
import no.nav.dagpenger.regel.api.streams.DagpengerBehovProducer
import java.time.LocalDate

private val LOGGER = KotlinLogging.logger {}

internal fun Routing.behov(store: SubsumsjonStore, producer: DagpengerBehovProducer) {
    authenticate {
        route("/behov") {
            post {
                mapRequestToBehov(call.receive()).apply {
                    store.opprettBehov(this).also {
                        producer.produceEvent(it)
                    }.also {
                        call.response.header(HttpHeaders.Location, "/behov/status/${it.behovId}")
                        call.respond(HttpStatusCode.Accepted, StatusResponse("PENDING"))
                    }.also {
                        LOGGER.info("Produserte behov ${it.behovId} for intern id  ${it.behandlingsId} med beregningsdato ${it.beregningsDato}.")
                    }
                }
            }

            route("/status") {
                get("/{behovId}") {
                    val behovId = BehovId(call.parameters["behovid"] ?: throw BadRequestException())

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
}

private data class StatusResponse(val status: String)

internal fun mapRequestToBehov(request: BehovRequest): Behov = Behov(
    aktørId = request.aktorId,
    vedtakId = request.vedtakId,
    beregningsDato = request.beregningsdato,
    harAvtjentVerneplikt = request.harAvtjentVerneplikt,
    oppfyllerKravTilFangstOgFisk = request.oppfyllerKravTilFangstOgFisk,
    bruktInntektsPeriode = request.bruktInntektsPeriode,
    manueltGrunnlag = request.manueltGrunnlag,
    antallBarn = request.antallBarn ?: 0,
    inntektsId = request.inntektsId
)

internal data class BehovRequest(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: LocalDate,
    val harAvtjentVerneplikt: Boolean?,
    val oppfyllerKravTilFangstOgFisk: Boolean?,
    val bruktInntektsPeriode: InntektsPeriode?,
    val manueltGrunnlag: Int?,
    val antallBarn: Int?,
    val inntektsId: String? = null
)
