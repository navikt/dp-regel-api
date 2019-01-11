package no.nav.dagpenger.regel.api.grunnlag

data class GrunnlagBeregningResultat(
    val grunnlag: Int
) {
    companion object {
        val exampleGrunnlag = mapOf(
            "oppfyllerMinsteinntekt" to true
        )
    }
}

class GrunnlagBeregninger {
    val beregninger = mutableMapOf(
        "456" to GrunnlagBeregningResultat(250000)
    )

    fun hasDataForBeregning(beregningsId: String) = beregninger.containsKey(beregningsId)

    fun getBeregning(beregningsId: String) = beregninger[beregningsId] ?: throw GrunnlagBeregningNotFoundException(
        "no beregning for id found"
    )
}

class GrunnlagBeregningNotFoundException(override val message: String) : RuntimeException(message)