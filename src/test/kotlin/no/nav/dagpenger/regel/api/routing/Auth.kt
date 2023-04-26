package no.nav.dagpenger.regel.api.routing

import io.ktor.http.HttpMethod
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.handleRequest
import no.nav.dagpenger.inntekt.ApiKeyVerifier
import no.nav.dagpenger.regel.api.Configuration

private val auth = Configuration().auth

private val apiKey by lazy {
    val keyVerifier = ApiKeyVerifier(auth.secret)
    return@lazy keyVerifier.generate(auth.allowedKeys.first())
}

internal val authApiKeyVerifier = auth.authApiKeyVerifier

internal fun TestApplicationEngine.handleAuthenticatedRequest(
    method: HttpMethod,
    uri: String,
    setup: TestApplicationRequest.() -> Unit = {}
): TestApplicationCall {
    return this.handleRequest(method, uri) {
        addHeader("X-API-KEY", apiKey)
        setup()
    }
}
