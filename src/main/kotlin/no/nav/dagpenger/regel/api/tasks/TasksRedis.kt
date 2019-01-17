package no.nav.dagpenger.regel.api.tasks

import com.google.gson.Gson
import io.lettuce.core.api.sync.RedisCommands
import no.nav.dagpenger.regel.api.Regel
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class TasksRedis(val redisCommands: RedisCommands<String, String>) : Tasks {

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

    override fun getTask(taskId: String) = redisCommands.getTask(taskId)

    // skal bli kalt av kafka-consumer når en regelberegning er ferdig
    override fun updateTask(taskId: String, ressursId: String) {
        val task = redisCommands.getTask(taskId)
        task.status = TaskStatus.DONE
        task.ressursId = ressursId
        redisCommands.set(taskId, task)
    }
}

class TaskNotFoundException(override val message: String) : RuntimeException(message)

fun RedisCommands<String, String>.set(id: String, task: Task) {
    set("task:$id", Gson().toJson(task))
}

fun RedisCommands<String, String>.getTask(id: String): Task {
    return Gson().fromJson(get("task:$id"), Task::class.java)
}