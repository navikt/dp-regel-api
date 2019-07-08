package no.nav.dagpenger.regel.api.routing

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
import no.nav.dagpenger.regel.api.db.BruktSubsumsjonStore
import no.nav.dagpenger.regel.api.db.StoreException
import no.nav.dagpenger.regel.api.db.SubsumsjonBrukt
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class BruktSubsumsjonsRouteTest {

    @Test
    fun `401 on unauthorized requests`() {
        withTestApplication(MockApi()) {
            handleRequest(HttpMethod.Post, "/subsumsjonbrukt").response.status() shouldBe HttpStatusCode.Unauthorized
            handleRequest(HttpMethod.Post, "/subsumsjonbrukt") { addHeader("X-API-KEY", "notvalid") }
                .response.status() shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `saves subsumsjon brukt`() {
        val savedToStore = slot<SubsumsjonBrukt>()
        val storeMock = mockk<BruktSubsumsjonStore>(relaxed = false).apply {
            every { this@apply.insertSubsumsjonBrukt(capture(savedToStore)) } returns 1
        }
        val now = Instant.now()
        val expected = SubsumsjonBrukt(
            id = "someid",
            eksternId = "arenaId",
            arenaTs = now.toString(),
            ts = now.toEpochMilli()
        )
        withTestApplication(MockApi(
            bruktSubsumsjonStore = storeMock
        )) {
            handleAuthenticatedRequest(HttpMethod.Post, "/subsumsjonbrukt") {
                addHeader(HttpHeaders.ContentType, "application/json")
                setBody("""{
                        "id": "someid",
                        "eksternId": "arenaId",
                        "arenaTs": "$now",
                        "ts": ${now.toEpochMilli()}
                    }
                """.trimIndent())
            }.apply {
                assertEquals(HttpStatusCode.Accepted, response.status())
                assertTrue(savedToStore.isCaptured)
                assertEquals(expected, savedToStore.captured)
            }
        }
    }

    @Test
    fun `returns server error if we cannot save object`() {
        val storeMock = mockk<BruktSubsumsjonStore>(relaxed = false).apply {
            every { this@apply.insertSubsumsjonBrukt(any()) } throws StoreException("Failed")
        }
        val now = Instant.now()
        withTestApplication(MockApi(
            bruktSubsumsjonStore = storeMock
        )) {
            handleAuthenticatedRequest(HttpMethod.Post, "/subsumsjonbrukt") {
                addHeader(HttpHeaders.ContentType, "application/json")
                setBody("""{
                        "id": "someid",
                        "eksternId": "arenaId",
                        "arenaTs": "$now",
                        "ts": ${now.toEpochMilli()}
                    }
                """.trimIndent())
            }.apply {
                assertEquals(HttpStatusCode.InternalServerError, response.status())
            }
        }
    }
}