package no.nav.dagpenger.regel.api.minsteinntekt

class MinsteinntektSubsumsjonerDummy : MinsteinntektSubsumsjoner {
    var storedMinsteinntektSubsumsjon: MinsteinntektSubsumsjon? = null

    override fun getMinsteinntektSubsumsjon(subsumsjonsId: String): MinsteinntektSubsumsjon {
        return storedMinsteinntektSubsumsjon!!
    }

    override fun insertMinsteinntektSubsumsjon(minsteinntektSubsumsjon: MinsteinntektSubsumsjon) {
        storedMinsteinntektSubsumsjon = minsteinntektSubsumsjon
    }
}