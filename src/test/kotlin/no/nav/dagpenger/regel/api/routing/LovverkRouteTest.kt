package no.nav.dagpenger.regel.api.routing

import de.huxhorn.sulky.ulid.ULID
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.BehovId
import no.nav.dagpenger.regel.api.models.Faktum
import no.nav.dagpenger.regel.api.models.InternBehov
import no.nav.dagpenger.regel.api.models.Status
import no.nav.dagpenger.regel.api.models.Subsumsjon
import no.nav.dagpenger.regel.api.models.SubsumsjonId
import no.nav.dagpenger.regel.api.streams.DagpengerBehovProducer
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LovverkRouteTest {
    val subsumsjonMock = Subsumsjon(
        behovId = BehovId("01DSFSRMWGYP0AVHAHY282W3GN"),
        faktum = Faktum("aktorId", 1, LocalDate.now()),
        grunnlagResultat = emptyMap(),
        minsteinntektResultat = mapOf("oppfyllerMinsteinntekt" to true),
        periodeResultat = emptyMap(),
        satsResultat = emptyMap(),
        problem = null
    )

    @Test
    fun `401 on unauthorized requests`() {
        withTestApplication(MockApi()) {
            handleRequest(
                HttpMethod.Post,
                "lovverk/krever-ny-behandling"
            ).response.status() shouldBe HttpStatusCode.Unauthorized
            handleRequest(HttpMethod.Post, "lovverk/krever-ny-behandling") { addHeader("X-API-KEY", "notvalid") }
                .response.status() shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `må ikke reberegnes når begge har samme resultat på oppfyllerKravTilMinsteinntekt`() {

        val subsumsjonStore = mockk<SubsumsjonStore>().apply {
            every { behovStatus(any()) } returns Status.Done(BehovId(ULID().nextULID()))
            every { getSubsumsjonByResult(any()) } returns subsumsjonMock
            every { getSubsumsjon(any()) } returns subsumsjonMock
            every { getBehov(any()) } returns InternBehov(aktørId = "abc", behandlingsId = mockk(), beregningsDato = LocalDate.of(2020, 1, 13))
        }
        val behovProducer = mockk<DagpengerBehovProducer>(relaxed = true)
        withTestApplication(MockApi(subsumsjonStore = subsumsjonStore, kafkaDagpengerBehovProducer = behovProducer)) {
            handleAuthenticatedRequest(HttpMethod.Post, "/lovverk/krever-ny-behandling") {
                addHeader(HttpHeaders.ContentType, "application/json")
                setBody(
                    """
{
    "dato": "2020-01-13",
    "subsumsjoner": ["$subsumsjonId1", "$subsumsjonId2"]
}
                    """.trimIndent()
                )
            }
                .apply {
                    response.status() shouldBe HttpStatusCode.OK
                    withClue("Response should be handled") { requestHandled shouldBe true }
                    response.content shouldBe """{"reberegnes":false}"""
                    verify { subsumsjonStore.getSubsumsjonByResult(SubsumsjonId(subsumsjonId1)) }
                    verify { subsumsjonStore.getSubsumsjonByResult(SubsumsjonId(subsumsjonId2)) }
                }
        }
    }

    @Test
    fun `må reberegnes når subsumsjoner har ulike resultat på oppfyllerKravTilMinsteinntekt`() {
        val subsumsjonStore = mockk<SubsumsjonStore>().apply {
            every { behovStatus(any()) } returns Status.Done(BehovId(ULID().nextULID()))
            every { getSubsumsjonByResult(any()) } returns subsumsjonMock
            every { getSubsumsjon(any()) } returns subsumsjonMock.copy(minsteinntektResultat = mapOf("oppfyllerMinsteinntekt" to false))
            every { getBehov(any()) } returns InternBehov(aktørId = "abc", behandlingsId = mockk(), beregningsDato = LocalDate.of(2020, 1, 13))
        }
        val behovProducer = mockk<DagpengerBehovProducer>(relaxed = true)
        withTestApplication(MockApi(subsumsjonStore = subsumsjonStore, kafkaDagpengerBehovProducer = behovProducer)) {
            handleAuthenticatedRequest(HttpMethod.Post, "/lovverk/krever-ny-behandling") {
                addHeader(HttpHeaders.ContentType, "application/json")
                setBody(
                    """
{
    "dato": "2020-01-13",
    "subsumsjoner": ["$subsumsjonId1", "$subsumsjonId2"]
}
                    """.trimIndent()
                )
            }
                .apply {
                    response.status() shouldBe HttpStatusCode.OK
                    withClue("Response should be handled") { requestHandled shouldBe true }
                    response.content shouldBe """{"reberegnes":true}"""
                    verify { subsumsjonStore.getSubsumsjonByResult(SubsumsjonId(subsumsjonId1)) }
                }
        }
    }

    companion object {
        val subsumsjonId1 = ULID().nextULID()
        val subsumsjonId2 = ULID().nextULID()
    }
}
