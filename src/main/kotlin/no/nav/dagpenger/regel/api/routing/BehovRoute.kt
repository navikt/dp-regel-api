package no.nav.dagpenger.regel.api.routing

import io.ktor.application.call
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.BadRequestException
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.Behov
import no.nav.dagpenger.regel.api.models.BehovId
import no.nav.dagpenger.regel.api.models.InntektsPeriode
import no.nav.dagpenger.regel.api.models.Kontekst
import no.nav.dagpenger.regel.api.models.RegelKontekst
import no.nav.dagpenger.regel.api.models.Status
import no.nav.dagpenger.regel.api.streams.DagpengerBehovProducer
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

internal fun Route.behov(store: SubsumsjonStore, producer: DagpengerBehovProducer) {
    route("/behov") {
        post {
            withContext(IO) {
                mapRequestToBehov(call.receive()).apply {
                    store.opprettBehov(this).also {
                        producer.produceEvent(it)
                    }.also {
                        call.response.header(HttpHeaders.Location, "/behov/status/${it.behovId.id}")
                        call.respond(HttpStatusCode.Accepted, StatusResponse("PENDING"))
                    }.also {
                        logger.info("Produserte behov ${it.behovId} for intern id  ${it.behandlingsId} med beregningsdato ${it.beregningsDato}.")
                    }
                }
            }
        }

        route("/status") {
            get("/{behovId}") {
                withContext(IO) {
                    val behovId = BehovId(call.parameters["behovid"] ?: throw BadRequestException())

                    when (val status = store.behovStatus(behovId)) {
                        is Status.Done -> {
                            call.response.header(HttpHeaders.Location, "/subsumsjon/${status.behovId.id}")
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

internal fun mapRequestToBehov(request: BehovRequest): Behov {
    val id = request.regelkontekst.id ?: "N/A"
    return Behov(
        regelkontekst = RegelKontekst(id, request.regelkontekst.type),
        aktørId = request.aktorId,
        beregningsDato = request.beregningsdato,
        harAvtjentVerneplikt = request.harAvtjentVerneplikt,
        oppfyllerKravTilFangstOgFisk = request.oppfyllerKravTilFangstOgFisk,
        bruktInntektsPeriode = request.bruktInntektsPeriode,
        manueltGrunnlag = request.manueltGrunnlag,
        antallBarn = request.antallBarn ?: 0,
        inntektsId = request.inntektsId,
        lærling = request.lærling,
        regelverksdato = request.regelverksdato
    )
}

internal data class BehovRequest(
    val regelkontekst: RegelKontekst,
    val aktorId: String,
    val beregningsdato: LocalDate,
    val harAvtjentVerneplikt: Boolean?,
    val oppfyllerKravTilFangstOgFisk: Boolean?,
    val bruktInntektsPeriode: InntektsPeriode?,
    val manueltGrunnlag: Int?,
    val antallBarn: Int?,
    val inntektsId: String? = null,
    val lærling: Boolean?,
    val regelverksdato: LocalDate? = null
) {
    data class RegelKontekst(val id: String? = null, val type: Kontekst)
}
