package no.nav.dagpenger.regel.api.models

import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class InternBehovTest {

    @Test
    fun `Mapping from BehovV2 to Packet with all fields`() {
        val behov = InternBehov("behovId", "aktørId", BehandlingsId.nyBehandlingsIdFraEksternId(EksternId("1234", Kontekst.VEDTAK)), LocalDate.now(), true, true, InntektsPeriode(YearMonth.now(), YearMonth.now()), 1, 1)
        val packet = InternBehov.toPacket(behov)

        packet.getStringValue(PacketKeys.BEHOV_ID) shouldBe behov.behovId
        packet.getStringValue(PacketKeys.AKTØR_ID) shouldBe behov.aktørId
        packet.getIntValue(PacketKeys.VEDTAK_ID) shouldBe behov.behandlingsId.eksternId.id.toInt()
        packet.getStringValue(PacketKeys.BEHANDLINGSID) shouldBe behov.behandlingsId.id
        packet.getLocalDate(PacketKeys.BEREGNINGS_DATO) shouldBe behov.beregningsDato
        packet.getBoolean(PacketKeys.HAR_AVTJENT_VERNE_PLIKT) shouldBe behov.harAvtjentVerneplikt
        packet.getBoolean(PacketKeys.OPPFYLLER_KRAV_TIL_FANGST_OG_FISK) shouldBe behov.oppfyllerKravTilFangstOgFisk
        InntektsPeriode.fromPacket(packet) shouldBe behov.bruktInntektsPeriode
        packet.getIntValue(PacketKeys.ANTALL_BARN) shouldBe behov.antallBarn
        packet.getIntValue(PacketKeys.MANUELT_GRUNNLAG) shouldBe behov.manueltGrunnlag
    }

    @Test
    fun `Mapping from Behov to Packet with nullable fields`() {
        val behov = InternBehov("behovId", "aktørId", BehandlingsId.nyBehandlingsIdFraEksternId(EksternId("1234", Kontekst.VEDTAK)), LocalDate.now())
        val packet = InternBehov.toPacket(behov)

        packet.getNullableBoolean(PacketKeys.HAR_AVTJENT_VERNE_PLIKT) shouldBe null
        packet.getNullableBoolean(PacketKeys.OPPFYLLER_KRAV_TIL_FANGST_OG_FISK) shouldBe null
        InntektsPeriode.fromPacket(packet) shouldBe behov.bruktInntektsPeriode
        packet.getNullableIntValue(PacketKeys.ANTALL_BARN) shouldBe null
        packet.getNullableIntValue(PacketKeys.MANUELT_GRUNNLAG) shouldBe null
    }
}