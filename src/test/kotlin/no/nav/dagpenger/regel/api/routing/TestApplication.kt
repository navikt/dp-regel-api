package no.nav.dagpenger.regel.api.routing

import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import no.nav.security.mock.oauth2.MockOAuth2Server

internal object TestApplication {
    private const val ISSUER_ID = "default"

    val mockOAuth2Server: MockOAuth2Server by lazy {
        MockOAuth2Server().also { it.start() }
    }

    val testOAuthToken: String by lazy { mockOAuth2Server.issueToken(ISSUER_ID).serialize() }

    internal fun <R> withMockAuthServerAndTestApplication(
        moduleFunction: Application.() -> Unit,
        test: TestApplicationEngine.() -> R
    ): R {
        try {
            System.setProperty("azure.app.well.known.url", mockOAuth2Server.wellKnownUrl(ISSUER_ID).toString())
            return withTestApplication(moduleFunction, test)
        } finally {
        }
    }

    internal fun TestApplicationEngine.handleAuthenticatedAzureAdRequest(
        method: HttpMethod,
        uri: String,
        test: TestApplicationRequest.() -> Unit = {}
    ): TestApplicationCall {
        return this.handleRequest(method, uri) {
            addHeader("Authorization", "Bearer $testOAuthToken")
            test()
        }
    }
}
