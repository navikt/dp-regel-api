package no.nav.dagpenger.regel.api

import java.lang.Exception

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
    val beregninger = mutableMapOf<String, GrunnlagBeregningResultat>(
        "456" to GrunnlagBeregningResultat(250000)
    )

    fun hasDataForBeregning(beregningsId: String) = beregninger.containsKey(beregningsId)

    fun getBeregning(beregningsId: String) = beregninger[beregningsId] ?: throw Exception("no beregning for id found")
}
