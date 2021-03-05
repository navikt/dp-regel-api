package no.nav.dagpenger.regel.api.routing

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import no.nav.dagpenger.regel.api.routing.TestApplication.withMockAuthServerAndTestApplication
import org.junit.jupiter.api.Test

class ActuatorRouteTest {

    @Test
    fun `isReady route returns 200 OK`() {
        withMockAuthServerAndTestApplication(MockApi()) {
            handleRequest(HttpMethod.Get, "/isReady").apply {
                response.status() shouldBe HttpStatusCode.OK
                response.content shouldBe "READY"
            }
        }
    }

    @Test
    fun `isAlive route returns 200 OK if all HealtChecks are up`() {

        val healthCheck = mockk<HealthCheck>().apply {
            every { this@apply.status() } returns HealthStatus.UP
        }

        withMockAuthServerAndTestApplication(
            MockApi(
                healthChecks = listOf(healthCheck, healthCheck)
            )
        ) {
            handleRequest(HttpMethod.Get, "/isAlive").apply {
                response.status() shouldBe HttpStatusCode.OK
                response.content shouldBe "ALIVE"
            }
        }

        verify(exactly = 2) {
            healthCheck.status()
        }
    }

    @Test
    fun `isAlive route returns 503 Not Available if a health check is down `() {

        val healthCheck = mockk<HealthCheck>().apply {
            every { this@apply.status() } returns HealthStatus.UP andThen HealthStatus.DOWN
        }

        withMockAuthServerAndTestApplication(
            MockApi(
                healthChecks = listOf(healthCheck, healthCheck)
            )
        ) {
            handleRequest(HttpMethod.Get, "/isAlive").apply {
                response.status() shouldBe HttpStatusCode.ServiceUnavailable
            }
        }

        verify(exactly = 2) {
            healthCheck.status()
        }
    }

    @Test
    fun `The application produces metrics`() {
        withMockAuthServerAndTestApplication(MockApi()) {
            handleRequest(HttpMethod.Get, "/metrics").run {
                response.status() shouldBe HttpStatusCode.OK
                response.content shouldContain "jvm_"
            }
        }
    }
}
