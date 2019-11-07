package no.nav.dagpenger.regel.api.routing

import io.kotlintest.shouldBe
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.mockk.mockk

import org.junit.jupiter.api.Test

class V2BehovRouteTest {

    @Test
    fun `401 on unauthorized requests`() {
        withTestApplication(MockApi()) {
            // handleRequest(HttpMethod.Get, "/v2/behov/status/id").response.status() shouldBe HttpStatusCode.Unauthorized
            handleRequest(HttpMethod.Post, "/v2/behov").response.status() shouldBe HttpStatusCode.Unauthorized
            handleRequest(HttpMethod.Post, "/v2/behov") { addHeader("X-API-KEY", "notvalid") }
                    .response.status() shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Status when behov is done, pending or not found`() {

        withTestApplication(MockApi(
                subsumsjonStore = mockk()
        )) {

            handleAuthenticatedRequest(HttpMethod.Post, "/v2/behov")
                    .apply {
                        response.status() shouldBe HttpStatusCode.Accepted
                    }
        }
    }
}