package no.nav.dagpenger.regel.api

import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektBeregning
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektBeregninger

class MinsteinntektBeregningerDummy : MinsteinntektBeregninger {
    var storedMinsteinntektBeregning: MinsteinntektBeregning? = null

    override fun getMinsteinntektBeregning(subsumsjonsId: String): MinsteinntektBeregning {
        return storedMinsteinntektBeregning!!
    }

    override fun setMinsteinntektBeregning(minsteinntektBeregning: MinsteinntektBeregning) {
        storedMinsteinntektBeregning = minsteinntektBeregning
    }
}