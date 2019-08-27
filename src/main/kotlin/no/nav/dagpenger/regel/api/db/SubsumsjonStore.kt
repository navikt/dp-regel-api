package no.nav.dagpenger.regel.api.db


import de.huxhorn.sulky.ulid.ULID
import no.nav.dagpenger.regel.api.models.Behov
import no.nav.dagpenger.regel.api.models.EksternId
import no.nav.dagpenger.regel.api.models.InternBehov
import no.nav.dagpenger.regel.api.models.InternId
import no.nav.dagpenger.regel.api.models.Kontekst
import no.nav.dagpenger.regel.api.models.Status
import no.nav.dagpenger.regel.api.models.Subsumsjon

internal interface SubsumsjonStore {


    fun opprettBehov(behov: Behov): InternBehov {
        val eksternId = EksternId(behov.vedtakId.toString(), Kontekst.VEDTAK)
        val internId = hentKoblingTilEkstern(eksternId)
        val internBehov = InternBehov.fromBehov(behov, internId)
        insertBehov(internBehov)
        return internBehov
    }
    fun insertBehov(behov: InternBehov): Int
    fun hentKoblingTilEkstern(eksternId: EksternId): InternId

    fun behovStatus(id: String): Status

    fun insertSubsumsjon(subsumsjon: Subsumsjon): Int
    fun getSubsumsjon(id: String): Subsumsjon
    fun getSubsumsjonByResult(subsumsjonId: SubsumsjonId): Subsumsjon
}

data class SubsumsjonId(val id: String) {
    init {
        try {
            ULID.parseULID(id)
        } catch (e: IllegalArgumentException) {
            throw IllegalSubsumsjonIdException("Id $id is not a valid subsumsjon id")
        }
    }
}

class IllegalSubsumsjonIdException(override val message: String) : RuntimeException(message)

internal class SubsumsjonNotFoundException(override val message: String) : RuntimeException(message)

internal class BehovNotFoundException(override val message: String) : RuntimeException(message)

internal class StoreException(override val message: String) : RuntimeException(message)
