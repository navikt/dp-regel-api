package no.nav.dagpenger.regel.api.minsteinntekt

interface MinsteinntektSubsumsjoner {

    fun getMinsteinntektSubsumsjon(subsumsjonsId: String): MinsteinntektSubsumsjon

    fun insertMinsteinntektSubsumsjon(minsteinntektSubsumsjon: MinsteinntektSubsumsjon)
}

class MinsteinntektSubsumsjonNotFoundException(override val message: String) : RuntimeException(message)
