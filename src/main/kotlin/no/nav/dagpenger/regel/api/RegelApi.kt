package no.nav.dagpenger.regel.api

import com.ryanharter.ktor.moshi.moshi
import com.squareup.moshi.JsonDataException
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Locations
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.pipeline.PipelineContext
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import mu.KotlinLogging
import no.nav.dagpenger.ktor.auth.apiKeyAuth
import no.nav.dagpenger.plain.producerConfig
import no.nav.dagpenger.regel.api.auth.AuthApiKeyVerifier
import no.nav.dagpenger.regel.api.db.BehovNotFoundException
import no.nav.dagpenger.regel.api.db.PostgresSubsumsjonStore
import no.nav.dagpenger.regel.api.db.SubsumsjonNotFoundException
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.db.dataSourceFrom
import no.nav.dagpenger.regel.api.db.migrate
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.routing.behov
import no.nav.dagpenger.regel.api.routing.metrics
import no.nav.dagpenger.regel.api.routing.naischecks
import no.nav.dagpenger.regel.api.routing.subsumsjon
import no.nav.dagpenger.regel.api.streams.DagpengerBehovProducer
import no.nav.dagpenger.regel.api.streams.KafkaDagpengerBehovProducer
import no.nav.dagpenger.regel.api.streams.SubsumsjonPond
import no.nav.dagpenger.regel.api.streams.subsumsjonPacketStrategies
import org.slf4j.event.Level
import java.util.concurrent.TimeUnit

val APPLICATION_NAME = "dp-regel-api"

private val LOGGER = KotlinLogging.logger {}

fun main() {
    val config = Configuration()

    migrate(config)

    val subsumsjonStore = PostgresSubsumsjonStore(dataSourceFrom(config))

    val kafkaConsumer = SubsumsjonPond(subsumsjonPacketStrategies(subsumsjonStore), config).also {
        LOGGER.info { "Starting Subsumsjon Consumer" }
        it.start()
    }

    val kafkaProducer = KafkaDagpengerBehovProducer(producerConfig(
        clientId = APPLICATION_NAME,
        bootstrapServers = config.kafka.brokers,
        credential = config.kafka.credential()))

    val app = embeddedServer(Netty, port = config.application.httpPort) {
        api(
            subsumsjonStore,
            kafkaProducer,
            config.auth.authApiKeyVerifier,
            listOf(subsumsjonStore as HealthCheck)
        )
    }.also {
        it.start(wait = false)
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        kafkaConsumer.stop()
        kafkaProducer.stop()
        app.stop(10, 60, TimeUnit.SECONDS)
    })
}

internal fun Application.api(
    subsumsjonStore: SubsumsjonStore,
    kafkaProducer: DagpengerBehovProducer,
    apiAuthApiKeyVerifier: AuthApiKeyVerifier,
    healthChecks: List<HealthCheck>
) {
    install(DefaultHeaders)

    install(Authentication) {
        apiKeyAuth {
            apiKeyName = "X-API-KEY"
            validate { creds -> apiAuthApiKeyVerifier.verify(creds) }
        }
    }

    install(CallLogging) {
        level = Level.INFO

        filter { call ->
            !call.request.path().startsWith("/isAlive") &&
                !call.request.path().startsWith("/isReady") &&
                !call.request.path().startsWith("/metrics")
        }
    }

    install(ContentNegotiation) {
        moshi(moshiInstance)
    }
    install(Locations)

    install(MicrometerMetrics) {
        registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT, CollectorRegistry.defaultRegistry, Clock.SYSTEM)
    }

    install(StatusPages) {
        exception<BadRequestException> { cause ->
            badRequest(cause)
        }
        exception<JsonDataException> { cause ->
            badRequest(cause)
        }
        exception<BehovNotFoundException> { cause ->
            notFound(cause)
        }
        exception<SubsumsjonNotFoundException> { cause ->
            notFound(cause)
        }
    }

    routing {
        behov(subsumsjonStore, kafkaProducer)
        subsumsjon(subsumsjonStore)
        naischecks(healthChecks)
        metrics()
    }
}

private suspend fun <T : Throwable> PipelineContext<Unit, ApplicationCall>.badRequest(
    cause: T
) {
    call.respond(HttpStatusCode.BadRequest)
    throw cause
}

private suspend fun <T : Throwable> PipelineContext<Unit, ApplicationCall>.notFound(
    cause: T
) {
    call.respond(HttpStatusCode.NotFound)
    throw cause
}

class BadRequestException : RuntimeException()
