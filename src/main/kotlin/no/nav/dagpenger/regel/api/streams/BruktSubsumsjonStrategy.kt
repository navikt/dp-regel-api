package no.nav.dagpenger.regel.api.streams

import mu.KotlinLogging
import no.nav.dagpenger.regel.api.Vaktmester
import no.nav.dagpenger.regel.api.db.BruktSubsumsjonStore
import no.nav.dagpenger.regel.api.db.EksternSubsumsjonBrukt
import no.nav.dagpenger.regel.api.db.SubsumsjonBruktNotFoundException
import no.nav.dagpenger.regel.api.db.SubsumsjonNotFoundException
import no.nav.dagpenger.regel.api.models.Faktum
import no.nav.dagpenger.regel.api.models.SubsumsjonId

internal class BruktSubsumsjonStrategy(
    private val vaktmester: Vaktmester,
    private val bruktSubsumsjonStore: BruktSubsumsjonStore
) {
    private val logger = KotlinLogging.logger { }

    fun handle(bruktSubsumsjon: EksternSubsumsjonBrukt): Faktum? {
        return try {
            logger.info("Mottatt $bruktSubsumsjon ")
            val internSubsumsjonBrukt = bruktSubsumsjonStore.eksternTilInternSubsumsjon(bruktSubsumsjon)
            bruktSubsumsjonStore.insertSubsumsjonBrukt(internSubsumsjonBrukt)
            vaktmester.markerSomBrukt(internSubsumsjonBrukt)
            logger.info("Lagret $bruktSubsumsjon til database")

            bruktSubsumsjonStore.getSubsumsjonByResult(SubsumsjonId(internSubsumsjonBrukt.id)).faktum
        } catch (e: SubsumsjonNotFoundException) {
            logger.warn(e) { "Fant ikke subsumsjon med id ${bruktSubsumsjon.id}, ekstern id ${bruktSubsumsjon.eksternId}" }
            null
        } catch (e: SubsumsjonBruktNotFoundException) {
            logger.warn(e) { "Fant ekstern id for ${bruktSubsumsjon.id}, ekstern id ${bruktSubsumsjon.eksternId}" }
            null
        }
    }
}
