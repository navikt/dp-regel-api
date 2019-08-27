package no.nav.dagpenger.regel.api.models

import no.nav.dagpenger.events.Packet

internal object PacketKeys {
    const val BEHANDLINGSID: String = "behandlingsId"
    const val MANUELT_GRUNNLAG = "manueltGrunnlag"
    const val ANTALL_BARN = "antallBarn"
    const val HAR_AVTJENT_VERNE_PLIKT = "harAvtjentVerneplikt"
    const val BEREGNINGS_DATO = "beregningsDato"
    const val VEDTAK_ID = "vedtakId"
    const val AKTØR_ID = "aktørId"
    const val BEHOV_ID = "behovId"
    const val GRUNNLAG_RESULTAT = "grunnlagResultat"
    const val GRUNNLAG_INNTEKTSPERIODER = "grunnlagInntektsPerioder"
    const val INNTEKT = "inntektV1"
    const val MINSTEINNTEKT_RESULTAT = "minsteinntektResultat"
    const val MINSTEINNTEKT_INNTEKTSPERIODER = "minsteinntektInntektsPerioder"
    const val PERIODE_RESULTAT = "periodeResultat"
    const val SATS_RESULTAT = "satsResultat"
    const val OPPFYLLER_KRAV_TIL_FANGST_OG_FISK = "oppfyllerKravTilFangstOgFisk"
    const val BRUKT_INNTEKTSPERIODE = "bruktInntektsPeriode"
    const val PROBLEM = "system_problem"
    const val INNTEKTS_ID = "inntektsId"
}

internal val Packet.behovId: String
    get() = this.getStringValue(PacketKeys.BEHOV_ID)
