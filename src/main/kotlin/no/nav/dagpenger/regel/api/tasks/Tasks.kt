package no.nav.dagpenger.regel.api.tasks

import no.nav.dagpenger.regel.api.Regel
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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
    val tasks: HashMap<String, Task> = hashMapOf(
            "123456" to Task(
                    Regel.MINSTEINNTEKT,
                    TaskStatus.PENDING,
                    ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(2).format(
                            DateTimeFormatter.ISO_ZONED_DATE_TIME
                    )
            ),
            "987654" to Task(
                    Regel.DAGPENGEGRUNNLAG,
                    TaskStatus.PENDING,
                    ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(2).format(
                            DateTimeFormatter.ISO_ZONED_DATE_TIME
                    )
            )
    )

    fun createTask(regel: Regel): String {
        if (regel == Regel.MINSTEINNTEKT) {
            return "123456"
        } else {
            return "987654"
        }
    }

    fun getTask(taskId: String) = tasks[taskId] ?: throw TaskNotFoundException("no task found for id:{$taskId}")

    // skal bli kalt av kafka-consumer når en regelberegning er ferdig
    fun updateTask(taskId: String, ressursId: String) {
        tasks[taskId]?.status = TaskStatus.DONE
        tasks[taskId]?.ressursId = ressursId
    }
}
class TaskNotFoundException(override val message: String) : RuntimeException(message)
