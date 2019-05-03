package no.nav.dagpenger.regel.api.v1.models

import no.nav.dagpenger.regel.api.moshiInstance
import java.time.LocalDate


internal data class Subsumsjon(
    val id: String,
    val behovId: String,
    val faktum: Faktum,
    val grunnlagResultat: Map<String, Any>?,
    val minsteinntektResultat: Map<String, Any>?,
    val periodeResultat: Map<String, Any>?,
    val satsResultat: Map<String, Any>?

) {
    companion object Adapter {
        private val adapter = moshiInstance.adapter<Subsumsjon>(Subsumsjon::class.java)
        fun toJson(subsumsjon: Subsumsjon): String = adapter.toJson(subsumsjon)
        fun fromJson(json: String): Subsumsjon? = adapter.fromJson(json)
    }
}

internal data class Faktum(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: LocalDate,
    val inntektsId: String? = null,
    val harAvtjentVerneplikt: Boolean? = false,
    val oppfyllerKravTilFangstOgFisk: Boolean? = false,
    val antallBarn: Int? = 0,
    val manueltGrunnlag: Int? = null,
    val bruktInntektsPeriode: InntektsPeriode? = null
)

internal data class GrunnlagResultat(
    val avkortet: Int,
    val uavkortet: Int,
    val beregningsregel: String,
    val harAvkortet: Boolean,
    val inntektsPerioder: Set<InntektResponse>

)

internal data class MinsteinntektResultat(
    val oppfyllerKravTilMinsteArbeidsinntekt: Boolean,
    val inntektsPerioder: Set<InntektResponse>
)

internal data class PeriodeResultat(
    val antallUker: Int
)

internal data class SatsResultat(
    val dagsats: Int,
    val ukesats: Int,
    val benyttet90ProsentRegel: Boolean
)


internal class SubsumsjonSerDerException(message: String) : RuntimeException(message)
