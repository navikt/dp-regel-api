package no.nav.dagpenger.regel.api

import de.nielsfalk.ktor.swagger.SwaggerSupport
import de.nielsfalk.ktor.swagger.description
import de.nielsfalk.ktor.swagger.example
import de.nielsfalk.ktor.swagger.examples
import de.nielsfalk.ktor.swagger.get
import de.nielsfalk.ktor.swagger.ok
import de.nielsfalk.ktor.swagger.post
import de.nielsfalk.ktor.swagger.responds
import de.nielsfalk.ktor.swagger.version.shared.Group
import de.nielsfalk.ktor.swagger.version.shared.Information
import de.nielsfalk.ktor.swagger.version.v2.Swagger
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.locations.Locations
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KotlinLogging
import java.util.concurrent.TimeUnit

private val LOGGER = KotlinLogging.logger {}

data class MinsteInntektBeregningsRequest(
    val aktorId: String,
    val verneplikt: Boolean,
    val fangstOgFisk: Boolean
)

data class GrunnlagBeregningsRequest(
    val aktorId: String,
    val verneplikt: Boolean,
    val fangstOgFisk: Boolean
)

data class TaskResponse(
    val regel: Regel,
    val status: TaskStatus,
    val expires: String
)

enum class Regel {
    MINSTEINNTEKT, GRUNNLAG
}

class RegelApi {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val app = embeddedServer(Netty, port = 8092, module = Application::api)
            app.start(wait = false)
            Runtime.getRuntime().addShutdownHook(Thread {
                app.stop(5, 60, TimeUnit.SECONDS)
            })
        }
    }
}

fun Application.api() {

    val tasks = Tasks()
    val grunnlagBeregninger = GrunnlagBeregninger()
    val minsteinntektBeregninger = MinsteinntektBeregninger()

    install(DefaultHeaders)
    install(CallLogging)
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

    routing {
        task(tasks)
        minsteinntekt(minsteinntektBeregninger, tasks)
        grunnlag(grunnlagBeregninger, tasks)
    }
}

@Group("Minsteinntekt")
@Location("/minsteinntekt")
class PostMinsteinntekt

@Group("Minsteinntekt")
@Location("/minsteinntekt/{id}")
data class GetMinsteinntekt(val id: String)

fun Routing.minsteinntekt(minsteinntektBeregninger: MinsteinntektBeregninger, tasks: Tasks) {
    post<PostMinsteinntekt, MinsteInntektBeregningsRequest>(
        "minsteinntektsberegning"
            .description("Start minsteinntektsberegning")
            .examples()
            .responds()
    ) { _, request ->
        val taskId = tasks.createTask(Regel.MINSTEINNTEKT)

        // dette skal egentlig bli gjort av kafka-consumer når regelberegning er ferdig
        tasks.updateTask(taskId, "123")

        call.response.header(HttpHeaders.Location, "/task/$taskId")
        call.respond(HttpStatusCode.Accepted, taskResponseFromTask(tasks.getTask(taskId)))
    }

    get<GetMinsteinntekt>(
        "resultat av minsteinntektsberegning".responds(
            ok<MinsteinntektBeregningResultat>(
                example(
                    "model",
                    MinsteinntektBeregningResultat.exampleInntektBeregning
                )
            )
        )
    ) { param ->
        val id = param.id

        call.respond(minsteinntektBeregninger.getBeregning(id))
    }
}

@Group("Grunnlag")
@Location("/grunnlag")
class PostGrunnlag

@Group("Grunnlag")
@Location("/grunnlag/{id}")
data class GetGrunnlag(val id: String)

fun Routing.grunnlag(grunnlagBeregninger: GrunnlagBeregninger, tasks: Tasks) {
    post<PostGrunnlag, GrunnlagBeregningsRequest>(
        "grunnlagberegning"
            .description("")
            .examples()
            .responds()
    ) { _, request ->

        val taskId = tasks.createTask(Regel.GRUNNLAG)

        // dette skal egentlig bli gjort av kafka-consumer når regelberegning er ferdig
        tasks.updateTask(taskId, "456")

        call.response.header(HttpHeaders.Location, "/task/$taskId")
        call.respond(HttpStatusCode.Accepted, taskResponseFromTask(tasks.getTask(taskId)))
    }

    get<GetGrunnlag>(
        "resultat av grunnlagsberegning".responds(
            ok<GrunnlagBeregningResultat>(
                example(
                    "model",
                    GrunnlagBeregningResultat.exampleGrunnlag
                )
            )
        )
    ) { param ->
        val id = param.id

        call.respond(grunnlagBeregninger.getBeregning(id))
    }
}

fun taskResponseFromTask(task: Task): TaskResponse {
    return TaskResponse(task.regel, task.status, task.expires)
}

@Group("Task")
@Location("/task/{id}")
data class GetTask(val id: String)

fun Routing.task(tasks: Tasks) {
    get<GetTask>(
        "task"
            .description("")
    ) { param ->
        val id = param.id

        val task = tasks.getTask(id)
        task.status

        if (task.status == TaskStatus.PENDING) {
            call.respond(taskResponseFromTask(task))
        } else if (task.status == TaskStatus.DONE) {
            call.response.header(
                HttpHeaders.Location, "/${task.regel.toString().toLowerCase()}/${task.ressursId}"
            )
            call.respond(HttpStatusCode.SeeOther)
        }
    }
}