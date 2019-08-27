package no.nav.dagpenger.regel.api.models

import de.huxhorn.sulky.ulid.ULID
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.regel.api.moshiInstance
import java.time.LocalDate

internal data class Behov(
    val behovId: String = ulidGenerator.nextULID(),
    val aktørId: String,
    val vedtakId: Int,
    val beregningsDato: LocalDate,
    val harAvtjentVerneplikt: Boolean? = null,
    val oppfyllerKravTilFangstOgFisk: Boolean? = null,
    val bruktInntektsPeriode: InntektsPeriode? = null,
    val antallBarn: Int? = null,
    val manueltGrunnlag: Int? = null,
    val inntektsId: String? = null
) {
    companion object Mapper {
        private val ulidGenerator = ULID()

        private val adapter = moshiInstance.adapter<Behov>(Behov::class.java)

        fun toJson(behov: Behov): String = adapter.toJson(behov)

        fun fromJson(json: String): Behov? = adapter.fromJson(json)

        fun toPacket(behov: Behov): Packet = Packet("{}").apply {
            this.putValue(PacketKeys.BEHOV_ID, behov.behovId)
            this.putValue(PacketKeys.AKTØR_ID, behov.aktørId)
            this.putValue(PacketKeys.VEDTAK_ID, behov.vedtakId)
            this.putValue(PacketKeys.BEREGNINGS_DATO, behov.beregningsDato)
            behov.harAvtjentVerneplikt?.let { this.putValue(PacketKeys.HAR_AVTJENT_VERNE_PLIKT, it) }
            behov.oppfyllerKravTilFangstOgFisk?.let { this.putValue(PacketKeys.OPPFYLLER_KRAV_TIL_FANGST_OG_FISK, it) }
            behov.bruktInntektsPeriode?.let { this.putValue(PacketKeys.BRUKT_INNTEKTSPERIODE, it.toJson()) }
            behov.antallBarn?.let { this.putValue(PacketKeys.ANTALL_BARN, it) }
            behov.manueltGrunnlag?.let { this.putValue(PacketKeys.MANUELT_GRUNNLAG, it) }
            behov.inntektsId?.let { this.putValue(PacketKeys.INNTEKTS_ID, it) }
        }
    }

    fun toPacket(): Packet = Mapper.toPacket(this)
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
