package no.nav.dagpenger.regel.api.models

import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class InternBehovTest {

    @Test
    fun `Mapping from BehovV2 to Packet with all fields`() {
        val behov = InternBehov(BehovId("01DSFVQ4NQQ64SNT4Z16TJXXE7"), "aktørId", BehandlingsId.nyBehandlingsIdFraEksternId(RegelKontekst("1234", Kontekst.VEDTAK)), LocalDate.now(), true, true, InntektsPeriode(YearMonth.now(), YearMonth.now()), 1, 1)
        val packet = InternBehov.toPacket(behov)

        packet.getStringValue(PacketKeys.BEHOV_ID) shouldBe behov.behovId.id
        packet.getStringValue(PacketKeys.AKTØR_ID) shouldBe behov.aktørId
        packet.getIntValue(PacketKeys.VEDTAK_ID) shouldBe behov.behandlingsId.regelKontekst.id.toInt()
        packet.getStringValue(PacketKeys.BEHANDLINGSID) shouldBe behov.behandlingsId.id
        packet.getLocalDate(PacketKeys.BEREGNINGS_DATO) shouldBe behov.beregningsDato
        packet.getBoolean(PacketKeys.HAR_AVTJENT_VERNE_PLIKT) shouldBe behov.harAvtjentVerneplikt
        packet.getBoolean(PacketKeys.OPPFYLLER_KRAV_TIL_FANGST_OG_FISK) shouldBe behov.oppfyllerKravTilFangstOgFisk
        InntektsPeriode.fromPacket(packet) shouldBe behov.bruktInntektsPeriode
        packet.getIntValue(PacketKeys.ANTALL_BARN) shouldBe behov.antallBarn
        packet.getIntValue(PacketKeys.MANUELT_GRUNNLAG) shouldBe behov.manueltGrunnlag
    }

    @Test
    fun `Mapping from Behov to Packet with different kontekst type`() {
        BehandlingsId.nyBehandlingsIdFraEksternId(RegelKontekst("1234", Kontekst.VEDTAK)).run {
            val behov = InternBehov(BehovId("01DSFVQ4NQQ64SNT4Z16TJXXE7"), "aktørId", this, LocalDate.now(), true, true, InntektsPeriode(YearMonth.now(), YearMonth.now()), 1, 1)
            val packetWithVedtak = InternBehov.toPacket(behov)
            packetWithVedtak.getIntValue(PacketKeys.VEDTAK_ID) shouldBe this.regelKontekst.id.toInt()
            packetWithVedtak.getNullableIntValue(PacketKeys.CORONA_ID) shouldBe null
        }

        BehandlingsId.nyBehandlingsIdFraEksternId(RegelKontekst("1234", Kontekst.CORONA)).run {
            val behov = InternBehov(BehovId("01DSFVQ4NQQ64SNT4Z16TJXXE7"), "aktørId", this, LocalDate.now(), true, true, InntektsPeriode(YearMonth.now(), YearMonth.now()), 1, 1)
            val packetWithVedtak = InternBehov.toPacket(behov)
            packetWithVedtak.getIntValue(PacketKeys.CORONA_ID) shouldBe this.regelKontekst.id.toInt()
            packetWithVedtak.getNullableIntValue(PacketKeys.VEDTAK_ID) shouldBe null
        }
    }

    @Test
    fun `Mapping from Behov to Packet with nullable fields`() {
        val behov = InternBehov(BehovId("01DSFVQY33P2A5K7GHNC96W3JJ"), "aktørId", BehandlingsId.nyBehandlingsIdFraEksternId(RegelKontekst("1234", Kontekst.VEDTAK)), LocalDate.now())
        val packet = InternBehov.toPacket(behov)

        packet.getNullableBoolean(PacketKeys.HAR_AVTJENT_VERNE_PLIKT) shouldBe null
        packet.getNullableBoolean(PacketKeys.OPPFYLLER_KRAV_TIL_FANGST_OG_FISK) shouldBe null
        InntektsPeriode.fromPacket(packet) shouldBe behov.bruktInntektsPeriode
        packet.getNullableIntValue(PacketKeys.ANTALL_BARN) shouldBe null
        packet.getNullableIntValue(PacketKeys.MANUELT_GRUNNLAG) shouldBe null
    }
}