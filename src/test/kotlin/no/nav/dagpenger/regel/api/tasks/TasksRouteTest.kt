package no.nav.dagpenger.regel.api.tasks

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import no.nav.dagpenger.regel.api.DagpengerBehovProducerDummy
import no.nav.dagpenger.regel.api.api
import no.nav.dagpenger.regel.api.grunnlag.DagpengegrunnlagBeregningerDummy
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektBeregningerDummy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class TasksRouteTest {

    @Test
    fun `get request for done task`() = testApp {
        handleRequest(HttpMethod.Get, "/task/${TasksDummy.minsteinntektDoneTaskId}").apply {
            assertTrue(requestHandled)
            assertEquals(HttpStatusCode.SeeOther, response.status())
            assertTrue(response.headers.contains(HttpHeaders.Location))
        }
    }

    @Test
    fun `get request for pending task`() = testApp {
        handleRequest(HttpMethod.Get, "/task/${TasksDummy.minsteinntektPendingTaskId}").apply {
            assertTrue(requestHandled)
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    @Test
    fun `get request for unknown task`() = testApp {
        handleRequest(HttpMethod.Get, "/task/unkown!").apply {
            assertTrue(requestHandled)
            assertEquals(HttpStatusCode.NotFound, response.status())
        }
    }

    private fun testApp(callback: TestApplicationEngine.() -> Unit) {
        withTestApplication({ api(
            TasksDummy(),
            MinsteinntektBeregningerDummy(),
            DagpengegrunnlagBeregningerDummy(),
            DagpengerBehovProducerDummy())
        }) { callback() }
    }
}