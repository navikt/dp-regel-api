package no.nav.dagpenger.regel.api.v1.models

import com.squareup.moshi.JsonAdapter
import no.nav.dagpenger.regel.api.moshiInstance
import java.time.LocalDate

internal val behovJsonAdapter: JsonAdapter<Behov> = moshiInstance.adapter<Behov>(Behov::class.java)

internal data class Behov(
    val id: String,
    val aktørId: String,
    val vedtakId: Int,
    val beregningsDato: LocalDate,
    val harAvtjentVerneplikt: Boolean? = null,
    val antallBarn: Int? = null,
    val manueltGrunnlag: Int? = null
)

internal sealed class Status {
    data class Done(val subsumsjonsId: String) : Status() {
        companion object {
            override fun toString() = "Done"
        }
    }

    object Pending : Status() {
        override fun toString() = "Pending"
    }
}



