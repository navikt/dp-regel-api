package no.nav.dagpenger.regel.api.v1.models

import no.nav.dagpenger.events.Packet

internal object PacketKeys {
    const val MANUELT_GRUNNLAG = "manueltGrunnlag"
    const val ANTALL_BARN = "antallBarn"
    const val HAR_AVTJENT_VERNE_PLIKT = "harAvtjentVernePlikt"
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
    const val SENESTE_INNTEKTSMÅNED = "senesteInntektsmåned"
    const val OPPFYLLER_KRAV_TIL_FANGST_OG_FISK = "oppfyllerKravTilFangstOgFisk"
    const val BRUKT_INNTEKTSPERIODE = "bruktInntektsPeriode"
}

internal fun Packet.getBehovId(): String =
    this.getStringValue(PacketKeys.BEHOV_ID)

