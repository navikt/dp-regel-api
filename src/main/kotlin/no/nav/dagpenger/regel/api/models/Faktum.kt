package no.nav.dagpenger.regel.api.models

import no.nav.dagpenger.events.Packet
import java.time.LocalDate

data class Faktum(
    val aktorId: String,
    val regelkontekst: RegelKontekst,
    val vedtakId: Int,
    val beregningsdato: LocalDate,
    val inntektsId: String? = null,
    val inntektAvvik: Boolean? = null,
    val inntektManueltRedigert: Boolean? = null,
    val harAvtjentVerneplikt: Boolean? = null,
    val oppfyllerKravTilFangstOgFisk: Boolean? = null,
    val antallBarn: Int? = null,
    val manueltGrunnlag: Int? = null,
    val lærling: Boolean? = null,
    val bruktInntektsPeriode: InntektsPeriode? = null,
    val regelverksdato: LocalDate? = null
) {

    companion object Mapper {
        fun faktumFrom(packet: Packet): Faktum {
            val inntekt = inntektFrom(packet)
            return Faktum(
                aktorId = packet.getStringValue(PacketKeys.AKTØR_ID),
                regelkontekst = RegelKontekst(
                    packet.getStringValue(PacketKeys.KONTEKST_ID),
                    Kontekst.valueOf(packet.getStringValue(PacketKeys.KONTEKST_TYPE))
                ),
                vedtakId = packet.getIntValue(PacketKeys.VEDTAK_ID),
                beregningsdato = packet.getLocalDate(PacketKeys.BEREGNINGS_DATO),
                inntektsId = inntekt?.inntektsId,
                inntektAvvik = inntekt?.harAvvik(),
                inntektManueltRedigert = inntekt?.manueltRedigert,
                harAvtjentVerneplikt = packet.getNullableBoolean(PacketKeys.HAR_AVTJENT_VERNE_PLIKT),
                oppfyllerKravTilFangstOgFisk = packet.getNullableBoolean(PacketKeys.OPPFYLLER_KRAV_TIL_FANGST_OG_FISK),
                antallBarn = packet.getNullableIntValue(PacketKeys.ANTALL_BARN),
                manueltGrunnlag = packet.getNullableIntValue(PacketKeys.MANUELT_GRUNNLAG),
                lærling = packet.getNullableBoolean(PacketKeys.LÆRLING),
                bruktInntektsPeriode = InntektsPeriode.fromPacket(packet),
                regelverksdato = packet.getNullableLocalDate(PacketKeys.REGELVERKSDATO)
            )
        }
    }
}
