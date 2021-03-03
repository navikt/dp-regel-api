package no.nav.dagpenger.regel.api.models

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class FaktumTest {
    @Test
    fun `Mapping from  Packet to Faktum`() {
        val packet = InternBehov.fromBehov(
            behov = Behov(
                aktørId = "aktørId",
                vedtakId = 1,
                beregningsDato = LocalDate.of(2011, 7, 22),
                harAvtjentVerneplikt = true,
                oppfyllerKravTilFangstOgFisk = true,
                bruktInntektsPeriode = InntektsPeriode(YearMonth.of(2011, 7), YearMonth.of(2011, 7)),
                antallBarn = 1,
                lærling = false,
                manueltGrunnlag = 1,
                regelverksdato = LocalDate.of(2020, 1, 1)
            ),
            behandlingsId = BehandlingsId.nyBehandlingsIdFraEksternId(RegelKontekst("1", Kontekst.VEDTAK))
        ).toPacket().apply {
            this.putValue(
                PacketKeys.INNTEKT,
                mapOf(
                    Pair("inntektsId", "inntektsId"),
                    Pair("inntektsListe", listOf<String>()),
                    Pair("manueltRedigert", true),
                    Pair("sisteAvsluttendeKalenderMåned", YearMonth.now().toString())
                )
            )
        }

        Faktum.faktumFrom(packet).let {
            it.aktorId shouldBe "aktørId"
            it.vedtakId shouldBe 1
            it.beregningsdato shouldBe LocalDate.of(2011, 7, 22)
            it.inntektsId shouldBe "inntektsId"
            it.harAvtjentVerneplikt shouldBe true
            it.oppfyllerKravTilFangstOgFisk shouldBe true
            it.antallBarn shouldBe 1
            it.manueltGrunnlag shouldBe 1
            it.bruktInntektsPeriode shouldBe InntektsPeriode(YearMonth.of(2011, 7), YearMonth.of(2011, 7))
            it.lærling shouldBe false
            it.regelverksdato shouldBe LocalDate.of(2020, 1, 1)
        }
    }
}
