package no.nav.dagpenger.regel.api.minsteinntekt

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

class MinsteinntektBeregninger {
    val beregninger = mutableMapOf<String, MinsteinntektBeregningResultat>(
        "123" to MinsteinntektBeregningResultat(true, 52)
    )

    fun hasDataForBeregning(beregningsId: String) = beregninger.containsKey(beregningsId)

    fun getBeregning(beregningsId: String) = beregninger[beregningsId] ?: throw MinsteinntektBeregningNotFoundException(
        "no beregning for id found"
    )
}

class MinsteinntektBeregningNotFoundException(override val message: String) : RuntimeException(message)
