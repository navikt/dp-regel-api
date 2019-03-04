package no.nav.dagpenger.regel.api

import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class SubsumsjonsBehov (
    val behovId: String,
    val aktørId: String,
    val vedtakId: Int,
    val beregningsDato: LocalDate,
    val harAvtjentVerneplikt: Boolean? = null,
    val antallBarn: Int? = null,
    val inntektV1: Inntekt? = null,
    val grunnlag: Int? = null,
    val senesteInntektsmåned: YearMonth? = null,
    val bruktInntektsPeriode: BruktInntektsPeriode? = null,

    val tasks: List<String>? = null,

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

data class Inntekt(
    val inntektsId: String,
    val inntektsListe: List<KlassifisertInntektMåned>
)

data class KlassifisertInntektMåned(
    val årMåned: YearMonth,
    val klassifiserteInntekter: List<KlassifisertInntekt>
)

data class KlassifisertInntekt(
    val beløp: BigDecimal,
    val inntektKlasse: InntektKlasse
)

enum class InntektKlasse {
    ARBEIDSINNTEKT,
    DAGPENGER,
    DAGPENGER_FANGST_FISKE,
    SYKEPENGER_FANGST_FISKE,
    FANGST_FISKE,
    SYKEPENGER,
    TILTAKSLØNN
}