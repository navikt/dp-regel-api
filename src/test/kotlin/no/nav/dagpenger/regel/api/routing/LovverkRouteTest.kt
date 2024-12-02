package no.nav.dagpenger.regel.api.routing

import de.huxhorn.sulky.ulid.ULID
import io.kotest.matchers.shouldBe
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.BehovId
import no.nav.dagpenger.regel.api.models.Faktum
import no.nav.dagpenger.regel.api.models.InternBehov
import no.nav.dagpenger.regel.api.models.Kontekst
import no.nav.dagpenger.regel.api.models.RegelKontekst
import no.nav.dagpenger.regel.api.models.Status
import no.nav.dagpenger.regel.api.models.Subsumsjon
import no.nav.dagpenger.regel.api.models.SubsumsjonId
import no.nav.dagpenger.regel.api.routing.TestApplication.autentisert
import no.nav.dagpenger.regel.api.routing.TestApplication.testApp
import no.nav.dagpenger.regel.api.streams.DagpengerBehovProducer
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LovverkRouteTest {
    val subsumsjonMock =
        Subsumsjon(
            behovId = BehovId("01DSFSRMWGYP0AVHAHY282W3GN"),
            faktum = Faktum("aktorId", RegelKontekst("1", Kontekst.vedtak), LocalDate.now()),
            grunnlagResultat = emptyMap(),
            minsteinntektResultat = mapOf("oppfyllerMinsteinntekt" to true),
            periodeResultat = emptyMap(),
            satsResultat = emptyMap(),
            problem = null,
        )

    val behov =
        InternBehov(
            aktørId = "abc",
            behandlingsId = mockk(),
            beregningsDato = LocalDate.of(2020, 1, 13),
        )

    val subsumsjonStore =
        mockk<SubsumsjonStore>().apply {
            every { behovStatus(any()) } returns Status.Done(BehovId(ULID().nextULID()))
            every { getSubsumsjonerByResults(any()) } returns listOf(subsumsjonMock)
            every { opprettBehov(any()) } returns behov
            every { getBehov(any()) } returns behov
        }

    private val behovProducer = mockk<DagpengerBehovProducer>(relaxed = true)

    @Test
    fun `401 on unauthorized requests`() {
        testApp(
            mockApi(),
        ) {
            val response = client.post("lovverk/vurdering/minsteinntekt")
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `må ikke reberegnes når begge har samme resultat på oppfyllerMinsteinntekt`() {
        subsumsjonStore.apply {
            every { getSubsumsjon(any()) } returns subsumsjonMock
        }
        testApp(
            mockApi(
                subsumsjonStore = subsumsjonStore,
                kafkaDagpengerBehovProducer = behovProducer,
            ),
        ) {
            val response = autentisert("lovverk/vurdering/minsteinntekt", HttpMethod.Post, body = jsonRequestBody)
            response.status shouldBe HttpStatusCode.OK
            response.headers["Content-Type"] shouldBe
                ContentType.Application.Json.withParameter("charset", "UTF-8")
                    .toString()

            response.bodyAsText() shouldBe """{"nyVurdering":false}"""
            verify {
                subsumsjonStore.getSubsumsjonerByResults(
                    listOf(
                        SubsumsjonId(subsumsjonId1),
                        SubsumsjonId(subsumsjonId2),
                    ),
                )
            }
        }
    }

    @Test
    fun `må reberegnes når subsumsjoner har ulike resultat på oppfyllerMinsteinntekt`() {
        subsumsjonStore.apply {
            every { getSubsumsjon(any()) } returns
                subsumsjonMock.copy(minsteinntektResultat = mapOf("oppfyllerMinsteinntekt" to false))
        }

        testApp(
            mockApi(
                subsumsjonStore = subsumsjonStore,
                kafkaDagpengerBehovProducer = behovProducer,
            ),
        ) {
            val response = autentisert("lovverk/vurdering/minsteinntekt", HttpMethod.Post, body = jsonRequestBody)
            response.status shouldBe HttpStatusCode.OK
            response.headers["Content-Type"] shouldBe
                ContentType.Application.Json.withParameter("charset", "UTF-8")
                    .toString()

            response.bodyAsText() shouldBe """{"nyVurdering":true}"""
            verify {
                subsumsjonStore.getSubsumsjonerByResults(
                    listOf(
                        SubsumsjonId(subsumsjonId1),
                        SubsumsjonId(subsumsjonId2),
                    ),
                )
            }
        }
    }

    companion object {
        val subsumsjonId1 = ULID().nextULID()
        val subsumsjonId2 = ULID().nextULID()
    }

    val jsonRequestBody =
        """
        {
        "beregningsdato": "2020-01-13",
        "subsumsjonIder": ["$subsumsjonId1", "$subsumsjonId2"]
         }
        """.trimIndent()
}
