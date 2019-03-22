package no.nav.dagpenger.regel.api.tasks

import no.nav.dagpenger.regel.api.Regel

data class TaskResponse(
    val regel: Regel,
    val status: TaskStatus,
    val expires: String
)

fun taskPending(regel: Regel) = TaskResponse(regel, TaskStatus.PENDING, "not in use")

enum class TaskStatus {
    PENDING, DONE
}

data class Task(
    val taskId: String,
    val regel: Regel,
    val behovId: String,
    var status: TaskStatus,
    val expires: String, // todo: real date
    var subsumsjonsId: String? = null
)

