package no.nav.dagpenger.inntekt

import io.ktor.http.auth.HeaderValueEncoding
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationFailedCause
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.auth.Credential
import io.ktor.server.auth.Principal
import io.ktor.server.auth.UnauthorizedResponse
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.response.respond
import org.apache.commons.codec.binary.Hex
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

enum class ApiKeyLocation(val location: String) {
    QUERY("query"),
    HEADER("header")
}

fun ApplicationRequest.apiKeyAuthenticationCredentials(
    apiKeyName: String,
    apiKeyLocation: ApiKeyLocation
): ApiKeyCredential? {
    return when (
        val value: String? = when (apiKeyLocation) {
            ApiKeyLocation.QUERY -> this.queryParameters[apiKeyName]
            ApiKeyLocation.HEADER -> this.headers[apiKeyName]
        }
    ) {
        null -> null
        else -> ApiKeyCredential(value)
    }
}

data class ApiKeyCredential(val value: String) : Credential
data class ApiPrincipal(val apiKeyCredential: ApiKeyCredential?) : Principal

class ApiKeyAuthenticationProvider internal constructor(config: Configuration) : AuthenticationProvider(config) {

    private var apiKeyName: String = config.apiKeyName
    private var apiKeyLocation: ApiKeyLocation = config.apiKeyLocation
    private val authenticationFunction = config.authenticationFunction

    class Configuration(name: String?) : Config(name) {
        internal var authenticationFunction: suspend ApplicationCall.(ApiKeyCredential) -> Principal? = { null }

        var apiKeyName: String = ""

        var apiKeyLocation: ApiKeyLocation = ApiKeyLocation.HEADER

        fun validate(body: suspend ApplicationCall.(ApiKeyCredential) -> Principal?) {
            authenticationFunction = body
        }

        internal fun build() = ApiKeyAuthenticationProvider(this)
    }

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val credential = call.request.apiKeyAuthenticationCredentials(apiKeyName, apiKeyLocation)
        val principal = credential?.let { authenticationFunction(call, it) }

        val cause = when {
            credential == null -> AuthenticationFailedCause.NoCredentials
            principal == null -> AuthenticationFailedCause.InvalidCredentials
            else -> null
        }

        if (cause != null) {
            context.challenge(apiKeyName, cause) { challenge, call ->
                call.respond(
                    UnauthorizedResponse(
                        HttpAuthHeader.Parameterized(
                            "API_KEY",
                            mapOf("key" to apiKeyName),
                            HeaderValueEncoding.QUOTED_ALWAYS
                        )
                    )
                )
                challenge.complete()
            }
        }

        if (principal != null) {
            context.principal(principal)
        }
    }
}

fun AuthenticationConfig.apiKeyAuth(
    name: String? = null,
    configure: ApiKeyAuthenticationProvider.Configuration.() -> Unit
) {
    val provider = ApiKeyAuthenticationProvider.Configuration(name).apply(configure).build()
    register(provider)
}

internal class ApiKeyVerifier(private val secret: String) {

    private val algorithm = "HmacSHA256"

    fun verify(apiKey: String, expectedApiKey: String): Boolean {
        return apiKey == generate(expectedApiKey)
    }

    fun generate(apiKey: String): String {
        return String(Hex.encodeHex(generateDigest(apiKey.toByteArray(StandardCharsets.UTF_8))))
    }

    private fun generateDigest(apiKey: ByteArray): ByteArray {
        val secret = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), algorithm)
        val mac = Mac.getInstance(algorithm)
        mac.init(secret)
        return mac.doFinal(apiKey)
    }
}
