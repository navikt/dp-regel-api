package no.nav.dagpenger.regel.api.tasks

import no.nav.dagpenger.regel.api.Regel

enum class TaskStatus {
    PENDING, DONE
}

data class Task(
    val regel: Regel,
    var status: TaskStatus,
    val expires: String, // todo: real date
    var ressursId: String? = null
)

interface Tasks {

    fun createTask(regel: Regel): String

    fun getTask(taskId: String): Task

    // skal bli kalt av kafka-consumer når en regelberegning er ferdig
    fun updateTask(taskId: String, ressursId: String)
}
