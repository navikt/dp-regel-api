package no.nav.dagpenger.regel.api.routing

import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.server.application.Application
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.security.mock.oauth2.MockOAuth2Server

internal object TestApplication {
    private const val ISSUER_ID = "default"

    val mockOAuth2Server: MockOAuth2Server by lazy {
        MockOAuth2Server().also {
            it.start()
        }
    }

    val testOAuthToken: String by lazy {
        mockOAuth2Server.issueToken(
            issuerId = ISSUER_ID,
        ).serialize()
    }

    internal fun testApp(
        moduleFunction: Application.() -> Unit,
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        try {
            System.setProperty("azure.app.well.known.url", mockOAuth2Server.wellKnownUrl(ISSUER_ID).toString())
            testApplication {
                application {
                    apply(moduleFunction)
                }
                createClient {
                    developmentMode = true
                    followRedirects = false
                }

                test()
            }
        } finally {
        }
    }

    internal suspend fun ApplicationTestBuilder.autentisert(
        endepunkt: String,
        httpMethod: HttpMethod = HttpMethod.Post,
        body: String? = null,
    ): HttpResponse =
        client.request(endepunkt) {
            this.method = httpMethod
            body?.let { this.setBody(TextContent(it, ContentType.Application.Json)) }
            this.header(HttpHeaders.Authorization, "Bearer $testOAuthToken")
            this.header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            this.header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }
}
