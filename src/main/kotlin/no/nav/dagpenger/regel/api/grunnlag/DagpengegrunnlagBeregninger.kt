package no.nav.dagpenger.regel.api.grunnlag

interface DagpengegrunnlagBeregninger {
    fun getGrunnlagBeregning(subsumsjonsId: String): DagpengegrunnlagResponse

    fun setGrunnlagBeregning(dagpengegrunnlagBeregning: DagpengegrunnlagResponse)
}

class GrunnlagBeregningNotFoundException(override val message: String) : RuntimeException(message)