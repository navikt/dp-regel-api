package no.nav.dagpenger.regel.api.tasks

import de.nielsfalk.ktor.swagger.description
import de.nielsfalk.ktor.swagger.get
import de.nielsfalk.ktor.swagger.version.shared.Group
import io.ktor.application.call
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.Routing
import no.nav.dagpenger.regel.api.TaskResponse

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
