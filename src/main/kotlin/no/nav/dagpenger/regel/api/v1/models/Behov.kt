package no.nav.dagpenger.regel.api.v1.models

import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.regel.api.moshiInstance
import java.time.LocalDate

data class Behov(
    val behovId: String,
    val aktørId: String,
    val vedtakId: Int,
    val beregningsDato: LocalDate,
    val harAvtjentVerneplikt: Boolean? = null,
    val antallBarn: Int? = null,
    val manueltGrunnlag: Int? = null
) {
    companion object Mapper {
        private val adapter = moshiInstance.adapter<Behov>(Behov::class.java)

        fun toJson(behov: Behov): String = adapter.toJson(behov)

        fun fromJson(json: String): Behov? = adapter.fromJson(json)

        fun toPacket(behov: Behov): Packet = Packet("{}").apply {
            this.putValue(PacketKeys.BEHOV_ID, behov.behovId)
            this.putValue(PacketKeys.AKTØR_ID, behov.aktørId)
            this.putValue(PacketKeys.VEDTAK_ID, behov.vedtakId)
            this.putValue(PacketKeys.BEREGNINGS_DATO, behov.beregningsDato)
            behov.harAvtjentVerneplikt?.let { this.putValue(PacketKeys.HAR_AVTJENT_VERNE_PLIKT, it) }
            behov.antallBarn?.let { this.putValue(PacketKeys.ANTALL_BARN, it) }
            behov.manueltGrunnlag?.let { this.putValue(PacketKeys.MANUELT_GRUNNLAG, it) }
        }
    }
}

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



