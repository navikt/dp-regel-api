package no.nav.dagpenger.regel.api.grunnlag

class DagpengegrunnlagBeregningerDummy : DagpengegrunnlagBeregninger {
    var storedDagpengegrunnlagBeregning: DagpengegrunnlagResponse? = null
    override fun getGrunnlagBeregning(subsumsjonsId: String): DagpengegrunnlagResponse {
        return storedDagpengegrunnlagBeregning!!
    }

    override fun setGrunnlagBeregning(dagpengegrunnlagBeregning: DagpengegrunnlagResponse) {
        storedDagpengegrunnlagBeregning = dagpengegrunnlagBeregning
    }
}