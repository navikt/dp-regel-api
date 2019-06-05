package no.nav.dagpenger.regel.api.models

import no.nav.dagpenger.events.Packet
import java.time.LocalDate

internal data class Faktum(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: LocalDate,
    val inntektsId: String? = null,
    val inntektAvvik: Boolean? = null,
    val inntektManueltRedigert: Boolean? = null,
    val harAvtjentVerneplikt: Boolean? = null,
    val oppfyllerKravTilFangstOgFisk: Boolean? = null,
    val antallBarn: Int? = null,
    val manueltGrunnlag: Int? = null,
    val bruktInntektsPeriode: InntektsPeriode? = null
) {
    companion object Mapper {
        fun faktumFrom(packet: Packet): Faktum {
            val inntekt = inntektFrom(packet)
            return Faktum(
                aktorId = packet.getStringValue(PacketKeys.AKTØR_ID),
                vedtakId = packet.getIntValue(PacketKeys.VEDTAK_ID),
                beregningsdato = packet.getLocalDate(PacketKeys.BEREGNINGS_DATO),
                inntektsId = inntekt?.inntektsId,
                inntektAvvik = inntekt?.harAvvik(),
                inntektManueltRedigert = inntekt?.manueltRedigert,
                harAvtjentVerneplikt = packet.getNullableBoolean(PacketKeys.HAR_AVTJENT_VERNE_PLIKT),
                oppfyllerKravTilFangstOgFisk = packet.getNullableBoolean(PacketKeys.OPPFYLLER_KRAV_TIL_FANGST_OG_FISK),
                antallBarn = packet.getNullableIntValue(PacketKeys.ANTALL_BARN),
                manueltGrunnlag = packet.getNullableIntValue(PacketKeys.MANUELT_GRUNNLAG),
                bruktInntektsPeriode = InntektsPeriode.fromPacket(packet)
            )
        }
    }
}
