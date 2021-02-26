package no.nav.dagpenger.regel.api.models

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.events.Problem
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class SubsumsjonTest {

    @Test
    fun `Map to JSON string`() {
        Subsumsjon(
            behovId = BehovId("01DSFTA586H33ESMTYMY6QD4ZD"),
            faktum = Faktum(
                aktorId = "aktorId",
                vedtakId = 1,
                kontekst = RegelKontekst("id", Kontekst.VEDTAK),
                beregningsdato = LocalDate.now()
            ),
            grunnlagResultat = emptyMap(),
            minsteinntektResultat = emptyMap(),
            periodeResultat = emptyMap(),
            satsResultat = emptyMap(),
            problem = Problem(title = "problem")

        ).toJson() shouldBe """{"behovId":"01DSFTA586H33ESMTYMY6QD4ZD","faktum":{"aktorId":"aktorId","vedtakId":1,"kontekst":{"id":"id","type":"vedtak"},"beregningsdato":"2021-02-26"},"grunnlagResultat":{},"minsteinntektResultat":{},"periodeResultat":{},"satsResultat":{},"problem":{"type":"about:blank","title":"problem","status":500,"instance":"about:blank"}}""".trimIndent()
    }

    @Test
    fun `Map from JSON string to object`() {
        @Language("JSON") val subsumsjon =
            Subsumsjon.fromJson("""{"behovId":"01DSFTA586H33ESMTYMY6QD4ZD","faktum":{"aktorId":"aktorId","vedtakId":1,"kontekst": { "id": "id", "type": "vedtak" },"beregningsdato":"2019-05-09","inntektsId":"inntektsId","harAvtjentVerneplikt":true,"oppfyllerKravTilFangstOgFisk":true,"antallBarn":1,"manueltGrunnlag":0,"bruktInntektsPeriode":{"førsteMåned":"2019-05","sisteMåned":"2019-05"}},"grunnlagResultat":{},"minsteinntektResultat":{},"periodeResultat":{},"satsResultat":{},"problem":{"type":"about:blank","title":"problem","status":500,"instance":"about:blank"}}""")
        subsumsjon shouldNotBe null

        subsumsjon?.apply {
            behovId shouldBe BehovId("01DSFTA586H33ESMTYMY6QD4ZD")
            grunnlagResultat shouldBe emptyMap()
            periodeResultat shouldBe emptyMap()
            minsteinntektResultat shouldBe emptyMap()
            satsResultat shouldBe emptyMap()
            problem shouldNotBe null
        }

        subsumsjon?.faktum?.apply {
            aktorId shouldBe "aktorId"
            vedtakId shouldBe 1
            beregningsdato shouldBe LocalDate.of(2019, 5, 9)
            inntektsId shouldBe "inntektsId"
            harAvtjentVerneplikt shouldBe true
            oppfyllerKravTilFangstOgFisk shouldBe true
            antallBarn shouldBe 1
            manueltGrunnlag shouldBe 0
            bruktInntektsPeriode shouldBe InntektsPeriode(YearMonth.of(2019, 5), YearMonth.of(2019, 5))
        }
    }
}
