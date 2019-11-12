package no.nav.dagpenger.regel.api.db

import de.huxhorn.sulky.ulid.ULID
import no.nav.dagpenger.regel.api.models.BehandlingsId
import no.nav.dagpenger.regel.api.models.Behov
import no.nav.dagpenger.regel.api.models.EksternId
import no.nav.dagpenger.regel.api.models.InternBehov
import no.nav.dagpenger.regel.api.models.Kontekst
import no.nav.dagpenger.regel.api.models.Status
import no.nav.dagpenger.regel.api.models.Subsumsjon
import java.time.ZoneId
import java.time.ZonedDateTime

interface SubsumsjonStore {

    fun opprettBehov(behov: Behov): InternBehov {
        val eksternId = EksternId(behov.vedtakId.toString(), Kontekst.VEDTAK)
        val behandlingsId = hentKoblingTilEkstern(eksternId)
        val internBehov = InternBehov.fromBehov(behov, behandlingsId)
        insertBehov(internBehov)
        return internBehov
    }

    fun insertBehov(behov: InternBehov): Int
    fun hentKoblingTilEkstern(eksternId: EksternId): BehandlingsId
    fun getBehov(behovId: String): InternBehov
    fun behovStatus(behovId: String): Status
    fun insertSubsumsjon(subsumsjon: Subsumsjon, created: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"))): Int
    fun getSubsumsjon(behovId: String): Subsumsjon
    fun getSubsumsjonByResult(subsumsjonId: SubsumsjonId): Subsumsjon
    fun delete(subsumsjon: Subsumsjon)
}

data class SubsumsjonId(val id: String) {
    init {
        try {
            val uppercaseId = id.toUpperCase()
            ULID.parseULID(uppercaseId)
        } catch (e: IllegalArgumentException) {
            throw IllegalSubsumsjonIdException("Id $id is not a valid subsumsjon id")
        }
    }
}

class IllegalSubsumsjonIdException(override val message: String) : RuntimeException(message)

internal class SubsumsjonNotFoundException(override val message: String) : RuntimeException(message)

internal class BehovNotFoundException(override val message: String) : RuntimeException(message)

internal class StoreException(override val message: String) : RuntimeException(message)
