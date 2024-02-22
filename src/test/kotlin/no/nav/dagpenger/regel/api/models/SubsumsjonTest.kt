@file:Suppress("ktlint:standard:max-line-length")

package no.nav.dagpenger.regel.api.models

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.regel.api.db.JsonAdapter
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class SubsumsjonTest {
    @Test
    fun `Map to JSON string`() {
        Subsumsjon(
            behovId = BehovId("01DSFTA586H33ESMTYMY6QD4ZD"),
            faktum = Faktum("aktorId", RegelKontekst("1", Kontekst.vedtak), LocalDate.of(2019, 5, 9)),
            grunnlagResultat = emptyMap(),
            minsteinntektResultat = emptyMap(),
            periodeResultat = emptyMap(),
            satsResultat = emptyMap(),
            problem = Problem(title = "problem"),
        ).toJson() shouldBe """{"behovId":"01DSFTA586H33ESMTYMY6QD4ZD","faktum":{"aktorId":"aktorId","regelkontekst":{"id":"1","type":"vedtak"},"beregningsdato":"2019-05-09"},"grunnlagResultat":{},"minsteinntektResultat":{},"periodeResultat":{},"satsResultat":{},"problem":{"type":"about:blank","title":"problem","status":500,"instance":"about:blank"}}"""
    }

    @Test
    fun `Map from JSON string to object`() {
        val subsumsjon =
            JsonAdapter.fromJson(
                """{"behovId":"01DSFTA586H33ESMTYMY6QD4ZD","faktum":{"aktorId":"aktorId","regelkontekst":{"id":"1","type":"vedtak"},"beregningsdato":"2019-05-09","inntektsId":"inntektsId","harAvtjentVerneplikt":true,"oppfyllerKravTilFangstOgFisk":true,"antallBarn":1,"manueltGrunnlag":0,"bruktInntektsPeriode":{"førsteMåned":"2019-05","sisteMåned":"2019-05"}},"grunnlagResultat":{},"minsteinntektResultat":{},"periodeResultat":{},"satsResultat":{},"problem":{"type":"about:blank","title":"problem","status":500,"instance":"about:blank"}}""",
            )
        subsumsjon shouldNotBe null

        subsumsjon.apply {
            behovId shouldBe BehovId("01DSFTA586H33ESMTYMY6QD4ZD")
            grunnlagResultat shouldBe emptyMap()
            periodeResultat shouldBe emptyMap()
            minsteinntektResultat shouldBe emptyMap()
            satsResultat shouldBe emptyMap()
            problem shouldNotBe null
        }

        subsumsjon.faktum.apply {
            aktorId shouldBe "aktorId"
            regelkontekst shouldBe RegelKontekst("1", Kontekst.vedtak)
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
