package no.nav.dagpenger.regel.api.sats

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
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyAll
import no.nav.dagpenger.regel.api.DagpengerBehovProducer
import no.nav.dagpenger.regel.api.Regel
import no.nav.dagpenger.regel.api.Status
import no.nav.dagpenger.regel.api.SubsumsjonsBehov
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.SatsSubsumsjon
import no.nav.dagpenger.regel.api.routes.MockApi
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SatsRouteTest {

    @Test
    fun `Valid json to sats endpoint should be accepted, saved and produce an event to Kafka`() {
        val slot = slot<SubsumsjonsBehov>()
        val storeMock = mockk<SubsumsjonStore>(relaxed = true).apply {
            every { insertBehov(any(), regel = Regel.SATS) } returns 1
        }
        val kafkaMock = mockk<DagpengerBehovProducer>(relaxed = true)

        withTestApplication(MockApi(
            subsumsjonStore = storeMock,
            kafkaDagpengerBehovProducer = kafkaMock
        )) {

            handleRequest(HttpMethod.Post, "/sats") {
                addHeader(HttpHeaders.ContentType, "application/json")
                setBody("""
            {
                "aktorId": "9000000028204",
                "vedtakId": 1,
                "beregningsdato": "2019-01-08",
                "manueltGrunnlag": 100,
                "antallBarn" : 1,
                "harAvtjentVerneplikt": true

            }
            """)
            }.apply {
                response.status() shouldBe HttpStatusCode.Accepted
                withClue("Response should be handled") { requestHandled shouldBe true }
                response.headers.contains(HttpHeaders.Location) shouldBe true
                response.headers[HttpHeaders.Location]?.let { location ->
                    location shouldStartWith "/sats/status/"
                    withClue("Behov id should be present") { location shouldNotEndWith "/sats/status/" }
                }
            }
        }

        verifyAll {
            storeMock.insertBehov(subsumsjonsBehov = capture(slot), regel = Regel.SATS)
            kafkaMock.produceEvent(any())
        }

        with(slot.captured) {
            aktørId shouldBe "9000000028204"
            vedtakId shouldBe 1
            beregningsDato shouldBe LocalDate.parse("2019-01-08")
            manueltGrunnlag shouldBe 100
            antallBarn shouldBe 1
            harAvtjentVerneplikt shouldBe true
        }
    }

    @Test
    fun `Subpaths subsumsjon and status are present`() {
        val storeMock = mockk<SubsumsjonStore>(relaxed = true)

        val subsumsjon = mockk<SatsSubsumsjon>(relaxed = true)

        every {
            storeMock.getSubsumsjon("1", Regel.SATS)
        } returns subsumsjon

        every {
            storeMock.behovStatus("2", Regel.SATS)
        } returns Status.Done("2")

        withTestApplication(MockApi(
            subsumsjonStore = storeMock
        )) {
            handleRequest(HttpMethod.Get, "/sats/1")

            handleRequest(HttpMethod.Get, "/sats/status/2") {}
        }

        verify {
            storeMock.getSubsumsjon("1", Regel.SATS)
            storeMock.behovStatus("2", Regel.SATS)
        }
    }
}
