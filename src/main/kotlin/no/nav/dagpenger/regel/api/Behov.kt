package no.nav.dagpenger.regel.api

import java.time.LocalDate

data class SubsumsjonsBehov (
    val behovId: String,
    val aktørId: String,
    val vedtakId: Int,
    val beregningsDato: LocalDate,
    val harAvtjentVerneplikt: Boolean? = null,
    val antallBarn: Int? = null,
    val inntekt: Inntekt? = null,
    val grunnlag: Int? = null,

    val tasks: List<String>? = null,

    var minsteinntektResultat: MinsteinntektResultat? = null,
    var periodeResultat: PeriodeResultat? = null,
    var grunnlagResultat: GrunnlagResultat? = null,
    var satsResultat: SatsResultat? = null
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

data class GrunnlagResultat(
    val sporingsId: String,
    val subsumsjonsId: String,
    val regelIdentifikator: String,
    val avkortet: Int,
    val uavkortet: Int
)

data class SatsResultat(
    val sporingsId: String,
    val subsumsjonsId: String,
    val regelIdentifikator: String,
    val dagsats: Int,
    val ukesats: Int
)

data class Inntekt(val inntektsId: String, val inntekt: Int)