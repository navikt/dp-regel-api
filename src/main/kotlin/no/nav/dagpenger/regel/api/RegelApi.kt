package no.nav.dagpenger.regel.api

import com.ryanharter.ktor.moshi.moshi
import com.squareup.moshi.JsonDataException
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Locations
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.pipeline.PipelineContext
import no.nav.dagpenger.regel.api.db.BehovNotFoundException
import no.nav.dagpenger.regel.api.db.PostgresSubsumsjonStore
import no.nav.dagpenger.regel.api.db.SubsumsjonNotFoundException
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.db.dataSourceFrom
import no.nav.dagpenger.regel.api.db.migrate
import no.nav.dagpenger.regel.api.grunnlag.grunnlag
import no.nav.dagpenger.regel.api.minsteinntekt.minsteinntekt
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.monitoring.naischecks
import no.nav.dagpenger.regel.api.periode.periode
import no.nav.dagpenger.regel.api.sats.sats
import org.slf4j.event.Level
import java.util.concurrent.TimeUnit

val APPLICATION_NAME = "dp-regel-api"

enum class Regel {
    MINSTEINNTEKT,
    PERIODE,
    GRUNNLAG,
    SATS;
}

fun main() {
    val config = Configuration()

    migrate(config)

    val subsumsjonStore = PostgresSubsumsjonStore(dataSourceFrom(config))

    val kafkaConsumer = KafkaDagpengerBehovConsumer(config, subsumsjonStore).also {
        it.start()
    }

    val kafkaProducer = KafkaDagpengerBehovProducer(producerConfig(
        APPLICATION_NAME,
        config.kafka.brokers,
        config.kafka.credential()))

    val app = embeddedServer(Netty, port = config.application.httpPort) {
        api(
            subsumsjonStore,
            kafkaProducer,
            listOf(subsumsjonStore as HealthCheck, kafkaConsumer as HealthCheck, kafkaProducer as HealthCheck)
        )
    }.also {
        it.start(wait = false)
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        kafkaConsumer.stop()
        app.stop(10, 60, TimeUnit.SECONDS)
    })
}

fun Application.api(
    subsumsjonStore: SubsumsjonStore,
    kafkaProducer: DagpengerBehovProducer,
    healthChecks: List<HealthCheck>
) {
    install(DefaultHeaders)
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
        minsteinntekt(subsumsjonStore, kafkaProducer)
        periode(subsumsjonStore, kafkaProducer)
        grunnlag(subsumsjonStore, kafkaProducer)
        sats(subsumsjonStore, kafkaProducer)
        naischecks(healthChecks)
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
