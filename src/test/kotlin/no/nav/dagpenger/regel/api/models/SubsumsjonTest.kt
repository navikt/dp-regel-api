package no.nav.dagpenger.regel.api.models

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import no.nav.dagpenger.events.Problem
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class SubsumsjonTest {

    @Test
    fun `Map to JSON string`() {
        Subsumsjon(
            "id",
            "behovId",
            Faktum("aktorId", 1, LocalDate.of(2019, 5, 9)),
            mapOf(),
            mapOf(),
            mapOf(),
            mapOf(),
            Problem(title = "problem")

        ).toJson() shouldBe """{"id":"id","behovId":"behovId","faktum":{"aktorId":"aktorId","vedtakId":1,"beregningsdato":"2019-05-09"},"grunnlagResultat":{},"minsteinntektResultat":{},"periodeResultat":{},"satsResultat":{},"problem":{"type":"about:blank","title":"problem","status":500,"instance":"about:blank"}}"""
    }

    @Test
    fun `Map from JSON string to object`() {
        val subsumsjon = Subsumsjon.fromJson("""{"id":"id","behovId":"behovId","faktum":{"aktorId":"aktorId","vedtakId":1,"beregningsdato":"2019-05-09","inntektsId":"inntektsId","harAvtjentVerneplikt":true,"oppfyllerKravTilFangstOgFisk":true,"antallBarn":1,"manueltGrunnlag":0,"bruktInntektsPeriode":{"førsteMåned":"2019-05","sisteMåned":"2019-05"}},"grunnlagResultat":{},"minsteinntektResultat":{},"periodeResultat":{},"satsResultat":{},"problem":{"type":"about:blank","title":"problem","status":500,"instance":"about:blank"}}""")
        subsumsjon shouldNotBe null

        subsumsjon?.apply {
            id shouldBe "id"
            behovId shouldBe "behovId"
            grunnlagResultat shouldBe mapOf()
            periodeResultat shouldBe mapOf()
            minsteinntektResultat shouldBe mapOf()
            satsResultat shouldBe mapOf()
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