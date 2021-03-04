package no.nav.dagpenger.regel.api.routing

import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.Behov
import no.nav.dagpenger.regel.api.models.BehovId
import no.nav.dagpenger.regel.api.models.InternBehov
import no.nav.dagpenger.regel.api.models.Kontekst
import no.nav.dagpenger.regel.api.models.RegelKontekst
import no.nav.dagpenger.regel.api.models.Status
import no.nav.dagpenger.regel.api.models.Subsumsjon
import no.nav.dagpenger.regel.api.models.SubsumsjonId
import no.nav.dagpenger.regel.api.streams.DagpengerBehovProducer
import java.time.LocalDate

private val LOGGER = KotlinLogging.logger {}

internal fun Route.lovverk(store: SubsumsjonStore, producer: DagpengerBehovProducer) {
    suspend fun Subsumsjon.måReberegnes(beregningsdato: LocalDate): Boolean {
        store.getBehov(this.behovId).let { internBehov ->
            val behov = store.opprettBehov(internBehov.tilBehov(beregningsdato))
            producer.produceEvent(behov)
            if (store.sjekkResultat(behov.behovId, this)) {
                return true
            }
        }
        return false
    }
    route("/lovverk/vurdering") {
        post("/minsteinntekt") {
            withContext(Dispatchers.IO) {
                call.receive<KreverNyBehandlingParametre>().apply {
                    val beregningsdato = beregningsdato
                    val subsumsjonIder = subsumsjonIder.map { SubsumsjonId(it) }
                    store.getSubsumsjonerByResults(subsumsjonIder)
                        .any { subsumsjon -> subsumsjon.måReberegnes(beregningsdato) }
                        .let { call.respond(KreverNyVurdering(it)) }
                }.also {
                    LOGGER.info("Vurder om minsteinntekt må reberegnes for subsumsjoner ${it.subsumsjonIder} beregningsdato ${it.beregningsdato}.")
                }
            }
        }
    }
}

private const val UBRUKT_VEDTAK_ID = -9999

private fun InternBehov.tilBehov(beregningsdato: LocalDate) =
    Behov(
        regelkontekst = RegelKontekst("$UBRUKT_VEDTAK_ID", Kontekst.REVURDERING),
        aktørId = this.aktørId,
        beregningsDato = beregningsdato,
        harAvtjentVerneplikt = this.harAvtjentVerneplikt,
        oppfyllerKravTilFangstOgFisk = this.oppfyllerKravTilFangstOgFisk,
        bruktInntektsPeriode = this.bruktInntektsPeriode,
        antallBarn = this.antallBarn,
        manueltGrunnlag = this.manueltGrunnlag,
        inntektsId = this.inntektsId,
        lærling = this.lærling
    )

suspend fun SubsumsjonStore.sjekkResultat(behovId: BehovId, subsumsjon: Subsumsjon): Boolean {
    repeat(15) {
        LOGGER.info("Sjekker resultat. Runde: $it. For behov: $behovId og subsumsjon: $subsumsjon")
        when (this.behovStatus(behovId)) {
            is Status.Done -> return !(this.getSubsumsjon(behovId) sammeMinsteinntektResultatSom subsumsjon)
            is Status.Pending -> delay(1000)
        }
    }
    throw BehovTimeoutException()
}

class BehovTimeoutException : RuntimeException("Timet ut ved henting av behov")

private data class KreverNyVurdering(val nyVurdering: Boolean)

data class KreverNyBehandlingParametre(val subsumsjonIder: List<String>, val beregningsdato: LocalDate)
