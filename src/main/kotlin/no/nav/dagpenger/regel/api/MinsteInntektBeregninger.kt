package no.nav.dagpenger.regel.api

import java.lang.Exception

data class MinsteinntektBeregningResultat(
    val oppfyllerMinsteinntekt: Boolean,
    val periode: Int
) {
    companion object {
        val exampleInntektBeregning = mapOf(
            "oppfyllerMinsteinntekt" to true,
            "status" to 1
        )
    }
}

class MinsteInntektBeregninger {
    val beregninger = mutableMapOf<String, MinsteinntektBeregningResultat>(
        "123" to MinsteinntektBeregningResultat(true, 52)
    )

    fun hasDataForBeregning(beregningsId: String) = beregninger.containsKey(beregningsId)

    fun getBeregning(beregningsId: String) = beregninger[beregningsId] ?: throw Exception("no beregning for id found")
}
