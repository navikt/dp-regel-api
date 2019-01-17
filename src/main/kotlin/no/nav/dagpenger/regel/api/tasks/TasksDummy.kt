package no.nav.dagpenger.regel.api.tasks

import no.nav.dagpenger.regel.api.Regel
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TasksDummy : Tasks {
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

    override fun createTask(regel: Regel): String {
        if (regel == Regel.MINSTEINNTEKT) {
            return "123456"
        } else {
            return "987654"
        }
    }

    override fun getTask(taskId: String) = tasks[taskId] ?: throw TaskNotFoundException("no task found for id:{$taskId}")

    // skal bli kalt av kafka-consumer når en regelberegning er ferdig
    override fun updateTask(taskId: String, ressursId: String) {
        tasks[taskId]?.status = TaskStatus.DONE
        tasks[taskId]?.ressursId = ressursId
    }
}
