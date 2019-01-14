package no.nav.dagpenger.regel.api.minsteinntekt

import no.nav.dagpenger.regel.api.grunnlag.Parametere
import no.nav.dagpenger.regel.api.grunnlag.Utfall

class MinsteinntektBeregninger {

    val dummyresult = MinsteinntektBeregningsResult(
        "456",
        Utfall(true, 104),
        "2018-12-26T14:42:09Z",
        "2018-12-26T14:42:09Z",
        Parametere(
            "01019955667",
            123,
            "2019-01-11",
            "lasdfQ",
            InntektsPeriode("2019-01", "2018-01")
        ),
        false,
        false,
        false
    )

    val beregningsId_beregninger = mutableMapOf("123" to dummyresult)
    val aktorId_beregninger = mutableMapOf("01019955667" to dummyresult)

    fun getBeregningForBeregningsId(beregningsId: String) = dummyresult
//        beregningsId_beregninger[beregningsId] ?: throw MinsteinntektBeregningNotFoundException("no beregning for beregningsid ${beregningsId} found")

    fun getBeregningForAktorId(aktorId: String) = dummyresult
//        aktorId_beregninger[aktorId] ?: throw MinsteinntektBeregningNotFoundException("no beregning for aktorId ${aktorId} found")
}

class MinsteinntektBeregningNotFoundException(override val message: String) : RuntimeException(message)
