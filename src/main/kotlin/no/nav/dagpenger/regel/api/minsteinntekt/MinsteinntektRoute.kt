package no.nav.dagpenger.regel.api.minsteinntekt

import de.huxhorn.sulky.ulid.ULID
import io.ktor.application.call
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.Regel
import no.nav.dagpenger.regel.api.VilkårProducer
import no.nav.dagpenger.regel.api.minsteinntekt.model.MinsteinntektInnParametere
import no.nav.dagpenger.regel.api.tasks.Tasks
import no.nav.dagpenger.regel.api.tasks.taskResponseFromTask

private val LOGGER = KotlinLogging.logger {}

fun Routing.minsteinntekt(minsteinntektBeregninger: MinsteinntektBeregninger, tasks: Tasks, kafkaProducer: VilkårProducer) {

    val ulidGenerator = ULID()
    route("/minsteinntekt") {
        post {
            val parametere = call.receive<MinsteinntektInnParametere>()

            val taskId = tasks.createTask(Regel.MINSTEINNTEKT)

            tasks.updateTask(taskId, "123")

            call.response.header(HttpHeaders.Location, "/task/$taskId")
            call.respond(HttpStatusCode.Accepted, taskResponseFromTask(tasks.getTask(taskId)))
        }

        get("/{subsumsjonsid}"){
            call.parameters["subsumsjonsid"]?.let { subsumsjonsid ->
                call.respond(HttpStatusCode.OK)
            } ?: call.respond(HttpStatusCode.BadRequest)
        }
    }
}