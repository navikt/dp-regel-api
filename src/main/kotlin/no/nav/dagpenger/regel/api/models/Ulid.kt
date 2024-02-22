package no.nav.dagpenger.regel.api.models

import de.huxhorn.sulky.ulid.ULID

open class Ulid(private val rawId: String) {
    val id: String

    init {
        try {
            id = ULID.parseULID(rawId).toString()
        } catch (e: IllegalArgumentException) {
            throw IllegalUlidException("Id $rawId is not a valid uppercase ulid")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Ulid

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Ulid(id='$id')"
    }
}

class BehovId(rawId: String) : Ulid(rawId)

class SubsumsjonId(rawId: String) : Ulid(rawId)

class IllegalUlidException(override val message: String) : RuntimeException(message)
