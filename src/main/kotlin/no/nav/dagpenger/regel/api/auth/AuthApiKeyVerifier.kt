package no.nav.dagpenger.regel.api.auth

import io.ktor.auth.Principal
import no.nav.dagpenger.ktor.auth.ApiKeyCredential
import no.nav.dagpenger.ktor.auth.ApiKeyVerifier
import no.nav.dagpenger.ktor.auth.ApiPrincipal

internal class AuthApiKeyVerifier(secret: String, private val allowedKeys: List<String>) {
    private val verifier = ApiKeyVerifier(secret)
    fun verify(credential: ApiKeyCredential): Principal? =
        allowedKeys.map { verifier.verify(credential.value, it) }.firstOrNull { it }?.let { ApiPrincipal(credential) }
}
