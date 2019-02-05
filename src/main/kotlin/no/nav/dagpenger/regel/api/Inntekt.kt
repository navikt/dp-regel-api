package no.nav.dagpenger.regel.api

import java.time.YearMonth

data class Inntekt(
    val inntekt: Int,
    val periode: Int, // todo: enum?
    val inneholderNaeringsinntekter: Boolean,
    val andel: Int
) {
    init {
        val gyldigePerioder = setOf(1, 2, 3)
        if (!gyldigePerioder.contains(periode)) {
            throw IllegalArgumentException("Ugyldig periode for inntektgrunnlat, gyldige verdier er ${gyldigePerioder.joinToString { "$it" }}")
        }
    }
}

data class InntektsPeriode(
    val foersteMaaned: YearMonth? = null,
    val sisteMaaned: YearMonth
)
