package no.nav.dagpenger.regel.api.db

import de.huxhorn.sulky.ulid.ULID
import no.nav.dagpenger.regel.api.models.Behov
import no.nav.dagpenger.regel.api.models.EksternId
import no.nav.dagpenger.regel.api.models.InternId
import no.nav.dagpenger.regel.api.models.Status
import no.nav.dagpenger.regel.api.models.Subsumsjon
import java.lang.IllegalArgumentException

internal interface SubsumsjonStore {
    fun insertBehov(behov: Behov): Int

    fun behovStatus(id: String): Status

    fun insertSubsumsjon(subsumsjon: Subsumsjon): Int

    fun getSubsumsjon(id: String): Subsumsjon

    fun getSubsumsjonByResult(subsumsjonId: SubsumsjonId): Subsumsjon
    fun hentKoblingTilEkstern(eksternId: EksternId): InternId
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
