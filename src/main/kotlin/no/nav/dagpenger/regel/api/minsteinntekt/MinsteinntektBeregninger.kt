package no.nav.dagpenger.regel.api.minsteinntekt

interface MinsteinntektBeregninger {

    fun getMinsteinntektBeregning(subsumsjonsId: String): MinsteinntektBeregning

    fun setMinsteinntektBeregning(minsteinntektBeregning: MinsteinntektBeregning)
}

class MinsteinntektBeregningNotFoundException(override val message: String) : RuntimeException(message)
