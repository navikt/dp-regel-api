package no.nav.dagpenger.regel.api

import com.google.gson.JsonSyntaxException
import de.nielsfalk.ktor.swagger.SwaggerSupport
import de.nielsfalk.ktor.swagger.version.shared.Information
import de.nielsfalk.ktor.swagger.version.v2.Swagger
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Locations
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.grunnlag.GrunnlagBeregninger
import no.nav.dagpenger.regel.api.grunnlag.grunnlag
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektBeregninger
import no.nav.dagpenger.regel.api.minsteinntekt.minsteinntekt
import no.nav.dagpenger.regel.api.tasks.TaskStatus
import no.nav.dagpenger.regel.api.tasks.Tasks
import no.nav.dagpenger.regel.api.tasks.task
import org.slf4j.event.Level
import java.util.concurrent.TimeUnit
import io.lettuce.core.RedisClient
import no.nav.dagpenger.regel.api.tasks.TasksRedis

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

    val redisClient = RedisClient.create("redis-sentinel://${env.redisHost}:26379/0#mymaster")
    val connection = redisClient.connect()
    val redisCommands = connection.sync()

    val tasks = TasksRedis(redisCommands)

    //VilkårKafkaConsumer(env, redisCommands, tasks).start()

    val app = embeddedServer(Netty, port = 8092) {
        api(tasks, MinsteinntektBeregninger(), GrunnlagBeregninger())
    }
    app.start(wait = false)
    Runtime.getRuntime().addShutdownHook(Thread {
        connection.close()
        redisClient.shutdown()
        app.stop(5, 60, TimeUnit.SECONDS)
    })
}

fun Application.api(
    tasks: Tasks,
    minsteinntektBeregninger: MinsteinntektBeregninger,
    grunnlagBeregninger: GrunnlagBeregninger
) {
    install(DefaultHeaders)
    install(CallLogging) {
        level = Level.INFO
    }
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }
    install(Locations)

    install(SwaggerSupport) {
        forwardRoot = true
        val information = Information(
            title = "Dagpenger regel-api"
        )
        swagger = Swagger().apply {
            info = information
        }
    }

    install(StatusPages) {
        exception<JsonSyntaxException> { cause ->
            call.respond(HttpStatusCode.BadRequest)
            throw cause
        }
    }

    routing {
        task(tasks)
        minsteinntekt(minsteinntektBeregninger, tasks)
        grunnlag(grunnlagBeregninger, tasks)
        naischecks()
    }
}
