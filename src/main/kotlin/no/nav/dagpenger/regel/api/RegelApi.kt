package no.nav.dagpenger.regel.api

import de.nielsfalk.ktor.swagger.SwaggerSupport
import de.nielsfalk.ktor.swagger.version.shared.Information
import de.nielsfalk.ktor.swagger.version.v2.Swagger
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.locations.Locations
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
        naischecks()
    }
}
