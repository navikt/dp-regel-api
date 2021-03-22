package no.nav.dagpenger.regel.api.streams

import mu.KotlinLogging
import no.nav.dagpenger.regel.api.Vaktmester
import no.nav.dagpenger.regel.api.db.BruktSubsumsjonStore
import no.nav.dagpenger.regel.api.db.EksternSubsumsjonBrukt
import no.nav.dagpenger.regel.api.db.SubsumsjonBruktNotFoundException

internal class BruktSubsumsjonStrategy(
    private val vaktmester: Vaktmester,
    private val bruktSubsumsjonStore: BruktSubsumsjonStore
) {
    private val logger = KotlinLogging.logger { }

    fun handle(bruktSubsumsjon: EksternSubsumsjonBrukt) {
        try {
            logger.info("Mottatt $bruktSubsumsjon ")
            val internSubsumsjonBrukt = bruktSubsumsjonStore.eksternTilInternSubsumsjon(bruktSubsumsjon)
            bruktSubsumsjonStore.insertSubsumsjonBrukt(internSubsumsjonBrukt)
            vaktmester.markerSomBrukt(internSubsumsjonBrukt)
            logger.info("Lagret $bruktSubsumsjon til database")
        } catch (e: SubsumsjonBruktNotFoundException) {
            logger.error("Fant ikke $bruktSubsumsjon i databasen")
        }
    }
}
