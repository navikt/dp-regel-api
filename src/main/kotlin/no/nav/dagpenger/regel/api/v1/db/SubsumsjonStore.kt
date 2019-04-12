package no.nav.dagpenger.regel.api.v1.db

import no.nav.dagpenger.regel.api.v1.models.Behov
import no.nav.dagpenger.regel.api.v1.models.Status
import no.nav.dagpenger.regel.api.v1.models.Subsumsjon

interface SubsumsjonStore {
    fun insertBehov(behov: Behov): Int

    fun behovStatus(id: String): Status

    fun insertSubsumsjon(subsumsjon: Subsumsjon): Int

    fun getSubsumsjon(id: String): Subsumsjon
}

class SubsumsjonNotFoundException(override val message: String) : RuntimeException(message)

class BehovNotFoundException(override val message: String) : RuntimeException(message)

class StoreException(override val message: String) : RuntimeException(message)

