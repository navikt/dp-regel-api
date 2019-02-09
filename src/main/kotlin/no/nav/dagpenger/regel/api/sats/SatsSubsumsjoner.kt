package no.nav.dagpenger.regel.api.sats

interface SatsSubsumsjoner {

    fun getSatsSubsumsjon(subsumsjonsId: String): SatsSubsumsjon

    fun insertSatsSubsumsjon(satsSubsumsjon: SatsSubsumsjon)
}