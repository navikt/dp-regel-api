package no.nav.dagpenger.regel.api.models

import no.nav.dagpenger.regel.api.Regel
import no.nav.dagpenger.regel.api.moshiInstance
import java.time.LocalDate
import java.time.LocalDateTime

fun fromJson(json: String, regel: Regel): Subsumsjon? {
    return when (regel) {
        Regel.MINSTEINNTEKT -> moshiInstance.adapter<MinsteinntektSubsumsjon>(MinsteinntektSubsumsjon::class.java).fromJson(json)
        Regel.PERIODE -> moshiInstance.adapter<PeriodeSubsumsjon>(PeriodeSubsumsjon::class.java).fromJson(json)
        Regel.GRUNNLAG -> moshiInstance.adapter<GrunnlagSubsumsjon>(GrunnlagSubsumsjon::class.java).fromJson(json)
        Regel.SATS -> moshiInstance.adapter<SatsSubsumsjon>(SatsSubsumsjon::class.java).fromJson(json)
    }
}

fun toJson(subsumsjon: Subsumsjon): String? =
    when (subsumsjon) {
        is GrunnlagSubsumsjon -> moshiInstance.adapter<GrunnlagSubsumsjon>(GrunnlagSubsumsjon::class.java).toJson(subsumsjon)
        is MinsteinntektSubsumsjon -> moshiInstance.adapter<MinsteinntektSubsumsjon>(MinsteinntektSubsumsjon::class.java).toJson(subsumsjon)
        is PeriodeSubsumsjon -> moshiInstance.adapter<PeriodeSubsumsjon>(PeriodeSubsumsjon::class.java).toJson(subsumsjon)
        is SatsSubsumsjon -> moshiInstance.adapter<SatsSubsumsjon>(SatsSubsumsjon::class.java).toJson(subsumsjon)
    }

class SubsumsjonSerDerException(message: String) : RuntimeException(message)

sealed class Subsumsjon {
    abstract val subsumsjonsId: String
    abstract val behovId: String
    abstract val regel: Regel
}

data class GrunnlagSubsumsjon(
    override val subsumsjonsId: String,
    override val behovId: String,
    override val regel: Regel,
    val opprettet: LocalDateTime, // todo: ZonedDateTime?
    val utfort: LocalDateTime, // todo: ZonedDateTime?,
    val faktum: GrunnlagFaktum,
    val resultat: GrunnlagResultat,
    val inntekt: Set<InntektResponseGrunnlag>? = null
) : Subsumsjon()

data class GrunnlagResultat(
    val avkortet: Int,
    val uavkortet: Int,
    val beregningsregel: String,
    val harAvkortet: Boolean
)

data class GrunnlagFaktum(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: LocalDate,
    val inntektsId: String? = null,
    val harAvtjentVerneplikt: Boolean? = false,
    val oppfyllerKravTilFangstOgFisk: Boolean? = false,
    val antallBarn: Int? = 0,
    val manueltGrunnlag: Int? = null
)

data class MinsteinntektSubsumsjon(
    override val subsumsjonsId: String,
    override val behovId: String,
    override val regel: Regel,
    val opprettet: LocalDateTime, // todo: ZonedDateTime?
    val utfort: LocalDateTime, // todo: ZonedDateTime?,
    val faktum: MinsteinntektFaktum,
    val resultat: MinsteinntektResultat,
    val inntekt: Set<InntektResponse>
) : Subsumsjon()

data class MinsteinntektResultat(
    val oppfyllerKravTilMinsteArbeidsinntekt: Boolean
)

data class MinsteinntektFaktum(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: LocalDate,
    val inntektsId: String,
    val harAvtjentVerneplikt: Boolean? = false,
    val oppfyllerKravTilFangstOgFisk: Boolean? = false,
    val bruktInntektsPeriode: InntektsPeriode? = null
)

data class PeriodeSubsumsjon(
    override val subsumsjonsId: String,
    override val behovId: String,
    override val regel: Regel,
    val opprettet: LocalDateTime, // todo: ZonedDateTime?
    val utfort: LocalDateTime, // todo: ZonedDateTime?,
    val faktum: PeriodeFaktum,
    val resultat: PeriodeResultat
) : Subsumsjon()

data class PeriodeResultat(
    val antallUker: Int
)

data class PeriodeFaktum(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: LocalDate,
    val inntektsId: String,
    val harAvtjentVerneplikt: Boolean? = false,
    val oppfyllerKravTilFangstOgFisk: Boolean? = false,
    val bruktInntektsPeriode: InntektsPeriode? = null
)

data class SatsSubsumsjon(
    override val subsumsjonsId: String,
    override val behovId: String,
    override val regel: Regel,
    val opprettet: LocalDateTime, // todo: ZonedDateTime?
    val utfort: LocalDateTime, // todo: ZonedDateTime?,
    val faktum: SatsFaktum,
    val resultat: SatsResultat
) : Subsumsjon()

data class SatsResultat(
    val dagsats: Int,
    val ukesats: Int,
    val benyttet90ProsentRegel: Boolean
)

data class SatsFaktum(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: LocalDate,
    val antallBarn: Int? = 0,
    val manueltGrunnlag: Int? = null
)
