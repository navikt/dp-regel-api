package no.nav.dagpenger.regel.api

import no.nav.dagpenger.regel.api.tasks.Task
import no.nav.dagpenger.regel.api.tasks.TaskNotFoundException
import no.nav.dagpenger.regel.api.tasks.TaskStatus
import no.nav.dagpenger.regel.api.tasks.Tasks
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TasksDummy : Tasks {

    companion object {
        val minsteinntektPendingTaskId = "pendingMinsteinntekt"
        val minsteinntektDoneTaskId = "doneMinsteinntekt"
        val grunnlagPendingTaskId = "pendingGrunnlag"
    }

    val tasks: HashMap<String, Task> = hashMapOf(
        minsteinntektPendingTaskId to Task(
            minsteinntektPendingTaskId,
            Regel.MINSTEINNTEKT,
            "behov",
            TaskStatus.PENDING,
            ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(2).format(
                DateTimeFormatter.ISO_ZONED_DATE_TIME
            )
        ),
        minsteinntektDoneTaskId to Task(
            minsteinntektDoneTaskId,
            Regel.MINSTEINNTEKT,
            "behov",
            TaskStatus.DONE,
            ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(2).format(
                DateTimeFormatter.ISO_ZONED_DATE_TIME
            )
        ),
        grunnlagPendingTaskId to Task(
            grunnlagPendingTaskId,
            Regel.DAGPENGEGRUNNLAG,
            "behov",
            TaskStatus.PENDING,
            ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(2).format(
                DateTimeFormatter.ISO_ZONED_DATE_TIME
            )
        )
    )

    override fun createTask(regel: Regel, behovId: String): Task {
        if (regel == Regel.MINSTEINNTEKT) {
            return tasks[minsteinntektPendingTaskId]!!
        } else {
            return tasks[grunnlagPendingTaskId]!!
        }
    }

    override fun getTask(regel: Regel, behovId: String): Task? = getTask(behovId)

    override fun getTask(taskId: String): Task? = tasks[taskId] ?: throw TaskNotFoundException(
        "no task found for id:{$taskId}"
    )

    // skal bli kalt av kafka-consumer når en regelberegning er ferdig
    override fun updateTask(regel: Regel, behovId: String, subsumsjonsId: String): Task {
        val task = tasks[behovId]!!
        task.status = TaskStatus.DONE
        task.subsumsjonsId = subsumsjonsId
        return task
    }
}
