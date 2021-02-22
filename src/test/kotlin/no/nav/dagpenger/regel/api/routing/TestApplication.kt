package no.nav.dagpenger.regel.api.routing

import io.ktor.application.Application
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.withTestApplication
import no.nav.security.mock.oauth2.MockOAuth2Server

internal object TestApplication {
    private val server by lazy {
        MockOAuth2Server().also { it.start() }
    }

    internal fun <R> withMockAuthServerAndTestApplication(
        moduleFunction: Application.() -> Unit,
        test: TestApplicationEngine.() -> R
    ): R {
        try {
            System.setProperty("azure.app.well.known.url", server.wellKnownUrl("default").toString())
            return withTestApplication(moduleFunction, test)
        } finally {
        }
    }
}
