package no.nav.dagpenger.regel.api.db

interface SubsumsjonStore {
    fun get(subsumsjonsId: String): String

    fun insert(subsumsjonsId: String, json: String)

    fun isHealthy(): Boolean
}

class SubsumsjonNotFoundException(override val message: String) : RuntimeException(message)