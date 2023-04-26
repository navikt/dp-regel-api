package no.nav.dagpenger.regel.api.auth

import no.nav.dagpenger.inntekt.ApiKeyVerifier
import no.nav.dagpenger.regel.api.AuthApiKeyVerifier
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class AuthApiKeyVerifierTest {

    @Test
    fun verify() {
        val verifier = AuthApiKeyVerifier(ApiKeyVerifier("secret"), listOf("key1", "key2"))

        assertFalse(verifier.verify("key1"))
        assertNotNull(verifier.verify(ApiKeyVerifier("secret").generate("key1")))
        assertNotNull(verifier.verify(ApiKeyVerifier("secret").generate("key2")))
    }
}
