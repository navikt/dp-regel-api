package no.nav.dagpenger.regel.api.periode

class PeriodeSubsumsjonerDummy : PeriodeSubsumsjoner {
    var storedPeriodeSubsumsjon: PeriodeSubsumsjon? = null

    override fun getPeriodeSubsumsjon(subsumsjonsId: String): PeriodeSubsumsjon {
        return storedPeriodeSubsumsjon!!
    }

    override fun insertPeriodeSubsumsjon(periodeSubsumsjon: PeriodeSubsumsjon) {
        storedPeriodeSubsumsjon = periodeSubsumsjon
    }
}