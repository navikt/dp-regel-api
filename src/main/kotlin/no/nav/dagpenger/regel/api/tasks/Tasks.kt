package no.nav.dagpenger.regel.api.tasks

import no.nav.dagpenger.regel.api.Regel

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

interface Tasks {

    fun createTask(regel: Regel, behovId: String): Task

    fun getTask(regel: Regel, behovId: String): Task?

    fun getTask(taskId: String): Task?

    fun updateTask(regel: Regel, behovId: String, subsumsjonsId: String): Task
}
