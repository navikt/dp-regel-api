package no.nav.dagpenger.regel.api.minsteinntekt

class MinsteinntektBeregningerDummy : MinsteinntektSubsumsjoner {
    var storedMinsteinntektBeregning: MinsteinntektSubsumsjon? = null

    override fun getMinsteinntektSubsumsjon(subsumsjonsId: String): MinsteinntektSubsumsjon {
        return storedMinsteinntektBeregning!!
    }

    override fun insertMinsteinntektSubsumsjon(minsteinntektSubsumsjon: MinsteinntektSubsumsjon) {
        storedMinsteinntektBeregning = minsteinntektSubsumsjon
    }
}