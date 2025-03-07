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
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.Vaktmester.Companion.LOGGER
import no.nav.dagpenger.regel.api.auth.azureAdJWT
import no.nav.dagpenger.regel.api.db.BehovNotFoundException
import no.nav.dagpenger.regel.api.db.PostgresBruktSubsumsjonStore
import no.nav.dagpenger.regel.api.db.PostgresDataSourceBuilder
import no.nav.dagpenger.regel.api.db.PostgresSubsumsjonStore
import no.nav.dagpenger.regel.api.db.SubsumsjonNotFoundException
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
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
import no.nav.dagpenger.regel.api.streams.subsumsjonPacketStrategies
import org.slf4j.event.Level
import java.util.concurrent.TimeUnit
import kotlin.concurrent.fixedRateTimer

private val MAINLOGGER = KotlinLogging.logger {}

fun main() {
    PostgresDataSourceBuilder.runMigration()
    val subsumsjonStore = PostgresSubsumsjonStore(PostgresDataSourceBuilder.dataSource)
    val bruktSubsumsjonStore = PostgresBruktSubsumsjonStore(PostgresDataSourceBuilder.dataSource)
    val vaktmester = Vaktmester(dataSource = PostgresDataSourceBuilder.dataSource, subsumsjonStore = subsumsjonStore)

    fixedRateTimer(
        name = "vaktmester",
        initialDelay = TimeUnit.MINUTES.toMillis(10),
        period = TimeUnit.HOURS.toMillis(12),
        action = {
            MAINLOGGER.info { "Vaktmesteren rydder IKKE" }
            // vaktmester.rydd()
            MAINLOGGER.info { "Vaktmesteren er ferdig... for denne gang" }
        },
    )

    val aivenKafkaConsumer =
        AivenKafkaSubsumsjonConsumer(
            Configuration,
            SubsumsjonPond(subsumsjonPacketStrategies(subsumsjonStore), Configuration.regelTopicName),
        ).also {
            it.start()
        }

    val bruktSubsumsjonConsumer =
        KafkaSubsumsjonBruktConsumer(
            Configuration,
            BruktSubsumsjonStrategy(vaktmester = vaktmester, bruktSubsumsjonStore = bruktSubsumsjonStore),
        ).also {
            it.start()
        }

    val kafkaProducer =
        KafkaDagpengerBehovProducer(
            producerConfig(
                Configuration.id,
                Configuration.aivenBrokers,
                KafkaAivenCredentials(),
            ),
            Configuration.regelTopicName,
        )

    val app =
        embeddedServer(Netty, port = Configuration.httpPort) {
            api(
                subsumsjonStore,
                kafkaProducer,
                listOf(
                    subsumsjonStore as HealthCheck,
                    bruktSubsumsjonStore as HealthCheck,
                    aivenKafkaConsumer as HealthCheck,
                    kafkaProducer as HealthCheck,
                    bruktSubsumsjonConsumer as HealthCheck,
                ),
                Configuration,
            )
        }.also {
            it.start(wait = false)
        }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            aivenKafkaConsumer.stop()
            bruktSubsumsjonConsumer.stop()
            app.stop(10000, 60000)
        },
    )
}

internal fun Application.api(
    subsumsjonStore: SubsumsjonStore,
    kafkaProducer: DagpengerBehovProducer,
    healthChecks: List<HealthCheck>,
    config: Configuration,
    prometheusMeterRegistry: PrometheusRegistry = PrometheusRegistry.defaultRegistry,
) {
    install(DefaultHeaders)

    install(Authentication) {
        jwt(name = "jwt") {
            azureAdJWT(
                providerUrl = config.azureAppWellKnownUrl,
                realm = config.id,
                clientId = config.azureAppClientId,
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

        authenticate("jwt") {
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
