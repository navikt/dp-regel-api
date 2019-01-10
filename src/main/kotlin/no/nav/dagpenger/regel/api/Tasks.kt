package no.nav.dagpenger.regel.api

import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class Task(
    val regel: String,
    var status: String, // todo: enum?
    val expires: String, // todo: real date
    var ressursId: String? = null
)

class Tasks {
    val tasks = mutableMapOf<String, Task>()

    fun createTask(taskId: String) {
        tasks[taskId] = Task(
            "minsteinntekt",
            "pending",
            ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(2).format(
                DateTimeFormatter.ISO_ZONED_DATE_TIME
            )
        )
    }

    fun getTask(taskId: String) = tasks[taskId] ?: throw Exception("no task")

    // skal bli kallt av kafka-consumer når en regelberegning er ferdig
    fun updateTask(taskId: String, ressursId: String) {
        tasks[taskId]?.status = "done"
        tasks[taskId]?.ressursId = ressursId
    }
}