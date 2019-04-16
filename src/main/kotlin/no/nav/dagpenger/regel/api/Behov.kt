package no.nav.dagpenger.regel.api

import de.huxhorn.sulky.ulid.ULID
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.regel.api.models.InntektResponse
import no.nav.dagpenger.regel.api.models.InntektResponseGrunnlag
import java.time.LocalDate
import java.time.YearMonth

internal val ulidGenerator = ULID()

data class SubsumsjonsBehov(
    val behovId: String,
    val aktørId: String,
    val vedtakId: Int,
    val beregningsDato: LocalDate,
    val harAvtjentVerneplikt: Boolean? = null,
    val oppfyllerKravTilFangstOgFisk: Boolean? = false,
    val antallBarn: Int? = null,
    val inntektV1: Inntekt? = null,
    val manueltGrunnlag: Int? = null,
    val senesteInntektsmåned: YearMonth? = null,
    val bruktInntektsPeriode: BruktInntektsPeriode? = null,

    val minsteinntektInntektsPerioder: Set<InntektResponse>? = null,
    val grunnlagInntektsPerioder: Set<InntektResponseGrunnlag>? = null,
    var minsteinntektResultat: MinsteinntektResultat? = null,
    var periodeResultat: PeriodeResultat? = null,
    var grunnlagResultat: GrunnlagResultat? = null,
    var satsResultat: SatsResultat? = null
)

sealed class Status {
    data class Done(val subsumsjonsId: String) : Status() {
        companion object {
            override fun toString() = "Done"
        }
    }

    object Pending : Status() {
        override fun toString() = "Pending"
    }
}

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
    val beregningsregel: String,
    val harAvkortet: Boolean
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
