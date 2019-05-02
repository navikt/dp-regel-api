package no.nav.dagpenger.regel.api.v1.db

import no.nav.dagpenger.regel.api.v1.models.Behov
import no.nav.dagpenger.regel.api.v1.models.Status
import no.nav.dagpenger.regel.api.v1.models.Subsumsjon

internal interface SubsumsjonStore {
    fun insertBehov(behov: Behov): Int

    fun behovStatus(id: String): Status

    fun insertSubsumsjon(subsumsjon: Subsumsjon): Int

    fun getSubsumsjon(id: String): Subsumsjon
}

internal class SubsumsjonNotFoundException(override val message: String) : RuntimeException(message)

internal class BehovNotFoundException(override val message: String) : RuntimeException(message)

internal class StoreException(override val message: String) : RuntimeException(message)

