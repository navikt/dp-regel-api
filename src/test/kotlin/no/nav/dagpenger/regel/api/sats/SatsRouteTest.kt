package no.nav.dagpenger.regel.api.sats

import com.squareup.moshi.JsonEncodingException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import no.nav.dagpenger.regel.api.api
import no.nav.dagpenger.regel.api.dummyApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertTrue

val validJson = """
{
	"aktorId": "9000000028204",
    "vedtakId": 1,
    "beregningsdato": "2019-01-08"
}
""".trimIndent()

val jsonMissingFields = """
{
	"aktorId": "9000000028204",
}
""".trimIndent()
class SatsRouteTest {

    @Test
    fun `post request with good json`() = testApp {
        handleRequest(HttpMethod.Post, "/sats") {
            addHeader(HttpHeaders.ContentType, "application/json")
            setBody(validJson)
        }.apply {
            assertTrue(requestHandled)
            assertEquals(HttpStatusCode.Accepted, response.status())
            assertTrue(response.headers.contains(HttpHeaders.Location))
        }
    }

    @Test
    fun `post request with bad json`() {
        assertThrows<JsonEncodingException> {
            testApp {
                handleRequest(HttpMethod.Post, "/sats") {
                    addHeader(HttpHeaders.ContentType, "application/json")
                    setBody(jsonMissingFields)
                }
            }
        }
    }

    private fun testApp(callback: TestApplicationEngine.() -> Unit) {
        withTestApplication({ dummyApi() }) { callback() }
    }
}
