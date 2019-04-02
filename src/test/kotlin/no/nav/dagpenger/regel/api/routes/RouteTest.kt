package no.nav.dagpenger.regel.api.routes

import io.kotlintest.matchers.withClue
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.shouldThrow
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyAll
import no.nav.dagpenger.regel.api.Status
import no.nav.dagpenger.regel.api.db.BehovNotFoundException
import no.nav.dagpenger.regel.api.db.SubsumsjonNotFoundException
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.GrunnlagSubsumsjon
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class StatusRouteTest {
    @Test
    fun `Status when behov is done, pending or not found`() {
        val storeMock = mockk<SubsumsjonStore>(relaxed = false)
        every { storeMock.behovStatus("pending") } returns Status.Pending
        every { storeMock.behovStatus("done") } returns Status.Done("subsumsjonid")
        every { storeMock.behovStatus("notfound") } throws BehovNotFoundException("not found")

        withTestApplication(MockApi(
            subsumsjonStore = storeMock
        )) {

            handleRequest(HttpMethod.Get, "/grunnlag/status/pending")
                .apply {
                    response.status() shouldBe HttpStatusCode.OK
                    withClue("Response should be handled") { requestHandled shouldBe true }
                    response.content shouldNotBe null
                    response.content shouldBe """{"regel":"GRUNNLAG","status":"PENDING","expires":"not in use"}"""
                }

            handleRequest(HttpMethod.Get, "/grunnlag/status/done")
                .apply {
                    response.status() shouldBe HttpStatusCode.SeeOther
                    withClue("Response should be handled") { requestHandled shouldBe true }
                    response.headers[HttpHeaders.Location] shouldNotBe null
                    response.headers[HttpHeaders.Location] shouldBe "/grunnlag/subsumsjonid"
                }

            shouldThrow<BehovNotFoundException> {
                handleRequest(HttpMethod.Get, "/grunnlag/status/notfound")
            }
        }

        verifyAll {
            storeMock.behovStatus("pending")
            storeMock.behovStatus("done")
            storeMock.behovStatus("notfound")
        }
    }
}

class SubsumsjonTest {
    @Test
    fun `Returns subsumsjon if found`() {
        val storeMock = mockk<SubsumsjonStore>(relaxed = false)
        val subsumsjonMock = mockk<GrunnlagSubsumsjon>(relaxed = true)

        every { storeMock.getSubsumsjon("found") } returns subsumsjonMock

        withTestApplication(MockApi(
            subsumsjonStore = storeMock
        )) {

            handleRequest(HttpMethod.Get, "/grunnlag/found")
                .apply {
                    response.status() shouldBe HttpStatusCode.OK
                    response.content shouldNotBe null
                }
        }

        verifyAll {
            storeMock.getSubsumsjon("found")
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
                handleRequest(HttpMethod.Get, "/grunnlag/notfound")
            }
        }

        verifyAll {
            storeMock.getSubsumsjon("notfound")
        }
    }
}

class ActuatorTest {

    @Test
    fun `isAlive and isReady route returns 200 OK`() {
        withTestApplication(MockApi()) {
            handleRequest(HttpMethod.Get, "/isAlive").apply {
                response.status() shouldBe HttpStatusCode.OK
                response.content shouldBe "ALIVE"
            }

            handleRequest(HttpMethod.Get, "/isReady").apply {
                response.status() shouldBe HttpStatusCode.OK
                response.content shouldBe "READY"
            }
        }
    }

    @Test
    @Disabled("Not implemented")
    fun `The application produces metrics`() {
        withTestApplication(MockApi()) {
            handleRequest(HttpMethod.Get, "/metrics").run {
                response.status() shouldBe HttpStatusCode.OK
            }
        }
    }
}
