package no.nav.dagpenger.regel.api.routing

import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.Status
import no.nav.dagpenger.regel.api.models.SubsumsjonId
import no.nav.dagpenger.regel.api.streams.DagpengerBehovProducer
import java.time.LocalDate

internal fun Routing.lovverk(store: SubsumsjonStore, producer: DagpengerBehovProducer) {
    authenticate {
        route("/lovverk") {
            post("/krever-ny-behandling") {
                call.receive<KreverNyBehandlingParametre>().apply {
                    val beregningsdato = this.dato
                    subsumsjoner.map { subsumsjonId ->
                        store.getSubsumsjonByResult(SubsumsjonId(subsumsjonId)).let { subsumsjon ->
                            store.getBehov(subsumsjon.behovId).let { behov ->
                                val nyttBehov = behov.copy(beregningsDato = beregningsdato)
                                producer.produceEvent(nyttBehov)
                                loop@ while (true) {
                                    when (store.behovStatus(nyttBehov.behovId)) {
                                        is Status.Done -> {
                                            if (!(store.getSubsumsjon(nyttBehov.behovId) sammeMinsteinntektResultatSom subsumsjon)) {
                                                call.respond(MåReberegnesResponse(true))
                                                return@post
                                            }
                                            break@loop
                                        }
                                        is Status.Pending -> Thread.sleep(1000)
                                    }
                                }
                            }
                        }
                    }
                    call.respond(MåReberegnesResponse(false))
                }
            }.also {
                // LOGGER.info("Produserte behov ${it.behovId} for intern id  ${it.behandlingsId} med beregningsdato ${it.beregningsDato}.")
            }
        }
    }
}

private data class MåReberegnesResponse(val reberegnes: Boolean)

data class KreverNyBehandlingParametre(val subsumsjoner: List<String>, val dato: LocalDate)
