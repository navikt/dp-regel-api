package no.nav.dagpenger.regel.api.minsteinntekt

import com.google.gson.JsonSyntaxException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import no.nav.dagpenger.regel.api.api
import no.nav.dagpenger.regel.api.grunnlag.GrunnlagBeregninger
import no.nav.dagpenger.regel.api.tasks.Tasks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertTrue

val validJson = """
{
  "aktorId": "string",
  "beregningsdato": "string",
  "bruktinntektsPeriode": {
    "foersteMaaned": "string",
    "sisteMaaned": "string"
  },
  "harArbeidsperiodeEosSiste12Maaneder": true,
  "harAvtjentVerneplikt": true,
  "inntektsId": "string",
  "oppfyllerKravTilFangstOgFisk": true,
  "vedtakId": 0
}
""".trimIndent()

val jsonMissingFields = """
{
  "aktorId": "string",
  "beregningsdato": "string",
  "bruktinntektsPeriode": {
    "foersteMaaned": "string",
    "sisteMaaned": "string"
  },
  "harArbeidsperiodeEosSiste12Maaneder": true,
  "inntektsId": "string",
  "oppfyllerKravTilFangstOgFisk": true,
}
""".trimIndent()
class MinsteinntektRouteTest {

    @Test
    fun `post request with good json`() = testApp {
        handleRequest(HttpMethod.Post, "/minsteinntekt") {
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
        assertThrows<JsonSyntaxException> {
            testApp {
                handleRequest(HttpMethod.Post, "/minsteinntekt") {
                    addHeader(HttpHeaders.ContentType, "application/json")
                    setBody(jsonMissingFields)
                }.apply {
                    assertEquals(HttpStatusCode.BadRequest, response.status())
                }
            }
        }
    }

    private fun testApp(callback: TestApplicationEngine.() -> Unit) {
        withTestApplication({ api(Tasks(), MinsteinntektBeregninger(), GrunnlagBeregninger()) }) { callback() }
    }
}
