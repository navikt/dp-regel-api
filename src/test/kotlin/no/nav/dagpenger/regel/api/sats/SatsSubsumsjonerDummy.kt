package no.nav.dagpenger.regel.api.sats

class SatsSubsumsjonerDummy : SatsSubsumsjoner {
    var storedSatsSubsumsjon: SatsSubsumsjon? = null

    override fun getSatsSubsumsjon(subsumsjonsId: String): SatsSubsumsjon {
        return storedSatsSubsumsjon!!
    }

    override fun insertSatsSubsumsjon(satsSubsumsjon: SatsSubsumsjon) {
        storedSatsSubsumsjon = satsSubsumsjon
    }
}