package no.nav.dagpenger.regel.api

import com.fasterxml.jackson.core.JacksonException
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.embeddedServer
import io.ktor.server.locations.Locations
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import mu.KotlinLogging
import no.nav.dagpenger.inntekt.ApiKeyCredential
import no.nav.dagpenger.inntekt.ApiKeyVerifier
import no.nav.dagpenger.inntekt.ApiPrincipal
import no.nav.dagpenger.inntekt.apiKeyAuth
import no.nav.dagpenger.regel.api.Vaktmester.Companion.LOGGER
import no.nav.dagpenger.regel.api.auth.azureAdJWT
import no.nav.dagpenger.regel.api.db.BehovNotFoundException
import no.nav.dagpenger.regel.api.db.PostgresBruktSubsumsjonStore
import no.nav.dagpenger.regel.api.db.PostgresSubsumsjonStore
import no.nav.dagpenger.regel.api.db.SubsumsjonNotFoundException
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.db.dataSourceFrom
import no.nav.dagpenger.regel.api.db.migrate
import no.nav.dagpenger.regel.api.models.IllegalUlidException
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.routing.behov
import no.nav.dagpenger.regel.api.routing.lovverk
import no.nav.dagpenger.regel.api.routing.metrics
import no.nav.dagpenger.regel.api.routing.naischecks
import no.nav.dagpenger.regel.api.routing.subsumsjon
import no.nav.dagpenger.regel.api.serder.jacksonObjectMapper
import no.nav.dagpenger.regel.api.streams.AivenKafkaSubsumsjonConsumer
import no.nav.dagpenger.regel.api.streams.BruktSubsumsjonStrategy
import no.nav.dagpenger.regel.api.streams.DagpengerBehovProducer
import no.nav.dagpenger.regel.api.streams.KafkaDagpengerBehovProducer
import no.nav.dagpenger.regel.api.streams.KafkaSubsumsjonBruktConsumer
import no.nav.dagpenger.regel.api.streams.SubsumsjonPond
import no.nav.dagpenger.regel.api.streams.producerConfig
import no.nav.dagpenger.regel.api.streams.subsumsjonPacketStrategies
import no.nav.dagpenger.streams.KafkaAivenCredentials
import org.slf4j.event.Level
import java.util.concurrent.TimeUnit
import kotlin.concurrent.fixedRateTimer

private val MAINLOGGER = KotlinLogging.logger {}

fun main() {
    val config = Configuration()
    migrate(config)
    val dataSource = dataSourceFrom(config)
    val subsumsjonStore = PostgresSubsumsjonStore(dataSource)
    val bruktSubsumsjonStore = PostgresBruktSubsumsjonStore(dataSource)
    val vaktmester = Vaktmester(dataSource = dataSource, subsumsjonStore = subsumsjonStore)

    fixedRateTimer(
        name = "vaktmester",
        initialDelay = TimeUnit.MINUTES.toMillis(10),
        period = TimeUnit.HOURS.toMillis(12),
        action = {
            MAINLOGGER.info { "Vaktmesteren rydder IKKE" }
            // vaktmester.rydd()
            MAINLOGGER.info { "Vaktmesteren er ferdig... for denne gang" }
        }
    )

    val aivenKafkaConsumer =
        AivenKafkaSubsumsjonConsumer(
            config,
            SubsumsjonPond(subsumsjonPacketStrategies(subsumsjonStore), config, config.regelTopic)
        ).also {
            it.start()
        }

    val bruktSubsumsjonConsumer = KafkaSubsumsjonBruktConsumer(
        config,
        BruktSubsumsjonStrategy(vaktmester = vaktmester, bruktSubsumsjonStore = bruktSubsumsjonStore)
    ).also {
        it.start()
    }

    val kafkaProducer = KafkaDagpengerBehovProducer(
        producerConfig(
            config.application.id,
            config.kafka.aivenBrokers,
            KafkaAivenCredentials()
        ),
        config.regelTopic
    )

    val app = embeddedServer(Netty, port = config.application.httpPort) {
        api(
            subsumsjonStore,
            kafkaProducer,
            config.auth.authApiKeyVerifier,
            listOf(
                subsumsjonStore as HealthCheck,
                bruktSubsumsjonStore as HealthCheck,
                aivenKafkaConsumer as HealthCheck,
                kafkaProducer as HealthCheck,
                bruktSubsumsjonConsumer as HealthCheck
            ),
            config
        )
    }.also {
        it.start(wait = false)
    }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            aivenKafkaConsumer.stop()
            bruktSubsumsjonConsumer.stop()
            app.stop(10000, 60000)
        }
    )
}

internal fun Application.api(
    subsumsjonStore: SubsumsjonStore,
    kafkaProducer: DagpengerBehovProducer,
    apiAuthApiKeyVerifier: AuthApiKeyVerifier,
    healthChecks: List<HealthCheck>,
    config: Configuration,
    prometheusMeterRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry,
) {
    install(DefaultHeaders)

    install(Authentication) {
        apiKeyAuth(name = "X-API-KEY") {
            apiKeyName = "X-API-KEY"
            validate { apikeyCredential: ApiKeyCredential ->
                when {
                    apiAuthApiKeyVerifier.verify(apikeyCredential.value) -> ApiPrincipal(apikeyCredential)
                    else -> null
                }
            }
        }

        jwt(name = "jwt") {
            azureAdJWT(
                providerUrl = config.auth.azureAppWellKnownUrl,
                realm = config.application.id,
                clientId = config.auth.azureAppClientId
            )
        }
    }

    install(CallLogging) {
        level = Level.INFO
        disableDefaultColors()
        filter { call ->
            !call.request.path().startsWith("/isAlive") &&
                !call.request.path().startsWith("/isReady") &&
                !call.request.path().startsWith("/metrics")
        }
    }

    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(jacksonObjectMapper))
    }

    install(Locations)

    install(MicrometerMetrics) {
        registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT, prometheusMeterRegistry, Clock.SYSTEM)
    }

    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            LOGGER.warn("Request failed!", cause)
            call.respond(HttpStatusCode.BadRequest)
        }

        exception<JacksonException> { call, cause ->
            LOGGER.warn("Request failed!", cause)
            call.respond(HttpStatusCode.BadRequest)
        }

        exception<BehovNotFoundException> { call, cause ->
            LOGGER.warn("Request failed!", cause)
            call.respond(HttpStatusCode.NotFound)
        }
        exception<SubsumsjonNotFoundException> { call, cause ->
            LOGGER.warn("Request failed!", cause)
            call.respond(HttpStatusCode.NotFound)
        }
        exception<IllegalUlidException> { call, cause ->
            LOGGER.warn("Request failed!", cause)
            call.respond(HttpStatusCode.BadRequest)
        }
    }

    routing {
        naischecks(healthChecks)
        metrics()

        authenticate("X-API-KEY") {
            subsumsjon(subsumsjonStore)
            lovverk(subsumsjonStore, kafkaProducer)
            behov(subsumsjonStore, kafkaProducer)
        }

        authenticate("jwt") {
            route("/v1") {
                subsumsjon(subsumsjonStore)
                behov(subsumsjonStore, kafkaProducer)
                lovverk(subsumsjonStore, kafkaProducer)
            }
        }
    }
}

internal data class AuthApiKeyVerifier(private val apiKeyVerifier: ApiKeyVerifier, private val clients: List<String>) {
    fun verify(payload: String): Boolean {
        return clients.map { apiKeyVerifier.verify(payload, it) }.firstOrNull { it } ?: false
    }
}
