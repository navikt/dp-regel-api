package no.nav.dagpenger.regel.api.models

import io.kotlintest.shouldBe
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class InternBehovTest {

    @Test
    fun `Mapping from BehovV2 to Packet with all fields`() {
        val behov = InternBehov("behovId", "aktørId", InternId.nyInternIdFraEksternId(EksternId("1234", Kontekst.VEDTAK)), LocalDate.now(), true, true, InntektsPeriode(YearMonth.now(), YearMonth.now()), 1, 1)
        val packet = InternBehov.toPacket(behov)

        packet.getStringValue(PacketKeys.BEHOV_ID) shouldBe behov.behovId
        packet.getStringValue(PacketKeys.AKTØR_ID) shouldBe behov.aktørId
        packet.getIntValue(PacketKeys.VEDTAK_ID) shouldBe behov.internId.eksternId.id.toInt()
        packet.getStringValue(PacketKeys.INTERN_ID) shouldBe behov.internId.id
        packet.getLocalDate(PacketKeys.BEREGNINGS_DATO) shouldBe behov.beregningsDato
        packet.getBoolean(PacketKeys.HAR_AVTJENT_VERNE_PLIKT) shouldBe behov.harAvtjentVerneplikt
        packet.getBoolean(PacketKeys.OPPFYLLER_KRAV_TIL_FANGST_OG_FISK) shouldBe behov.oppfyllerKravTilFangstOgFisk
        InntektsPeriode.fromPacket(packet) shouldBe behov.bruktInntektsPeriode
        packet.getIntValue(PacketKeys.ANTALL_BARN) shouldBe behov.antallBarn
        packet.getIntValue(PacketKeys.MANUELT_GRUNNLAG) shouldBe behov.manueltGrunnlag
    }

    @Test
    fun `Mapping from Behov to Packet with nullable fields`() {
        val behov = InternBehov("behovId", "aktørId", InternId.nyInternIdFraEksternId(EksternId("1234", Kontekst.VEDTAK)), LocalDate.now())
        val packet = InternBehov.toPacket(behov)

        packet.getNullableBoolean(PacketKeys.HAR_AVTJENT_VERNE_PLIKT) shouldBe null
        packet.getNullableBoolean(PacketKeys.OPPFYLLER_KRAV_TIL_FANGST_OG_FISK) shouldBe null
        InntektsPeriode.fromPacket(packet) shouldBe behov.bruktInntektsPeriode
        packet.getNullableIntValue(PacketKeys.ANTALL_BARN) shouldBe null
        packet.getNullableIntValue(PacketKeys.MANUELT_GRUNNLAG) shouldBe null
    }

    @Property
    fun `roundtripping json generation`(@ForAll("behovGenerator") internBehov: InternBehov): Boolean {
        return internBehov == InternBehov.fromJson(internBehov.toJson())
    }

    @Provide
    fun behovGenerator(): Arbitrary<InternBehov> {
        val aktorIder = Arbitraries.strings()
        val eksternId = Arbitraries.integers()
        val verneplikt = Arbitraries.integers().between(0, 1)
        val fangst = Arbitraries.integers().between(0, 1)
        val antallBarn = Arbitraries.integers().between(0, 10)
        val manuellGrunnlag = Arbitraries.integers().between(0, 581298) // 6 G
        return Combinators.combine(aktorIder, eksternId, verneplikt, fangst, antallBarn, manuellGrunnlag).`as` { aktor, ekstern, vern, fangstOgFisk, barn, grunnlag ->
            InternBehov(
                aktørId = aktor,
                internId = InternId.nyInternIdFraEksternId(EksternId(ekstern.toString(), Kontekst.VEDTAK)),
                harAvtjentVerneplikt = vern == 0,
                oppfyllerKravTilFangstOgFisk = fangstOgFisk == 0,
                manueltGrunnlag = grunnlag,
                antallBarn = barn,
                beregningsDato = LocalDate.now()
            )
        }
    }
}