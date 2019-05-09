package no.nav.dagpenger.regel.api.v1.routing

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.shouldThrow
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyAll
import no.nav.dagpenger.regel.api.v1.db.SubsumsjonNotFoundException
import no.nav.dagpenger.regel.api.v1.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.v1.models.Faktum
import no.nav.dagpenger.regel.api.v1.models.InntektsPeriode
import no.nav.dagpenger.regel.api.v1.models.Subsumsjon
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth


internal class SubsumsjonRouteTest {
    @Test
    fun `Returns subsumsjon if found`() {
        val subsumsjon = Subsumsjon(
            "id",
            "behovId",
            Faktum("aktorid", 1, LocalDate.now(), "inntektsId", true, true, 1, 0, InntektsPeriode(YearMonth.now(), YearMonth.now())),
            mapOf(),
            mapOf(),
            mapOf(),
            mapOf()
        )

        val storeMock = mockk<SubsumsjonStore>(relaxed = false).apply {
            every { this@apply.getSubsumsjon("subsumsjonsid") } returns subsumsjon
        }

        withTestApplication(MockApi(
            subsumsjonStore = storeMock
        )) {

            handleRequest(HttpMethod.Get, "/subsumsjon/subsumsjonsid")
                .apply {
                    response.status() shouldBe HttpStatusCode.OK
                    response.content shouldNotBe null
                    response.content?.let {
                        Subsumsjon.fromJson(it) shouldBe subsumsjon
                    }
                }
        }

        verifyAll {
            storeMock.getSubsumsjon("subsumsjonsid")
        }


    }

    @Test
    fun `Throws exception if not found`() {
        val storeMock = mockk<SubsumsjonStore>(relaxed = false)

        every { storeMock.getSubsumsjon("notfound") } throws SubsumsjonNotFoundException("Not found")

        withTestApplication(MockApi(
            subsumsjonStore = storeMock
        )) {
            shouldThrow<SubsumsjonNotFoundException> {
                handleRequest(HttpMethod.Get, "/subsumsjon/notfound")
            }
        }

        verifyAll {
            storeMock.getSubsumsjon("notfound")
        }
    }
}