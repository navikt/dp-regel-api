package no.nav.dagpenger.regel.api.db

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class EksternSubsumsjonBruktTest {

    @Test
    fun `json deserialisering og serialisering`() {
        val json =
            """
            {
              "eksternId": "3.6774602E7",
              "id": "01F00VRPMJAF8YMEKYVS1CMESY",
              "arenaTs": "2021-03-05T10:58:15+01:00[Europe/Oslo]",
              "ts": 1614938299326,
              "utfall": "JA",
              "vedtakStatus": "IVERK"
            }
            """.trimIndent()

        val eksternSubsumsjonBrukt = EksternSubsumsjonBrukt.fromJson(json)

        eksternSubsumsjonBrukt shouldNotBe null
        eksternSubsumsjonBrukt.eksternId shouldBe 36774602L

        EksternSubsumsjonBrukt.fromJson(eksternSubsumsjonBrukt.toJson()) shouldBe eksternSubsumsjonBrukt
    }
}
