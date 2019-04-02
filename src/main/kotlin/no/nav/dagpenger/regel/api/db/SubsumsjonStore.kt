package no.nav.dagpenger.regel.api.db

import no.nav.dagpenger.regel.api.Regel
import no.nav.dagpenger.regel.api.Status
import no.nav.dagpenger.regel.api.SubsumsjonsBehov
import no.nav.dagpenger.regel.api.models.Subsumsjon

interface SubsumsjonStore {
    fun insertBehov(subsumsjonsBehov: SubsumsjonsBehov, regel: Regel)

    fun behovStatus(behovId: String, regel: Regel): Status

    fun insertSubsumsjon(subsumsjon: Subsumsjon)

    fun getSubsumsjon(subsumsjonId: String, regel: Regel): Subsumsjon
}

class SubsumsjonNotFoundException(override val message: String) : RuntimeException(message)

class BehovNotFoundException(override val message: String) : RuntimeException(message)

class StoreException(override val message: String) : RuntimeException(message)