package no.nav.dagpenger.regel.api.models

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class PacketTest {

    @Test
    fun `Mapping from InternBehov to Packet with all fields`() {
        val behov = InternBehov(
            behovId = BehovId("01DSFVQ4NQQ64SNT4Z16TJXXE7"),
            aktørId = "aktørId",
            behandlingsId = BehandlingsId.nyBehandlingsIdFraEksternId(RegelKontekst("1234", Kontekst.VEDTAK)),
            beregningsDato = LocalDate.now(),
            harAvtjentVerneplikt = true,
            oppfyllerKravTilFangstOgFisk = true,
            bruktInntektsPeriode = InntektsPeriode(YearMonth.now(), YearMonth.now()),
            antallBarn = 1,
            manueltGrunnlag = 1,
            lærling = false,
            regelverksdato = LocalDate.of(2020, 1, 2)
        )
        val packet = InternBehov.toPacket(behov)

        packet.getStringValue(PacketKeys.BEHOV_ID) shouldBe behov.behovId.id
        packet.getStringValue(PacketKeys.AKTØR_ID) shouldBe behov.aktørId
        packet.getIntValue(PacketKeys.VEDTAK_ID) shouldBe behov.behandlingsId.regelKontekst.id.toInt()
        packet.kontekst shouldBe behov.behandlingsId.regelKontekst.type
        packet.kontekstId shouldBe behov.behandlingsId.regelKontekst.id
        packet.getStringValue(PacketKeys.BEHANDLINGSID) shouldBe behov.behandlingsId.id
        packet.getLocalDate(PacketKeys.BEREGNINGS_DATO) shouldBe behov.beregningsDato
        packet.getBoolean(PacketKeys.HAR_AVTJENT_VERNE_PLIKT) shouldBe behov.harAvtjentVerneplikt
        packet.getBoolean(PacketKeys.OPPFYLLER_KRAV_TIL_FANGST_OG_FISK) shouldBe behov.oppfyllerKravTilFangstOgFisk
        InntektsPeriode.fromPacket(packet) shouldBe behov.bruktInntektsPeriode
        packet.getIntValue(PacketKeys.ANTALL_BARN) shouldBe behov.antallBarn
        packet.getIntValue(PacketKeys.MANUELT_GRUNNLAG) shouldBe behov.manueltGrunnlag
        packet.getBoolean(PacketKeys.LÆRLING) shouldBe behov.lærling
        packet.getLocalDate(PacketKeys.REGELVERKSDATO) shouldBe behov.regelverksdato
    }

    @Test
    fun `Mapping from InternBehov to Packet with nullable fields`() {
        val behov = InternBehov(BehovId("01DSFVQY33P2A5K7GHNC96W3JJ"), "aktørId", BehandlingsId.nyBehandlingsIdFraEksternId(RegelKontekst("1234", Kontekst.VEDTAK)), LocalDate.now())
        val packet = InternBehov.toPacket(behov)

        packet.getNullableBoolean(PacketKeys.HAR_AVTJENT_VERNE_PLIKT) shouldBe null
        packet.getNullableBoolean(PacketKeys.OPPFYLLER_KRAV_TIL_FANGST_OG_FISK) shouldBe null
        InntektsPeriode.fromPacket(packet) shouldBe behov.bruktInntektsPeriode
        packet.getNullableIntValue(PacketKeys.ANTALL_BARN) shouldBe null
        packet.getNullableIntValue(PacketKeys.MANUELT_GRUNNLAG) shouldBe null
        packet.getNullableBoolean(PacketKeys.LÆRLING) shouldBe null
        packet.getNullableLocalDate(PacketKeys.REGELVERKSDATO) shouldBe null
    }
}
