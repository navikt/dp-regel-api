package no.nav.dagpenger.regel.api.models

import java.math.BigDecimal

data class InntektResponse(
    val inntekt: BigDecimal,
    val periode: Int, // todo: enum?
    val inntektsPeriode: InntektsPeriode,
    val inneholderFangstOgFisk: Boolean,
    val andel: BigDecimal
) {
    init {
        val gyldigePerioder = setOf(1, 2, 3)
        if (!gyldigePerioder.contains(periode)) {
            throw IllegalArgumentException("Ugyldig periode for inntektgrunnlag, gyldige verdier er ${gyldigePerioder.joinToString { "$it" }}")
        }
    }
}
