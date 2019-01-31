package no.nav.dagpenger.regel.api.tasks

import io.lettuce.core.api.sync.RedisCommands
import no.nav.dagpenger.regel.api.Regel
import no.nav.dagpenger.regel.api.moshiInstance
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class TasksRedis(val redisCommands: RedisCommands<String, String>) : Tasks {

    val jsonAdapter = moshiInstance.adapter(Task::class.java)

    override fun createTask(regel: Regel): String {
        val taskId = UUID.randomUUID().toString()
        val task = Task(
            regel,
            TaskStatus.PENDING,
            ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(2).format(
                DateTimeFormatter.ISO_ZONED_DATE_TIME
            )
        )
        redisCommands.set(taskId, task)
        return taskId
    }

    override fun getTask(taskId: String) =
        redisCommands.getTask(taskId) ?: throw TaskNotFoundException("no task found for id:{$taskId}")

    // skal bli kalt av kafka-consumer når en regelberegning er ferdig
    override fun updateTask(taskId: String, ressursId: String) {
        val task = getTask(taskId)
        task.status = TaskStatus.DONE
        task.ressursId = ressursId
        redisCommands.set(taskId, task)
    }

    fun RedisCommands<String, String>.set(id: String, task: Task) {
        set("task:$id", jsonAdapter.toJson(task))
    }

    fun RedisCommands<String, String>.getTask(id: String): Task? {
        return jsonAdapter.fromJson(get("task:$id"))
    }
}

class TaskNotFoundException(override val message: String) : RuntimeException(message)
