package no.nav.dagpenger.regel.api

import de.nielsfalk.ktor.swagger.SwaggerSupport
import de.nielsfalk.ktor.swagger.example
import de.nielsfalk.ktor.swagger.get
import de.nielsfalk.ktor.swagger.ok
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
import io.ktor.request.receive
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KotlinLogging

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

@Group("Minsteinntekt")
@Location("minsteinntekt/{id}")
data class GetMinsteinntekt(val id: String)

@Group("Grunnlag")
@Location("grunnlag/{id}")
data class GetGrunnlag(val id: String)

class RegelApi {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            embeddedServer(Netty, port = 8092, module = Application::api).start(wait = true)
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

fun Routing.minsteinntekt(minsteinntektBeregninger: MinsteinntektBeregninger, tasks: Tasks) {
    post("minsteinntekt") {
        val request = call.receive<MinsteInntektBeregningsRequest>()

        val taskId = tasks.createTask(Regel.MINSTEINNTEKT)

        // dette skal egentlig bli gjort av kafka-consumer når regelberegning er ferdig
        tasks.updateTask(taskId, "123")

        call.response.header(HttpHeaders.Location, "/task/$taskId")
        call.respond(HttpStatusCode.Accepted)
    }

    get<GetMinsteinntekt>("resultat av minsteinntektsberegning".responds(
            ok<MinsteinntektBeregningResultat>(
                    example("model",
                            MinsteinntektBeregningResultat.exampleInntektBeregning
                    )
            ))) { param ->
        val id = param.id

        call.respond(minsteinntektBeregninger.getBeregning(id))
    }
}

fun Routing.grunnlag(grunnlagBeregninger: GrunnlagBeregninger, tasks: Tasks) {
    post("grunnlag") {
        val request = call.receive<GrunnlagBeregningsRequest>()

        val taskId = tasks.createTask(Regel.GRUNNLAG)

        // dette skal egentlig bli gjort av kafka-consumer når regelberegning er ferdig
        tasks.updateTask(taskId, "456")

        call.response.header(HttpHeaders.Location, "/task/$taskId")
        call.respond(HttpStatusCode.Accepted)
    }

    get<GetGrunnlag>("resultat av grunnlagsberegning".responds(
            ok<GrunnlagBeregningResultat>(
                    example("model",
                            GrunnlagBeregningResultat.exampleGrunnlag
                    )
            ))) { param ->
        val id = param.id

        call.respond(grunnlagBeregninger.getBeregning(id))
    }
}

fun Routing.task(tasks: Tasks) {
    get("task/{id}") {
        val id = call.parameters["id"]

        val task = tasks.getTask(id ?: "awe")
        task.status

        if (task.status == TaskStatus.PENDING) {
            call.respond(TaskResponse(task.regel, task.status, task.expires))
        } else if (task.status == TaskStatus.DONE) {
            call.response.header(HttpHeaders.Location, "/${task.regel}/${task.ressursId}")
            call.respond(HttpStatusCode.SeeOther)
        }
    }
}