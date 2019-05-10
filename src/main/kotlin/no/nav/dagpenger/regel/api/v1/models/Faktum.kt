package no.nav.dagpenger.regel.api.v1.models

import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.regel.api.moshiInstance
import java.time.LocalDate

internal data class Faktum(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: LocalDate,
    val inntektsId: String? = null,
    val harAvtjentVerneplikt: Boolean? = null,
    val oppfyllerKravTilFangstOgFisk: Boolean? = null,
    val antallBarn: Int? = null,
    val manueltGrunnlag: Int? = null,
    val bruktInntektsPeriode: InntektsPeriode? = null
) {
    companion object Mapper {
        fun faktumFrom(packet: Packet): Faktum =
            Faktum(
                aktorId = packet.getStringValue(PacketKeys.AKTØR_ID),
                vedtakId = packet.getIntValue(PacketKeys.VEDTAK_ID),
                beregningsdato = packet.getLocalDate(PacketKeys.BEREGNINGS_DATO),
                inntektsId = inntektsIdFrom(packet),
                harAvtjentVerneplikt = packet.getNullableBoolean(PacketKeys.HAR_AVTJENT_VERNE_PLIKT),
                oppfyllerKravTilFangstOgFisk = packet.getNullableBoolean(PacketKeys.OPPFYLLER_KRAV_TIL_FANGST_OG_FISK),
                antallBarn = packet.getNullableIntValue(PacketKeys.ANTALL_BARN),
                manueltGrunnlag = packet.getNullableIntValue(PacketKeys.MANUELT_GRUNNLAG),
                bruktInntektsPeriode = bruktInntekstPeriodeFrom(packet)
            )
    }
}

private fun bruktInntekstPeriodeFrom(packet: Packet): InntektsPeriode? =
    packet.getNullableObjectValue(PacketKeys.BRUKT_INNTEKTSPERIODE) { json ->
        moshiInstance.adapter<InntektsPeriode>(InntektsPeriode::class.java).fromJsonValue(json)
    }

private fun inntektsIdFrom(packet: Packet): String? = packet.getNullableObjectValue(PacketKeys.INNTEKT) { json ->
    moshiInstance.adapter<Inntekt>(Inntekt::class.java).fromJsonValue(json)?.inntektsId
}

