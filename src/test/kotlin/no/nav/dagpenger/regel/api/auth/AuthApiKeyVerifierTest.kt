package no.nav.dagpenger.regel.api.auth

import no.nav.dagpenger.ktor.auth.ApiKeyCredential
import no.nav.dagpenger.ktor.auth.ApiKeyVerifier
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class AuthApiKeyVerifierTest {

    @Test
    fun verify() {
        val verifier = AuthApiKeyVerifier("secret", listOf("key1", "key2"))

        assertNull(verifier.verify(ApiKeyCredential("key1")))
        assertNotNull(verifier.verify(ApiKeyCredential(ApiKeyVerifier("secret").generate("key1"))))
        assertNotNull(verifier.verify(ApiKeyCredential(ApiKeyVerifier("secret").generate("key2"))))
    }
}