package no.nav.dagpenger.regel.api.grunnlag

import no.nav.dagpenger.regel.api.minsteinntekt.InntektsPeriode

class GrunnlagBeregninger {
    val dummyresult = GrunnlagBeregningsResultat(
        "456",
        Utfall(true, 104),
        "2018-12-26T14:42:09Z",
        "2018-12-26T14:42:09Z",
        GrunnlagBeregningsRequest(
            "01019955667",
            123,
            "2019-01-11",
            "lasdFQ=q",
            InntektsPeriode("2019-01", "2018-01"),
            false,
            false,
            false
        )
    )

    val beregninger_aktorId = mutableMapOf(
        "01019955667" to dummyresult
    )
    val beregninger = mutableMapOf(
        "456" to dummyresult
    )

    fun getBeregningForAktorId(aktorId: String) = dummyresult
//        beregninger_aktorId[aktorId] ?: throw GrunnlagBeregningNotFoundException("no beregning for id found")

    fun getBeregning(beregningsId: String) = dummyresult
//        beregninger[beregningsId] ?: throw GrunnlagBeregningNotFoundException("no beregning for id found")
}

class GrunnlagBeregningNotFoundException(override val message: String) : RuntimeException(message)
