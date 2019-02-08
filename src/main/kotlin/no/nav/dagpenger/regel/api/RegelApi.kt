package no.nav.dagpenger.regel.api

import com.ryanharter.ktor.moshi.moshi
import com.squareup.moshi.JsonDataException
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Locations
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.grunnlag.DagpengegrunnlagBeregninger
import no.nav.dagpenger.regel.api.grunnlag.DagpengegrunnlagBeregningerRedis
import no.nav.dagpenger.regel.api.grunnlag.grunnlag
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektSubsumsjoner
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektBeregningerRedis
import no.nav.dagpenger.regel.api.minsteinntekt.minsteinntekt
import no.nav.dagpenger.regel.api.tasks.TaskNotFoundException
import no.nav.dagpenger.regel.api.tasks.TaskStatus
import no.nav.dagpenger.regel.api.tasks.Tasks
import no.nav.dagpenger.regel.api.tasks.TasksRedis
import no.nav.dagpenger.regel.api.tasks.task
import org.slf4j.event.Level
import java.util.concurrent.TimeUnit

private val LOGGER = KotlinLogging.logger {}

data class TaskResponse(
    val regel: Regel,
    val status: TaskStatus,
    val expires: String
)

enum class Regel {
    MINSTEINNTEKT, DAGPENGEGRUNNLAG
}

fun main(args: Array<String>) {
    val env = Environment()

    val redisUri = RedisURI.Builder.sentinel(env.redisHost, "mymaster").build()
    val redisClient = RedisClient.create(redisUri)
    val connection = redisClient.connect()
    val redisCommands = connection.sync()

    val tasks = TasksRedis(redisCommands)
    val minsteinntektBeregninger = MinsteinntektBeregningerRedis(redisCommands)
    val dagpengegrunnlagBeregninger = DagpengegrunnlagBeregningerRedis(redisCommands)

    val kafkaProducer = KafkaDagpengerBehovProducer(env)
    val kafkaConsumer = KafkaDagpengerBehovConsumer(env, tasks, minsteinntektBeregninger)
    kafkaConsumer.start()

    val app = embeddedServer(Netty, port = 8092) {
        api(tasks, minsteinntektBeregninger, dagpengegrunnlagBeregninger, kafkaProducer)
    }

    app.start(wait = false)

    Runtime.getRuntime().addShutdownHook(Thread {
        connection.close()
        redisClient.shutdown()
        kafkaConsumer.stop()
        app.stop(10, 60, TimeUnit.SECONDS)
    })
}

fun Application.api(
    tasks: Tasks,
    minsteinntektBeregninger: MinsteinntektSubsumsjoner,
    grunnlagBeregninger: DagpengegrunnlagBeregninger,
    kafkaProducer: DagpengerBehovProducer
) {
    install(DefaultHeaders)
    install(CallLogging) {
        level = Level.INFO
    }
    install(ContentNegotiation) {
        moshi(moshiInstance)
    }
    install(Locations)

    install(StatusPages) {
        exception<BadRequestException> { cause ->
            LOGGER.warn("Bad request") { cause }
            call.respond(HttpStatusCode.BadRequest)
        }
        exception<JsonDataException> { cause ->
            LOGGER.warn("Bad request") { cause }
            call.respond(HttpStatusCode.BadRequest)
        }
        exception<TaskNotFoundException> { cause ->
            LOGGER.warn("Unknown task id") { cause }
            call.respond(HttpStatusCode.NotFound)
        }
    }

    routing {
        task(tasks)
        minsteinntekt(minsteinntektBeregninger, tasks, kafkaProducer)
        grunnlag(grunnlagBeregninger, tasks, kafkaProducer)
        naischecks()
    }
}

class BadRequestException : RuntimeException()
