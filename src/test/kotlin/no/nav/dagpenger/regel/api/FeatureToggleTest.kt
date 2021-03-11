package no.nav.dagpenger.regel.api

import io.kotest.matchers.shouldBe
import no.finn.unleash.FakeUnleash
import no.nav.dagpenger.regel.api.models.Kontekst
import no.nav.dagpenger.regel.api.routing.BehovRequest
import no.nav.dagpenger.regel.api.routing.mapRequestToBehov
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FeatureToggleTest {

    @Test
    fun `feature toggle for forhøya dagsats fungerer`() {
        val unleash = FakeUnleash()
        val behovRequest = BehovRequest(
            aktorId = "aktorId",
            regelkontekst = BehovRequest.RegelKontekst("1", Kontekst.vedtak),
            beregningsdato = LocalDate.of(2019, 11, 7),
            harAvtjentVerneplikt = null,
            oppfyllerKravTilFangstOgFisk = null,
            bruktInntektsPeriode = null,
            manueltGrunnlag = null,
            lærling = null,
            antallBarn = null,
            regelverksdato = LocalDate.of(2020, 1, 1),
            forrigeGrunnlag = 3
        )

        unleash.disable(FORHØYA_SATS_TOGGLE)
        mapRequestToBehov(
            behovRequest,
            unleash
        ).also {
            it.regelverksdato shouldBe null
            it.forrigeGrunnlag shouldBe null
        }

        unleash.enable(FORHØYA_SATS_TOGGLE)
        mapRequestToBehov(
            behovRequest,
            unleash
        ).also {
            it.regelverksdato shouldBe LocalDate.of(2020, 1, 1)
            it.forrigeGrunnlag shouldBe 3
        }
    }
}
