package no.nav.dagpenger.regel.api.db

import com.zaxxer.hikari.HikariDataSource
import org.junit.Test
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private object PostgresContainer {
    val instance by lazy {
        PostgreSQLContainer<Nothing>("postgres:11").apply {
            start()
        }
    }
}

private object DataSource {
    val instance by lazy {
        HikariDataSource().apply {
            username = PostgresContainer.instance.username
            password = PostgresContainer.instance.password
            jdbcUrl = PostgresContainer.instance.jdbcUrl
        }
    }
}

private fun withCleanDb(test: () -> Unit) = DataSource.instance.also { clean(it) }.run { test() }

private fun withMigratedDb(test: () -> Unit) = DataSource.instance.also { clean(it) }.also { migrate(it) }.run { test() }

class PostgresSubsumssjonStoreTest {
    @Test
    fun `Migration scripts are applied successfully`() {
        withCleanDb {
            val migrations = migrate(DataSource.instance)
            assertEquals(1, migrations, "Wrong number of migrations")
        }
    }

    @Test
    fun `CRUD Operations`() {
        withMigratedDb {
            val json = """{"test": 1}""".trimIndent()

            with(PostgresSubsumsjonStore(DataSource.instance)) {
                insert("1", json)
                assertEquals(json, get("1"))
            }
        }
    }

    @Test
    fun `Exeception if subsumsjon not found `() {
        withMigratedDb {

            with(PostgresSubsumsjonStore(DataSource.instance)) {
                assertFailsWith<SubsumsjonNotFoundException> { get("hubba")  }
            }
        }
    }

    @Test
    fun `Upsert on duplicate subsumsjon ids`() {
        withMigratedDb {

            with(PostgresSubsumsjonStore(DataSource.instance)) {
                val json1 = """{"test": 1}""".trimIndent()
                val json2 = """{"test": 2}""".trimIndent()
                val subsumsjonId = "1"

                insert(subsumsjonId, json1)
                assertEquals(json1, get(subsumsjonId))

                insert(subsumsjonId, json2)
                assertEquals(json2, get(subsumsjonId))
            }
        }
    }
}
