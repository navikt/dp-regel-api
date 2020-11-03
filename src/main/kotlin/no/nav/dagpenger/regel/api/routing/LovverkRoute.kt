package no.nav.dagpenger.regel.api.routing

import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import io.ktor.routing.route
import kotlinx.coroutines.delay
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.BehovId
import no.nav.dagpenger.regel.api.models.Status
import no.nav.dagpenger.regel.api.models.Subsumsjon
import no.nav.dagpenger.regel.api.models.SubsumsjonId
import no.nav.dagpenger.regel.api.streams.DagpengerBehovProducer
import java.lang.RuntimeException
import java.time.LocalDate

private val LOGGER = KotlinLogging.logger {}

internal fun Routing.lovverk(store: SubsumsjonStore, producer: DagpengerBehovProducer) {
    suspend fun Subsumsjon.måReberegnes(beregningsdato: LocalDate): Boolean {
        store.getBehov(this.behovId).let { behov ->
            val nyttBehov = behov.copy(beregningsDato = beregningsdato)
            producer.produceEvent(nyttBehov)
            if (store.sjekkResultat(nyttBehov.behovId, this)) {
                return true
            }
        }
        return false
    }

    authenticate {
        route("/lovverk") {
            post("/krever-ny-behandling") {
                call.receive<KreverNyBehandlingParametre>().apply {
                    val beregningsdato = beregningsdato
                    val subsumsjonIder = subsumsjonIder.map { SubsumsjonId(it) }
                    store.getSubsumsjonerByResults(subsumsjonIder)
                        .any { subsumsjon -> subsumsjon.måReberegnes(beregningsdato) }
                        .let { call.respond(MåReberegnesResponse(it)) }
                }.also {
                    LOGGER.info("Sjekker om ${it.subsumsjonIder} må reberegnes med beregningsdato ${it.beregningsdato}.")
                }
            }
        }
    }
}

suspend fun SubsumsjonStore.sjekkResultat(behovId: BehovId, subsumsjon: Subsumsjon): Boolean {
    repeat(15) {
        when (this.behovStatus(behovId)) {
            is Status.Done -> return !(this.getSubsumsjon(behovId) sammeMinsteinntektResultatSom subsumsjon)
            is Status.Pending -> delay(1000)
        }
    }
    throw BehovTimeoutException()
}

class BehovTimeoutException : RuntimeException("Timet ut ved henting av behov")

private data class MåReberegnesResponse(val reberegnes: Boolean)

data class KreverNyBehandlingParametre(val subsumsjonIder: List<String>, val beregningsdato: LocalDate)
