package no.nav.dagpenger.regel.api.routing

import de.huxhorn.sulky.ulid.ULID
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
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.regel.api.db.SubsumsjonNotFoundException
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.BehovId
import no.nav.dagpenger.regel.api.models.Faktum
import no.nav.dagpenger.regel.api.models.Subsumsjon
import no.nav.dagpenger.regel.api.models.SubsumsjonId
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SubsumsjonRouteTest {

    @Test
    fun `401 on unauthorized requests`() {
        withTestApplication(MockApi()) {
            handleRequest(HttpMethod.Get, "subsumsjon/id").response.status() shouldBe HttpStatusCode.Unauthorized
            handleRequest(HttpMethod.Get, "subsumsjon/id") { addHeader("X-API-KEY", "notvalid") }
                .response.status() shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Returns subsumsjon if found`() {
        val subsumsjon = Subsumsjon(
            behovId = BehovId("01DSFSSNA8S577XGQ8V1R9EBJ7"),
            faktum = Faktum("aktorId", 1, LocalDate.now()),
            grunnlagResultat = emptyMap(),
            minsteinntektResultat = emptyMap(),
            periodeResultat = emptyMap(),
            satsResultat = emptyMap(),
            problem = Problem(title = "problem")
        )

        val storeMock = mockk<SubsumsjonStore>(relaxed = false).apply {
            every { this@apply.getSubsumsjon(SubsumsjonId("01DSFGFVF3C1D1QQR69C7BRJT5")) } returns subsumsjon
        }

        withTestApplication(MockApi(
            subsumsjonStore = storeMock
        )) {

            handleAuthenticatedRequest(HttpMethod.Get, "/subsumsjon/01DSFGFVF3C1D1QQR69C7BRJT5")
                .apply {
                    response.status() shouldBe HttpStatusCode.OK
                    response.content shouldNotBe null
                    response.content?.let {
                        Subsumsjon.fromJson(it) shouldBe subsumsjon
                    }
                }
        }

        verifyAll {
            storeMock.getSubsumsjon(SubsumsjonId("01DSFGFVF3C1D1QQR69C7BRJT5"))
        }
    }

    @Test
    fun `Returns subsumsjon by result id if found`() {
        val id = ULID().nextULID()
        val subsumsjon = Subsumsjon(
            behovId = BehovId("01DSFSRMWGYP0AVHAHY282W3GN"),
            faktum = Faktum("aktorId", 1, LocalDate.now()),
            grunnlagResultat = emptyMap(),
            minsteinntektResultat = emptyMap(),
            periodeResultat = emptyMap(),
            satsResultat = emptyMap(),
            problem = Problem(title = "problem")
        )

        val storeMock = mockk<SubsumsjonStore>(relaxed = false).apply {
            every { this@apply.getSubsumsjonByResult(SubsumsjonId(id)) } returns subsumsjon
        }

        withTestApplication(MockApi(
            subsumsjonStore = storeMock
        )) {

            handleAuthenticatedRequest(HttpMethod.Get, "/subsumsjon/result/$id")
                .apply {
                    response.status() shouldBe HttpStatusCode.OK
                    response.content shouldNotBe null
                    response.content?.let {
                        Subsumsjon.fromJson(it) shouldBe subsumsjon
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

        every { storeMock.getSubsumsjon(SubsumsjonId("01DSFGJBRYVBX2CNJKHJ0BB2W9")) } throws SubsumsjonNotFoundException("Not found")

        withTestApplication(MockApi(
            subsumsjonStore = storeMock
        )) {
            shouldThrow<SubsumsjonNotFoundException> {
                handleAuthenticatedRequest(HttpMethod.Get, "/subsumsjon/01DSFGJBRYVBX2CNJKHJ0BB2W9")
            }
        }

        verifyAll {
            storeMock.getSubsumsjon(SubsumsjonId("01DSFGJBRYVBX2CNJKHJ0BB2W9"))
        }
    }
}