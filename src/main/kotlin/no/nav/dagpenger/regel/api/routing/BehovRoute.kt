package no.nav.dagpenger.regel.api.routing

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.MissingRequestParameterException
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import mu.KotlinLogging
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
                runCatching {
                    mapRequestToBehov(call.receive()).apply {
                        store.opprettBehov(this).also {
                            producer.produceEvent(it)
                        }.also {
                            call.response.header(HttpHeaders.Location, "${call.request.path()}/status/${it.behovId.id}")
                            call.respond(HttpStatusCode.Accepted, StatusResponse("PENDING"))
                        }.also {
                            logger.info("Produserte behov ${it.behovId} for intern id  ${it.behandlingsId} med beregningsdato ${it.beregningsDato}.")
                        }
                    }
                }.getOrElse {
                    logger.error("Feii i opprettesle av behov", it)
                    throw it
                }
            }
        }

        route("/status") {
            get("/{behovId}") {
                withContext(IO) {
                    val behovId = BehovId(call.parameters["behovid"] ?: throw MissingRequestParameterException("behovid"))

                    when (val status = store.behovStatus(behovId)) {
                        is Status.Done -> {
                            val rootPath = call.request.path().substringBefore("/behov")
                            call.response.header(HttpHeaders.Location, "$rootPath/subsumsjon/${status.behovId.id}")
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
    val id = request.regelkontekst.id ?: "0"
    return Behov(
        regelkontekst = RegelKontekst(id, request.regelkontekst.type),
        aktørId = request.aktorId,
        beregningsDato = request.beregningsdato,
        harAvtjentVerneplikt = request.harAvtjentVerneplikt,
        oppfyllerKravTilFangstOgFisk = request.oppfyllerKravTilFangstOgFisk,
        bruktInntektsPeriode = request.bruktInntektsPeriode,
        manueltGrunnlag = request.manueltGrunnlag,
        forrigeGrunnlag = request.forrigeGrunnlag,
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
    val forrigeGrunnlag: Int? = null,
    val antallBarn: Int?,
    val inntektsId: String? = null,
    val lærling: Boolean?,
    val regelverksdato: LocalDate? = null
) {
    data class RegelKontekst(val id: String? = null, val type: Kontekst)
}
