package no.nav.dagpenger.regel.api.periode

interface PeriodeSubsumsjoner {

    fun getPeriodeSubsumsjon(subsumsjonsId: String): PeriodeSubsumsjon

    fun insertPeriodeSubsumsjon(periodeSubsumsjon: PeriodeSubsumsjon)
}