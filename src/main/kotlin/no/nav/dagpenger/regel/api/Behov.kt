package no.nav.dagpenger.regel.api

import de.huxhorn.sulky.ulid.ULID
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektRequestParametere
import no.nav.dagpenger.regel.api.models.InntektResponse
import no.nav.dagpenger.regel.api.periode.PeriodeRequestParametere
import java.time.LocalDate
import java.time.YearMonth

internal val ulidGenerator = ULID()

data class SubsumsjonsBehov(
    val behovId: String,
    val aktørId: String,
    val vedtakId: Int,
    val beregningsDato: LocalDate,
    val harAvtjentVerneplikt: Boolean? = null,
    val antallBarn: Int? = null,
    val inntektV1: String? = null,
    val grunnlag: Int? = null,
    val senesteInntektsmåned: YearMonth? = null,
    val bruktInntektsPeriode: BruktInntektsPeriode? = null,

    val tasks: List<String>? = null,

    val minsteinntektInntektsPerioder: List<InntektResponse>? = null,
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
    val uavkortet: Int
)

data class SatsResultat(
    @Deprecated("Hvorfor er denne her? ")
    val sporingsId: String,
    val subsumsjonsId: String,
    val regelIdentifikator: String,
    val dagsats: Int,
    val ukesats: Int
)

fun mapRequestToBehov(
    request: MinsteinntektRequestParametere
): SubsumsjonsBehov {

    val senesteInntektsmåned = YearMonth.of(request.beregningsdato.year, request.beregningsdato.month)
    val bruktInntektsPeriode =
        if (request.bruktInntektsPeriode != null)
            BruktInntektsPeriode(request.bruktInntektsPeriode.foersteMaaned, request.bruktInntektsPeriode.sisteMaaned)
        else null

    return SubsumsjonsBehov(
        ulidGenerator.nextULID(),
        request.aktorId,
        request.vedtakId,
        request.beregningsdato,
        request.harAvtjentVerneplikt,
        senesteInntektsmåned = senesteInntektsmåned,
        bruktInntektsPeriode = bruktInntektsPeriode
    )
}

fun mapRequestToBehov(
    request: PeriodeRequestParametere
): SubsumsjonsBehov {

    val senesteInntektsmåned = YearMonth.of(request.beregningsdato.year, request.beregningsdato.month)

    val bruktInntektsPeriode =
        if (request.bruktInntektsPeriode != null)
            BruktInntektsPeriode(request.bruktInntektsPeriode.foersteMaaned, request.bruktInntektsPeriode.sisteMaaned)
        else null

    return SubsumsjonsBehov(
        ulidGenerator.nextULID(),
        request.aktorId,
        request.vedtakId,
        request.beregningsdato,
        request.harAvtjentVerneplikt,
        senesteInntektsmåned = senesteInntektsmåned,
        bruktInntektsPeriode = bruktInntektsPeriode
    )
}
