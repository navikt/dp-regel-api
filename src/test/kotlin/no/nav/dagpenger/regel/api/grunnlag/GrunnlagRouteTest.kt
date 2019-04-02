package no.nav.dagpenger.regel.api.grunnlag

import io.kotlintest.matchers.string.shouldNotEndWith
import io.kotlintest.matchers.string.shouldStartWith
import io.kotlintest.matchers.withClue
import io.kotlintest.shouldBe
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.mockk.mockk
import io.mockk.verifyAll
import no.nav.dagpenger.regel.api.DagpengerBehovProducer
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.routes.MockApi
import org.junit.jupiter.api.Test

class GrunnlagRouteTest {
    @Test
    fun `Valid json to grunnlag endpoint should be accepted, saved and produce an event to Kafka`() {
        val storeMock = mockk<SubsumsjonStore>(relaxed = true)
        val kafkaMock = mockk<DagpengerBehovProducer>(relaxed = true)

        withTestApplication(MockApi(
            storeMock,
            kafkaMock
        )) {

            handleRequest(HttpMethod.Post, "/grunnlag") {
                addHeader(HttpHeaders.ContentType, "application/json")
                setBody("""
            {
                "aktorId": "9000000028204",
                "vedtakId": 1,
                "beregningsdato": "2019-01-08",
                "manueltGrunnlag": 54200
            }
            """)
            }.apply {
                response.status() shouldBe HttpStatusCode.Accepted
                withClue("Response should be handled") { requestHandled shouldBe true }
                response.headers.contains(HttpHeaders.Location) shouldBe true
                response.headers[HttpHeaders.Location]?.let { location ->
                    location shouldStartWith "/grunnlag/status/"
                    withClue("Behov id should be present") { location shouldNotEndWith "/grunnlag/status/" }
                }
            }
        }

        verifyAll {
            storeMock.insertBehov(any())
            kafkaMock.produceEvent(any())
        }
    }

    @Test
    fun `Subpaths subsumsjon and status are present`() {
        val storeMock = mockk<SubsumsjonStore>(relaxed = true)

        withTestApplication(MockApi(
            subsumsjonStore = storeMock
        )) {
            handleRequest(HttpMethod.Get, "/grunnlag/1")

            handleRequest(HttpMethod.Get, "/grunnlag/status/2")
        }

        verifyAll {
            storeMock.getSubsumsjon("1")
            storeMock.behovStatus("2")
        }
    }
}
