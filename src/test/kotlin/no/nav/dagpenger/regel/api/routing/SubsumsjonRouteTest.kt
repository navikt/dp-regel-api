package no.nav.dagpenger.regel.api.routing

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
import no.nav.dagpenger.regel.api.db.SubsumsjonNotFoundException
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.Faktum
import no.nav.dagpenger.regel.api.models.Subsumsjon
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SubsumsjonRouteTest {
    @Test
    fun `Returns subsumsjon if found`() {
        val subsumsjon = Subsumsjon(
            "id",
            "behovId",
            Faktum("aktorid", 1, LocalDate.now()),
            mapOf(),
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