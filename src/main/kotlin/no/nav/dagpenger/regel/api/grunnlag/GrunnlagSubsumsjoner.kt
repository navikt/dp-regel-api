package no.nav.dagpenger.regel.api.grunnlag

interface GrunnlagSubsumsjoner {
    fun getGrunnlagSubsumsjon(subsumsjonsId: String): GrunnlagSubsumsjon

    fun setGrunnlagSubsumsjon(grunnlagSubsumsjon: GrunnlagSubsumsjon)
}
