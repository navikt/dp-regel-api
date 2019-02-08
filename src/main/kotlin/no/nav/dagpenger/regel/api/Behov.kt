package no.nav.dagpenger.regel.api

import java.time.LocalDate

data class SubsumsjonsBehov (
    val behovId: String,
    val aktørId: String,
    val vedtakId: Int,
    val beregningsDato: LocalDate,
    val avtjentVerneplikt: Boolean? = false,
    val eøs: Boolean? = false,
    val antallBarn: Int? = 0,
    val inntekt: Inntekt? = null,

    var tasks: List<String>? = null,

    var minsteinntektResultat: MinsteinntektResultat? = null,
    var periodeResultat: PeriodeResultat? = null
)

data class MinsteinntektResultat(
    val sporingsId: String,
    val subsumsjonsId: String,
    val regelIdentifikator: String,
    val oppfyllerMinsteinntekt: Boolean
)

data class PeriodeResultat(
    val sporingsId: String,
    val subsumsjonsId: String,
    val regelIdentifikator: String,
    val periodeAntallUker: Int
)

data class Inntekt(val inntektsId: String, val inntekt: Int)