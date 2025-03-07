package no.nav.dagpenger.regel.api.routing

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import no.nav.dagpenger.regel.api.routing.TestApplication.testApp
import org.junit.jupiter.api.Test

class ActuatorRouteTest {
    @Test
    fun `isReady route returns 200 OK`() {
        testApp(
            mockApi(),
        ) {
            val res = client.get("/isReady")
            res.status shouldBe HttpStatusCode.OK
            res.bodyAsText() shouldBe "READY"
        }
    }

    @Test
    fun `isAlive route returns 200 OK if all HealtChecks are up`() {
        val healthCheck =
            mockk<HealthCheck>().apply {
                every { this@apply.status() } returns HealthStatus.UP
            }

        testApp(
            mockApi(
                healthChecks = listOf(healthCheck, healthCheck),
            ),
        ) {
            val res = client.get("/isAlive")
            res.status shouldBe HttpStatusCode.OK
            res.bodyAsText() shouldBe "ALIVE"
        }

        verify(exactly = 2) {
            healthCheck.status()
        }
    }

    @Test
    fun `isAlive route returns 503 Not Available if a health check is down `() {
        val healthCheck =
            mockk<HealthCheck>().apply {
                every { this@apply.status() } returns HealthStatus.UP andThen HealthStatus.DOWN
            }

        testApp(
            mockApi(
                healthChecks = listOf(healthCheck, healthCheck),
            ),
        ) {
            val res = client.get("/isAlive")
            res.status shouldBe HttpStatusCode.ServiceUnavailable
        }

        verify(exactly = 2) {
            healthCheck.status()
        }
    }

    @Test
    fun `The application produces metrics`() {
        testApp(
            mockApi(),
        ) {
            val res = client.get("/metrics")
            res.status shouldBe HttpStatusCode.OK
            val result = res.bodyAsText()
            result shouldContain "jvm_"
        }
    }
}
