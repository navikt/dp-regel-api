package no.nav.dagpenger.regel.api.minsteinntekt

class MinsteinntektBeregningerDummy : MinsteinntektBeregninger {
    var storedMinsteinntektBeregning: MinsteinntektBeregning? = null

    override fun getMinsteinntektBeregning(subsumsjonsId: String): MinsteinntektBeregning {
        return storedMinsteinntektBeregning!!
    }

    override fun setMinsteinntektBeregning(minsteinntektBeregning: MinsteinntektBeregning) {
        storedMinsteinntektBeregning = minsteinntektBeregning
    }
}