package no.nav.dagpenger.regel.api.minsteinntekt.model
data class MinsteinntektResultat(
    val oppfyllerKravTilMinsteArbeidsinntekt: Boolean,
    val periodeAntallUker: Int? = null
) {
    init {
        val gyldigeUker = setOf(26, 52, 104)
        if (!gyldigeUker.contains(periodeAntallUker)) {
            throw IllegalArgumentException("Ugyldig antall uker for minsteinntekt, gyldige verdier er ${gyldigeUker.joinToString { "$it" }}")
        }
    }
}
