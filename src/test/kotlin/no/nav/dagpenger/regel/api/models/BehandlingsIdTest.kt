package no.nav.dagpenger.regel.api.models

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class BehandlingsIdTest {
    @Test
    fun `Skal generere unik intern id basert på ekstern id som er sendt inn `() {
        val behandlingsId1 = BehandlingsId.nyBehandlingsIdFraEksternId(RegelKontekst("1234", Kontekst.vedtak))
        val behandlingsId2 = BehandlingsId.nyBehandlingsIdFraEksternId(RegelKontekst("1234", Kontekst.vedtak))

        behandlingsId1 shouldBe behandlingsId1
        behandlingsId1 shouldNotBe behandlingsId2
    }
}
