package no.nav.dagpenger.regel.api.models

import de.huxhorn.sulky.ulid.ULID
import no.nav.dagpenger.regel.api.db.IllegalSubsumsjonIdException

data class UlidId private constructor (val id: String) {

    companion object {
        operator fun invoke(id: String): UlidId {
            return UlidId(id.toUpperCase())
        }
    }

    init {
        try {
            ULID.parseULID(id)
        } catch (e: IllegalArgumentException) {
            throw IllegalSubsumsjonIdException("Id $id is not a valid ulid")
        }
    }
}
