package no.nav.dagpenger.regel.api.tasks

import no.nav.dagpenger.regel.api.Regel
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class TaskStatus {
    PENDING, DONE
}

data class Task(
    val regel: Regel,
    var status: TaskStatus,
    val expires: String, // todo: real date
    var ressursId: String? = null
)

class Tasks {
    val tasks = mutableMapOf<String, Task>()

    fun createTask(regel: Regel): String {
        val taskId = UUID.randomUUID().toString()
        tasks[taskId] = Task(
            regel,
            TaskStatus.PENDING,
            ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(2).format(
                DateTimeFormatter.ISO_ZONED_DATE_TIME
            )
        )
        return taskId
    }

    fun getTask(taskId: String) = tasks[taskId] ?: throw TaskNotFoundException("no task found for id:{$taskId}")

    // skal bli kalt av kafka-consumer når en regelberegning er ferdig
    fun updateTask(taskId: String, ressursId: String) {
        tasks[taskId]?.status = TaskStatus.DONE
        tasks[taskId]?.ressursId = ressursId
    }
}
class TaskNotFoundException(override val message: String) : RuntimeException(message)
