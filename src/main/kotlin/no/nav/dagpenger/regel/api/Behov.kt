package no.nav.dagpenger.regel.api

import java.time.LocalDate
import java.time.YearMonth

data class SubsumsjonsBehov(
    val behovId: String,
    val aktørId: String,
    val vedtakId: Int,
    val beregningsDato: LocalDate,
    val harAvtjentVerneplikt: Boolean? = null,
    val antallBarn: Int? = null,
    val inntektV1: String? = null,
    val manueltGrunnlag: Int? = null,
    val senesteInntektsmåned: YearMonth? = null,
    val bruktInntektsPeriode: BruktInntektsPeriode? = null,

    val minsteinntektInntektsPerioder: String? = null,
    val grunnlagInntektsPerioder: String? = null,
    var minsteinntektResultat: MinsteinntektResultat? = null,
    var periodeResultat: PeriodeResultat? = null,
    var grunnlagResultat: GrunnlagResultat? = null,
    var satsResultat: SatsResultat? = null
)

data class BruktInntektsPeriode(
    val førsteMåned: YearMonth,
    val sisteMåned: YearMonth
)

data class MinsteinntektResultat(
    @Deprecated("Hvorfor er denne her? ")
    val sporingsId: String,
    val subsumsjonsId: String,
    val regelIdentifikator: String,
    val oppfyllerMinsteinntekt: Boolean
)

data class PeriodeResultat(
    @Deprecated("Hvorfor er denne her? ")
    val sporingsId: String,
    val subsumsjonsId: String,
    val regelIdentifikator: String,
    val periodeAntallUker: Int
)

data class GrunnlagResultat(
    @Deprecated("Hvorfor er denne her? ")
    val sporingsId: String,
    val subsumsjonsId: String,
    val regelIdentifikator: String,
    val avkortet: Int,
    val uavkortet: Int,
    val beregningsregel: String
)

data class SatsResultat(
    @Deprecated("Hvorfor er denne her? ")
    val sporingsId: String,
    val subsumsjonsId: String,
    val regelIdentifikator: String,
    val dagsats: Int,
    val ukesats: Int,
    val benyttet90ProsentRegel: Boolean
)
