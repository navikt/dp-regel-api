package no.nav.dagpenger.regel.api.grunnlag

class GrunnlagSubsumsjonerDummy : GrunnlagSubsumsjoner {
    var storedGrunnlagSubsumsjon: GrunnlagSubsumsjon? = null
    override fun getGrunnlagSubsumsjon(subsumsjonsId: String): GrunnlagSubsumsjon {
        return storedGrunnlagSubsumsjon!!
    }

    override fun setGrunnlagSubsumsjon(grunnlagSubsumsjon: GrunnlagSubsumsjon) {
        storedGrunnlagSubsumsjon = grunnlagSubsumsjon
    }
}