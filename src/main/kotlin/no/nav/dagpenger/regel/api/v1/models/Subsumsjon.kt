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
    companion object Mapper {
        private val adapter = moshiInstance.adapter<Subsumsjon>(Subsumsjon::class.java)
        fun toJson(subsumsjon: Subsumsjon): String = adapter.toJson(subsumsjon)
        fun fromJson(json: String): Subsumsjon? = adapter.fromJson(json)
    }

    fun toJson(): String = toJson(this)
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

internal class SubsumsjonSerDerException(message: String) : RuntimeException(message)
