package no.nav.dagpenger.regel.api.tasks

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import no.nav.dagpenger.regel.api.Regel
import no.nav.dagpenger.regel.api.dummyApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertTrue

class TasksRouteTest {

    @Test
    fun `get request for done task`() = testApp {
        val taskId = createTaskId(Regel.MINSTEINNTEKT, TasksDummy.minsteinntektDoneBehovId)
        handleRequest(HttpMethod.Get, "/task/$taskId").apply {
            assertTrue(requestHandled)
            assertEquals(HttpStatusCode.SeeOther, response.status())
            assertTrue(response.headers.contains(HttpHeaders.Location))
        }
    }

    @Test
    fun `get request for pending task`() = testApp {
        val taskId = createTaskId(Regel.MINSTEINNTEKT, TasksDummy.minsteinntektPendingBehovId)
        handleRequest(HttpMethod.Get, "/task/$taskId").apply {
            assertTrue(requestHandled)
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    @Test
    fun `get request for unknown task`() = testApp {
        assertThrows<TaskNotFoundException> {
            handleRequest(HttpMethod.Get, "/task/unkown!")
        }
    }

    private fun testApp(callback: TestApplicationEngine.() -> Unit) {
        withTestApplication({ dummyApi() }) { callback() }
    }
}