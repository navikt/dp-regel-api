package no.nav.dagpenger.regel.api.models

import de.huxhorn.sulky.ulid.ULID
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.regel.api.moshiInstance
import java.time.LocalDate

private val ulidGenerator = ULID()

data class Behov(
    val aktørId: String,
    val vedtakId: Int,
    val beregningsDato: LocalDate,
    val harAvtjentVerneplikt: Boolean? = null,
    val oppfyllerKravTilFangstOgFisk: Boolean? = null,
    val bruktInntektsPeriode: InntektsPeriode? = null,
    val antallBarn: Int? = null,
    val manueltGrunnlag: Int? = null
) {

    fun toJson() = toJson(this)

    companion object Mapper {
        private val adapter = moshiInstance.adapter(Behov::class.java)

        fun toJson(internBehov: Behov): String = adapter.toJson(internBehov)

        fun fromJson(json: String): Behov? = adapter.fromJson(json)
    }
}

data class InternBehov(
    val behovId: String = ulidGenerator.nextULID(),
    val aktørId: String,
    val internId: InternId,
    val beregningsDato: LocalDate,
    val harAvtjentVerneplikt: Boolean? = null,
    val oppfyllerKravTilFangstOgFisk: Boolean? = null,
    val bruktInntektsPeriode: InntektsPeriode? = null,
    val antallBarn: Int? = null,
    val manueltGrunnlag: Int? = null

) {
    fun toJson() = toJson(this)
    fun toPacket() = toPacket(this)
    companion object Mapper {
        private val adapter = moshiInstance.adapter(InternBehov::class.java)

        fun toJson(internBehov: InternBehov): String = adapter.toJson(internBehov)

        fun fromJson(json: String): InternBehov? = adapter.fromJson(json)

        fun toPacket(internBehov: InternBehov): Packet = Packet("{}").apply {
            this.putValue(PacketKeys.BEHOV_ID, internBehov.behovId)
            this.putValue(PacketKeys.AKTØR_ID, internBehov.aktørId)
            when (internBehov.internId.eksternId.kontekst) {
                Kontekst.VEDTAK -> this.putValue(PacketKeys.VEDTAK_ID, internBehov.internId.eksternId.id)
            }
            this.putValue(PacketKeys.INTERN_ID, internBehov.internId.id)
            this.putValue(PacketKeys.BEREGNINGS_DATO, internBehov.beregningsDato)
            internBehov.harAvtjentVerneplikt?.let { this.putValue(PacketKeys.HAR_AVTJENT_VERNE_PLIKT, it) }
            internBehov.oppfyllerKravTilFangstOgFisk?.let { this.putValue(PacketKeys.OPPFYLLER_KRAV_TIL_FANGST_OG_FISK, it) }
            internBehov.bruktInntektsPeriode?.let { this.putValue(PacketKeys.BRUKT_INNTEKTSPERIODE, it.toJson()) }
            internBehov.antallBarn?.let { this.putValue(PacketKeys.ANTALL_BARN, it) }
            internBehov.manueltGrunnlag?.let { this.putValue(PacketKeys.MANUELT_GRUNNLAG, it) }
        }

        fun fromBehov(behov: Behov, internId: InternId): InternBehov {
            return InternBehov(
                internId = internId,
                aktørId = behov.aktørId,
                harAvtjentVerneplikt = behov.harAvtjentVerneplikt,
                oppfyllerKravTilFangstOgFisk = behov.oppfyllerKravTilFangstOgFisk,
                manueltGrunnlag = behov.manueltGrunnlag,
                beregningsDato = behov.beregningsDato,
                bruktInntektsPeriode = behov.bruktInntektsPeriode,
                antallBarn = behov.antallBarn
            )
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
