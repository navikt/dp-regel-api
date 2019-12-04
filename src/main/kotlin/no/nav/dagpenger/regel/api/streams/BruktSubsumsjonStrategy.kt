package no.nav.dagpenger.regel.api.streams

import mu.KotlinLogging
import no.nav.dagpenger.regel.api.Vaktmester
import no.nav.dagpenger.regel.api.db.BruktSubsumsjonStore
import no.nav.dagpenger.regel.api.db.EksternSubsumsjonBrukt

internal class BruktSubsumsjonStrategy(private val vaktmester: Vaktmester, private val bruktSubsumsjonStore: BruktSubsumsjonStore) {
    private val logger = KotlinLogging.logger { }

    fun handle(brukteSubsumsjoner: Sequence<EksternSubsumsjonBrukt>) {
        brukteSubsumsjoner
            .onEach { b -> logger.info("Received $b ") }
            .filterNot { vedtak ->
                "AVSLU" == vedtak.vedtakStatus && "AVBRUTT" == vedtak.utfall
            }
            .onEach { b -> logger.info("Saving $b to database") }
            .forEach {
                val internSubsumsjonBrukt = bruktSubsumsjonStore.eksternTilInternSubsumsjon(it)
                bruktSubsumsjonStore.insertSubsumsjonBrukt(internSubsumsjonBrukt)
                vaktmester.markerSomBrukt(internSubsumsjonBrukt)
            }
    }
}