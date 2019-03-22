package no.nav.dagpenger.regel.api.db

import no.nav.dagpenger.regel.api.Status
import no.nav.dagpenger.regel.api.SubsumsjonsBehov
import no.nav.dagpenger.regel.api.models.Subsumsjon

interface SubsumsjonStore {
    fun insertBehov(subsumsjonsBehov: SubsumsjonsBehov)

    fun behovStatus(behovId: String): Status

    fun insertSubsumsjon(subsumsjon: Subsumsjon)

    fun getSubsumsjon(subsumsjonId: String): Subsumsjon
}

class SubsumsjonNotFoundException(override val message: String) : RuntimeException(message)

class BehovNotFoundException(override val message: String) : RuntimeException(message)

class StoreException(override val message: String) : RuntimeException(message)