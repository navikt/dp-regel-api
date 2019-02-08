package no.nav.dagpenger.regel.api.tasks

import io.lettuce.core.api.sync.RedisCommands
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.Regel
import no.nav.dagpenger.regel.api.moshiInstance
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val LOGGER = KotlinLogging.logger {}

class TasksRedis(val redisCommands: RedisCommands<String, String>) : Tasks {

    val jsonAdapter = moshiInstance.adapter(Task::class.java)

    override fun createTask(regel: Regel, behovId: String): Task {
        val taskId = createTaskId(regel, behovId)
        val task = Task(
            taskId,
            regel,
            behovId,
            TaskStatus.PENDING,
            ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(2).format(
                DateTimeFormatter.ISO_ZONED_DATE_TIME
            )
        )
        redisCommands.setTask(taskId, task)
        return task
    }

    override fun getTask(regel: Regel, behovId: String): Task? {
        val taskId = createTaskId(regel, behovId)
        return redisCommands.getTask(taskId)
    }

    override fun getTask(taskId: String): Task? {
        return redisCommands.getTask(taskId)
    }

    override fun updateTask(regel: Regel, behovId: String, subsumsjonsId: String): Task {
        val task = getTask(regel, behovId) ?: throw TaskNotFoundException("Could not find task for regel $regel and behov $behovId")
        task.status = TaskStatus.DONE
        task.subsumsjonsId = subsumsjonsId
        redisCommands.setTask(task.taskId, task)

        LOGGER.info("Updated task with id ${task.taskId}")

        return task
    }

    private fun RedisCommands<String, String>.setTask(taskId: String, task: Task) {
        set("task:$taskId", jsonAdapter.toJson(task))
    }

    private fun RedisCommands<String, String>.getTask(taskId: String): Task? {
        val json = get("task:$taskId") ?: return null
        return jsonAdapter.fromJson(json)
    }

    private fun createTaskId(regel: Regel, behovId: String): String {
        return "$regel:$behovId"
    }
}