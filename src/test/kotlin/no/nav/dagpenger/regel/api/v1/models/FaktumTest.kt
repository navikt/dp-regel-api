package no.nav.dagpenger.regel.api.v1.models

import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth


internal class FaktumTest {
    @Test
    fun `Mapping from  Packet to Faktum`() {
        val packet = Behov("behovId", "aktørId", 1, LocalDate.of(2011, 7, 22), true, true, InntektsPeriode(YearMonth.of(2011, 7), YearMonth.of(2011, 7)), 1, 1).toPacket().apply {
            this.putValue(PacketKeys.INNTEKT, """{"inntektsId": "inntektsId",  "inntektsListe":[]}""")
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
        }
    }
}