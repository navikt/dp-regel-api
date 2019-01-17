package no.nav.dagpenger.regel.api.tasks

import com.google.gson.Gson
import io.lettuce.core.api.sync.RedisCommands
import no.nav.dagpenger.regel.api.Regel
import redis.clients.jedis.Jedis
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class TasksRedis(val jedis: Jedis) : Tasks {

    override fun createTask(regel: Regel): String {
        val taskId = UUID.randomUUID().toString()
        val task = Task(
            regel,
            TaskStatus.PENDING,
            ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(2).format(
                DateTimeFormatter.ISO_ZONED_DATE_TIME
            )
        )
        jedis.set(taskId, task)
        return taskId
    }

    override fun getTask(taskId: String) = jedis.getTask(taskId)

    // skal bli kalt av kafka-consumer når en regelberegning er ferdig
    override fun updateTask(taskId: String, ressursId: String) {
        val task = jedis.getTask(taskId)
        task.status = TaskStatus.DONE
        task.ressursId = ressursId
        jedis.set(taskId, task)
    }
}

class TaskNotFoundException(override val message: String) : RuntimeException(message)

fun Jedis.set(id: String, task: Task) {
    set("task:$id", Gson().toJson(task))
}

fun Jedis.getTask(id: String): Task {
    return Gson().fromJson(get("task:$id"), Task::class.java)
}