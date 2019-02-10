package no.nav.dagpenger.regel.api.tasks

import no.nav.dagpenger.regel.api.Regel
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TasksDummy : Tasks {

    companion object {
        val minsteinntektPendingBehovId = "pendingMinsteinntekt"
        val minsteinntektDoneBehovId = "doneMinsteinntekt"
        val periodePendingBehovId = "pendingPeriode"
        val grunnlagPendingBehovId = "pendingGrunnlag"
        val satsPendingBehovId = "pendingSats"
    }

    val tasks: HashMap<String, Task> = hashMapOf(
        createTaskId(Regel.MINSTEINNTEKT, minsteinntektPendingBehovId) to Task(
            minsteinntektPendingBehovId,
            Regel.MINSTEINNTEKT,
            "behov",
            TaskStatus.PENDING,
            ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(2).format(
                DateTimeFormatter.ISO_ZONED_DATE_TIME
            )
        ),
        createTaskId(Regel.MINSTEINNTEKT, minsteinntektDoneBehovId) to Task(
            minsteinntektDoneBehovId,
            Regel.MINSTEINNTEKT,
            "behov",
            TaskStatus.DONE,
            ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(2).format(
                DateTimeFormatter.ISO_ZONED_DATE_TIME
            )
        ),
        createTaskId(Regel.PERIODE, periodePendingBehovId) to Task(
            periodePendingBehovId,
            Regel.PERIODE,
            "behov",
            TaskStatus.PENDING,
            ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(2).format(
                DateTimeFormatter.ISO_ZONED_DATE_TIME
            )
        ),
        createTaskId(Regel.GRUNNLAG, grunnlagPendingBehovId) to Task(
            grunnlagPendingBehovId,
            Regel.GRUNNLAG,
            "behov",
            TaskStatus.PENDING,
            ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(2).format(
                DateTimeFormatter.ISO_ZONED_DATE_TIME
            )
        ),
        createTaskId(Regel.SATS, satsPendingBehovId) to Task(
            satsPendingBehovId,
            Regel.SATS,
            "behov",
            TaskStatus.PENDING,
            ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(2).format(
                DateTimeFormatter.ISO_ZONED_DATE_TIME
            )
        )

    )

    override fun createTask(regel: Regel, behovId: String): Task {
        return when (regel) {
            Regel.MINSTEINNTEKT -> tasks[createTaskId(regel, minsteinntektPendingBehovId)]!!
            Regel.PERIODE -> tasks[createTaskId(regel, periodePendingBehovId)]!!
            Regel.GRUNNLAG -> tasks[createTaskId(regel, grunnlagPendingBehovId)]!!
            Regel.SATS -> tasks[createTaskId(regel, satsPendingBehovId)]!!
        }
    }

    override fun getTask(regel: Regel, behovId: String): Task? = getTask(createTaskId(regel, behovId))

    override fun getTask(taskId: String): Task? = tasks[taskId]

    override fun updateTask(regel: Regel, behovId: String, subsumsjonsId: String): Task {
        val task = tasks[createTaskId(regel, behovId)]!!
        task.status = TaskStatus.DONE
        task.subsumsjonsId = subsumsjonsId
        return task
    }
}

fun createTaskId(regel: Regel, behovId: String): String {
    return "$regel:$behovId"
}