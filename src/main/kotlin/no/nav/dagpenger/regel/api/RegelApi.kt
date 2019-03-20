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
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.db.migrate
import no.nav.dagpenger.regel.api.grunnlag.GrunnlagSubsumsjoner
import no.nav.dagpenger.regel.api.grunnlag.GrunnlagSubsumsjonerRedis
import no.nav.dagpenger.regel.api.grunnlag.grunnlag
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektSubsumsjoner
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektSubsumsjonerRedis
import no.nav.dagpenger.regel.api.minsteinntekt.minsteinntekt
import no.nav.dagpenger.regel.api.periode.PeriodeSubsumsjoner
import no.nav.dagpenger.regel.api.periode.PeriodeSubsumsjonerRedis
import no.nav.dagpenger.regel.api.periode.periode
import no.nav.dagpenger.regel.api.sats.SatsSubsumsjoner
import no.nav.dagpenger.regel.api.sats.SatsSubsumsjonerRedis
import no.nav.dagpenger.regel.api.sats.sats
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
    MINSTEINNTEKT,
    PERIODE,
    GRUNNLAG,
    SATS
}

fun main(args: Array<String>) {
    val config = Configuration()

    if (config.application.profile != Profile.LOCAL) {
        migrate(config)
    }
    val env = Environment()

    val redisUri = RedisURI.Builder.redis(env.redisHost).build()
    val redisClient = RedisClient.create(redisUri)
    val connection = redisClient.connect()
    val redisCommands = connection.sync()

    val tasks = TasksRedis(redisCommands)
    val minsteinntektSubsumsjoner = MinsteinntektSubsumsjonerRedis(redisCommands)
    val periodeSubsumsjoner = PeriodeSubsumsjonerRedis(redisCommands)
    val grunnlagSubsumsjoner = GrunnlagSubsumsjonerRedis(redisCommands)
    val satsSubsumsjoner = SatsSubsumsjonerRedis(redisCommands)

    val kafkaProducer = KafkaDagpengerBehovProducer(env)

    val kafkaConsumer =
            KafkaDagpengerBehovConsumer(
                    env,
                    tasks,
                    minsteinntektSubsumsjoner,
                    periodeSubsumsjoner,
                    grunnlagSubsumsjoner,
                    satsSubsumsjoner
            )
    kafkaConsumer.start()

    val app = embeddedServer(Netty, port = env.httpPort) {
        api(
                tasks,
                minsteinntektSubsumsjoner,
                periodeSubsumsjoner,
                grunnlagSubsumsjoner,
                satsSubsumsjoner,
                kafkaProducer
        )
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
    minsteinntektSubsumsjoner: MinsteinntektSubsumsjoner,
    periodeSubsumsjoner: PeriodeSubsumsjoner,
    grunnlagSubsumsjoner: GrunnlagSubsumsjoner,
    satsSubsumsjoner: SatsSubsumsjoner,
    kafkaProducer: DagpengerBehovProducer
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
        exception<TaskNotFoundException> { cause ->
            notFound(cause)
        }
        exception<SubsumsjonNotFoundException> { cause ->
            notFound(cause)
        }
    }

    routing {
        task(tasks)
        minsteinntekt(minsteinntektSubsumsjoner, tasks, kafkaProducer)
        periode(periodeSubsumsjoner, tasks, kafkaProducer)
        grunnlag(grunnlagSubsumsjoner, tasks, kafkaProducer)
        sats(satsSubsumsjoner, tasks, kafkaProducer)
        naischecks()
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

class SubsumsjonNotFoundException(override val message: String) : RuntimeException(message)