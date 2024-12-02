package no.nav.dagpenger.regel.api.routing

import de.huxhorn.sulky.ulid.ULID
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyAll
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.regel.api.db.JsonAdapter
import no.nav.dagpenger.regel.api.db.SubsumsjonNotFoundException
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.BehovId
import no.nav.dagpenger.regel.api.models.Faktum
import no.nav.dagpenger.regel.api.models.Kontekst
import no.nav.dagpenger.regel.api.models.RegelKontekst
import no.nav.dagpenger.regel.api.models.Subsumsjon
import no.nav.dagpenger.regel.api.models.SubsumsjonId
import no.nav.dagpenger.regel.api.routing.TestApplication.handleAuthenticatedAzureAdRequest
import no.nav.dagpenger.regel.api.routing.TestApplication.withMockAuthServerAndTestApplication
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SubsumsjonRouteV1Test {
    @Test
    fun `401 on unauthorized requests`() {
        withMockAuthServerAndTestApplication(mockApi()) {
            handleRequest(HttpMethod.Get, "v1/subsumsjon/id").response.status() shouldBe HttpStatusCode.Unauthorized
            handleRequest(HttpMethod.Get, "v1/subsumsjon/id") { addHeader(HttpHeaders.Authorization, "Bearer notvalid") }
                .response.status() shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Returns subsumsjon if found`() {
        val subsumsjon =
            Subsumsjon(
                behovId = BehovId("01DSFSSNA8S577XGQ8V1R9EBJ7"),
                faktum = Faktum("aktorId", RegelKontekst("1", Kontekst.vedtak), LocalDate.now()),
                grunnlagResultat = emptyMap(),
                minsteinntektResultat = emptyMap(),
                periodeResultat = emptyMap(),
                satsResultat = emptyMap(),
                problem = Problem(title = "problem"),
            )

        val storeMock =
            mockk<SubsumsjonStore>(relaxed = false).apply {
                every { this@apply.getSubsumsjon(BehovId("01DSFGFVF3C1D1QQR69C7BRJT5")) } returns subsumsjon
            }

        withMockAuthServerAndTestApplication(
            mockApi(
                subsumsjonStore = storeMock,
            ),
        ) {
            handleAuthenticatedAzureAdRequest(HttpMethod.Get, "v1/subsumsjon/01DSFGFVF3C1D1QQR69C7BRJT5")
                .apply {
                    response.status() shouldBe HttpStatusCode.OK
                    response.headers["Content-Type"] shouldBe ContentType.Application.Json.toString()
                    response.content shouldNotBe null
                    response.content?.let {
                        JsonAdapter.fromJson(it) shouldBe subsumsjon
                    }
                }
        }

        verifyAll {
            storeMock.getSubsumsjon(BehovId("01DSFGFVF3C1D1QQR69C7BRJT5"))
        }
    }

    @Test
    fun `Returns subsumsjon by result id if found`() {
        val id = ULID().nextULID()
        val subsumsjon =
            Subsumsjon(
                behovId = BehovId("01DSFSRMWGYP0AVHAHY282W3GN"),
                faktum = Faktum("aktorId", RegelKontekst("1", Kontekst.vedtak), LocalDate.now()),
                grunnlagResultat = emptyMap(),
                minsteinntektResultat = emptyMap(),
                periodeResultat = emptyMap(),
                satsResultat = emptyMap(),
                problem = Problem(title = "problem"),
            )

        val storeMock =
            mockk<SubsumsjonStore>(relaxed = false).apply {
                every { this@apply.getSubsumsjonByResult(SubsumsjonId(id)) } returns subsumsjon
            }

        withMockAuthServerAndTestApplication(
            mockApi(
                subsumsjonStore = storeMock,
            ),
        ) {
            handleAuthenticatedAzureAdRequest(HttpMethod.Get, "v1/subsumsjon/result/$id")
                .apply {
                    response.status() shouldBe HttpStatusCode.OK
                    response.headers["Content-Type"] shouldBe ContentType.Application.Json.toString()
                    response.content shouldNotBe null
                    response.content?.let {
                        JsonAdapter.fromJson(it) shouldBe subsumsjon
                    }
                }
        }

        verifyAll {
            storeMock.getSubsumsjonByResult(SubsumsjonId(id))
        }
    }

    @Test
    fun `Throws exception if not found`() {
        val storeMock = mockk<SubsumsjonStore>(relaxed = false)

        every { storeMock.getSubsumsjon(BehovId("01DSFGJBRYVBX2CNJKHJ0BB2W9")) } throws SubsumsjonNotFoundException("Not found")

        withMockAuthServerAndTestApplication(
            mockApi(
                subsumsjonStore = storeMock,
            ),
        ) {
            handleAuthenticatedAzureAdRequest(HttpMethod.Get, "v1/subsumsjon/01DSFGJBRYVBX2CNJKHJ0BB2W9").apply {
                response.status() shouldBe HttpStatusCode.NotFound
            }
        }

        verifyAll {
            storeMock.getSubsumsjon(BehovId("01DSFGJBRYVBX2CNJKHJ0BB2W9"))
        }
    }
}
