package no.nav.dagpenger.regel.api.routing

import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import io.ktor.routing.route
import kotlinx.coroutines.delay
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.BehovId
import no.nav.dagpenger.regel.api.models.Status
import no.nav.dagpenger.regel.api.models.Subsumsjon
import no.nav.dagpenger.regel.api.models.SubsumsjonId
import no.nav.dagpenger.regel.api.streams.DagpengerBehovProducer
import java.time.LocalDate

internal fun Routing.lovverk(store: SubsumsjonStore, producer: DagpengerBehovProducer) {
    authenticate {
        route("/lovverk") {
            post("/krever-ny-behandling") {
                call.receive<KreverNyBehandlingParametre>().apply {
                    val beregningsdato = this.dato
                    val måReberegnes =
                        store.getSubsumsjonerByResults(this.subsumsjoner.map { SubsumsjonId(it) }).any { subsumsjon ->
                            Blbalb(store, producer).måReberegnes(subsumsjon, beregningsdato)
                            /*store.getBehov(subsumsjon.behovId).let { behov ->
                                val nyttBehov = behov.copy(beregningsDato = beregningsdato)
                                producer.produceEvent(nyttBehov)
                                if (store.sjekkResultat(nyttBehov.behovId, subsumsjon)) {
                                    call.respond(MåReberegnesResponse(true))
                                    return@post
                                }
                            }*/
                        }
                    call.respond(MåReberegnesResponse(måReberegnes))
                }
            }.also {
                // LOGGER.info("Produserte behov ${it.behovId} for intern id  ${it.behandlingsId} med beregningsdato ${it.beregningsDato}.")
            }
        }
    }
}

suspend fun SubsumsjonStore.sjekkResultat(behovId: BehovId, subsumsjon: Subsumsjon): Boolean {
    var i = 0
    while (i < 15) {
        when (this.behovStatus(behovId)) {
            is Status.Done -> {
                if (!(this.getSubsumsjon(behovId) sammeMinsteinntektResultatSom subsumsjon)) {
                    return true
                }
                return false
            }
            is Status.Pending -> delay(1000).also { i++ }
        }
    }
}

private data class MåReberegnesResponse(val reberegnes: Boolean)

data class KreverNyBehandlingParametre(val subsumsjoner: List<String>, val dato: LocalDate)
