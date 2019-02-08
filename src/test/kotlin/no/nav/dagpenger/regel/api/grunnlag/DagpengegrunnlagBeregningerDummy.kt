package no.nav.dagpenger.regel.api.grunnlag

class DagpengegrunnlagBeregningerDummy : GrunnlagSubsumsjoner {
    var storedDagpengegrunnlagBeregning: GrunnlagSubsumsjon? = null
    override fun getGrunnlagSubsumsjon(subsumsjonsId: String): GrunnlagSubsumsjon {
        return storedDagpengegrunnlagBeregning!!
    }

    override fun setGrunnlagSubsumsjon(dagpengegrunnlagSubsumsjon: GrunnlagSubsumsjon) {
        storedDagpengegrunnlagBeregning = dagpengegrunnlagSubsumsjon
    }
}