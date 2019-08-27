package no.nav.dagpenger.regel.api.models

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import org.junit.jupiter.api.Test

class InternIdTest {



    @Test
    fun `Skal generere unik intern id basert på ekstern id som er sendt inn `(){

        val internId1 = InternId.nyInternIdFraEksternId(EksternId("1234", Kontekst.VEDTAK))
        val internId2 = InternId.nyInternIdFraEksternId(EksternId("1234", Kontekst.VEDTAK))

        internId1 shouldBe internId1
        internId1 shouldNotBe internId2

    }
}