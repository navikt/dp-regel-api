package no.nav.dagpenger.regel.api.routing

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import no.nav.dagpenger.regel.api.routing.TestApplication.handleAuthenticatedAzureAdRequest
import no.nav.dagpenger.regel.api.routing.TestApplication.withMockAuthServerAndTestApplication
import org.junit.jupiter.api.Test

internal class ProtectedRouteTest {

    @Test
    fun `401 on unauthorized requests`() {
        withMockAuthServerAndTestApplication(MockApi()) {
            handleRequest(
                HttpMethod.Get,
                "/secured"
            ).response.status() shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `200 on authorized requests`() {
        withMockAuthServerAndTestApplication(MockApi()) {
            handleRequest(
                HttpMethod.Get,
                "/secured"
            ) {
                addHeader("Authorization", "Bearer ${TestApplication.testOAuthToken}")
            }.response.status() shouldBe HttpStatusCode.OK
        }
    }

    @Test
    fun `200 on authorized requests with helper`() {
        withMockAuthServerAndTestApplication(MockApi()) {
            handleAuthenticatedAzureAdRequest(
                HttpMethod.Get,
                "/secured"
            ).response.status() shouldBe HttpStatusCode.OK
        }
    }
}
