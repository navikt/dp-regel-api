package no.nav.dagpenger.regel.api.arena.adapter.v1.models.common

data class InntektResponse(
    val inntekt: Int,
    val periode: Int, // todo: enum?
    val inntektsPeriode: InntektsPeriode,
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
