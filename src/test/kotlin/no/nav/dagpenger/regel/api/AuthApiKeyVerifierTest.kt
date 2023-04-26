package no.nav.dagpenger.inntekt

import no.nav.dagpenger.regel.api.AuthApiKeyVerifier
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class AuthApiKeyVerifierTest {

    @Test
    fun `Verify known clients `() {

        val apiKeyVerifier = ApiKeyVerifier("secret")

        val clientVerifier = AuthApiKeyVerifier(apiKeyVerifier, listOf("client1-key", "client2-key"))

        assertTrue { clientVerifier.verify(apiKeyVerifier.generate("client1-key")) }
        assertTrue { clientVerifier.verify(apiKeyVerifier.generate("client2-key")) }
    }

    @Test
    fun `Do not verify unknown clients `() {

        val apiKeyVerifier = ApiKeyVerifier("secret")

        val clientVerifier = AuthApiKeyVerifier(apiKeyVerifier, listOf("client3-key", "client4-key"))

        assertFalse { clientVerifier.verify(apiKeyVerifier.generate("client1-key")) }
        assertFalse { clientVerifier.verify(apiKeyVerifier.generate("client2-key")) }
    }

    @Test
    fun `Do not verify  clients with another secret `() {

        val apiKeyVerifier = ApiKeyVerifier("secret")
        val anotherKeyVerifier = ApiKeyVerifier("anohter-secret")

        val clientVerifier = AuthApiKeyVerifier(apiKeyVerifier, listOf("client1-key"))

        assertFalse { clientVerifier.verify(anotherKeyVerifier.generate("client1-key")) }
    }
}
