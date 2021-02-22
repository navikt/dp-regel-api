package no.nav.dagpenger.regel.api.auth

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.auth.jwt.JWTAuthenticationProvider
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.client.HttpClient
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.http
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import java.net.URL
import java.util.concurrent.TimeUnit

internal fun JWTAuthenticationProvider.Configuration.azureAdJWT(
    providerUrl: String,
    realm: String,
    clientId: String
) {
    this.verifier(jwkProvider(providerUrl), issuerProvider(providerUrl))
    this.realm = realm
    validate { credentials ->
        try {
            requireNotNull(credentials.payload.audience) {
                "Auth: Missing audience in token"
            }
            require(credentials.payload.audience.contains(clientId)) {
                "Auth: Valid audience not found in claims"
            }
            JWTPrincipal(credentials.payload)
        } catch (e: Throwable) {
            null
        }
    }
}

private val httpClient = HttpClient(CIO) {
    install(JsonFeature) {
        serializer = JacksonSerializer {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }
    engine {
        System.getenv("HTTP_PROXY")?.let {
            this.proxy = ProxyBuilder.http(it)
        }
    }
}

internal data class AzureAdOpenIdConfiguration(
    @JsonProperty("jwks_uri")
    val jwksUri: String,
    @JsonProperty("issuer")
    val issuer: String,
    @JsonProperty("token_endpoint")
    val tokenEndpoint: String,
    @JsonProperty("authorization_endpoint")
    val authorizationEndpoint: String
)

internal fun issuerProvider(url: String): String {
    return runBlocking {
        println(url)
        httpClient.get<AzureAdOpenIdConfiguration>(url).issuer
    }
}

internal fun jwkProvider(url: String): JwkProvider {
    return JwkProviderBuilder(URL(url))
        .cached(10, 24, TimeUnit.HOURS) // cache up to 10 JWKs for 24 hours
        .rateLimited(
            10,
            1,
            TimeUnit.MINUTES
        ) // if not cached, only allow max 10 different keys per minute to be fetched from external provider
        .build()
}
