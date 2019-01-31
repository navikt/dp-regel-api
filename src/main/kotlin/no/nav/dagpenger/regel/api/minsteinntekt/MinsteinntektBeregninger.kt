package no.nav.dagpenger.regel.api.minsteinntekt

import no.nav.dagpenger.regel.api.grunnlag.Utfall
import no.nav.dagpenger.regel.api.minsteinntekt.model.InntektsPeriode
import no.nav.dagpenger.regel.api.minsteinntekt.model.MinsteinntektBeregning
import no.nav.dagpenger.regel.api.minsteinntekt.model.MinsteinntektResultat
import no.nav.dagpenger.regel.api.minsteinntekt.model.MinsteinntektResultatParametere
import java.time.LocalDate
import java.time.LocalDateTime

class MinsteinntektBeregninger {

    val dummyresult = MinsteinntektBeregning(
        "456",
        LocalDateTime.now(),
        LocalDateTime.now(),
        MinsteinntektResultatParametere(
            "123",
            789,
            LocalDate.now()
        ),
        MinsteinntektResultat("true")

    )



    val beregningsId_beregninger = mutableMapOf("123" to dummyresult)
    val aktorId_beregninger = mutableMapOf("01019955667" to dummyresult)

    fun getBeregningForBeregningsId(beregningsId: String) = dummyresult
//        beregningsId_beregninger[beregningsId] ?: throw MinsteinntektBeregningNotFoundException("no beregning for beregningsid ${beregningsId} found")

    fun getBeregningForAktorId(aktorId: String) = dummyresult
//        aktorId_beregninger[aktorId] ?: throw MinsteinntektBeregningNotFoundException("no beregning for aktorId ${aktorId} found")
}

class MinsteinntektBeregningNotFoundException(override val message: String) : RuntimeException(message)
