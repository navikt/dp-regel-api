package no.nav.dagpenger.regel.api.tasks

import io.ktor.application.call
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import no.nav.dagpenger.regel.api.BadRequestException
import no.nav.dagpenger.regel.api.TaskResponse

fun taskResponseFromTask(task: Task): TaskResponse {
    return TaskResponse(task.regel, task.status, task.expires)
}

fun Routing.task(tasks: Tasks) {
    get("/task/{id}") {

        val id = call.parameters["id"] ?: throw BadRequestException()

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
