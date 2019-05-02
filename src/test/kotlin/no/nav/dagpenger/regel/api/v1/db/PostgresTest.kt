package no.nav.dagpenger.regel.api.v1.db

import com.zaxxer.hikari.HikariDataSource
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.mockk.mockk
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.db.clean
import no.nav.dagpenger.regel.api.db.hikariConfigFrom
import no.nav.dagpenger.regel.api.db.migrate
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import no.nav.dagpenger.regel.api.v1.models.Behov
import no.nav.dagpenger.regel.api.v1.models.Status
import no.nav.dagpenger.regel.api.v1.models.Subsumsjon
import org.junit.Test
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDate
import kotlin.test.assertEquals

private object PostgresContainer {
    val instance by lazy {
        PostgreSQLContainer<Nothing>("postgres:11.2").apply {
            start()
        }
    }
}

private object DataSource {
    val instance: HikariDataSource by lazy {
        HikariDataSource().apply {
            username = PostgresContainer.instance.username
            password = PostgresContainer.instance.password
            jdbcUrl = PostgresContainer.instance.jdbcUrl
            connectionTimeout = 1000L
        }
    }
}

private fun withCleanDb(test: () -> Unit) = DataSource.instance.also { clean(it) }.run { test() }

private fun withMigratedDb(test: () -> Unit) = DataSource.instance.also { clean(it) }.also { migrate(it) }.run { test() }

class PostgresTest {

    @Test
    fun `Migration scripts are applied successfully`() {
        withCleanDb {
            val migrations = migrate(DataSource.instance)
            assertEquals(2, migrations, "Wrong number of migrations")
        }
    }

    @Test
    fun `Migration scripts are idempotent`() {
        withCleanDb {
            migrate(DataSource.instance)

            val migrations = migrate(DataSource.instance)
            assertEquals(0, migrations, "Wrong number of migrations")
        }
    }

    @Test
    fun `JDBC url is set correctly from  config values `() {
        with(hikariConfigFrom(Configuration())) {
            assertEquals("jdbc:postgresql://localhost:5432/dp-regel-api", jdbcUrl)
        }
    }
}

class PostgresSubsumsjonStoreTest {

    @Test
    fun `Successful insert of behov`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                insertBehov(Behov("BEHOV_ID", "aktorid", 1, LocalDate.now())) shouldBe 1
            }
        }
    }

    @Test
    fun `Store health check UP`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                status() shouldBe HealthStatus.UP
            }
        }
    }

    @Test
    fun `Store health check DOWN`() {
        with(PostgresSubsumsjonStore(HikariDataSource().apply {
            username = PostgresContainer.instance.username
            password = "BAD PASSWORD"
            jdbcUrl = PostgresContainer.instance.jdbcUrl
            connectionTimeout = 1000L
        })) {
            status() shouldBe HealthStatus.DOWN
        }
    }

    @Test
    fun `Status of behov is DONE if the behov exists and a subsumsjon for the behov exists`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                val id = subsumsjon.behovId

                insertBehov(Behov(id, "aktorid", 1, LocalDate.now()))
                insertSubsumsjon(subsumsjon)

                behovStatus(id) shouldBe Status.Done(subsumsjon.id)
            }
        }
    }

    @Test
    fun `Status of behov is pending if the behov exists but no subsumsjon for the behov exists `() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                insertBehov(Behov("id", "aktorid", 1, LocalDate.now()))
                behovStatus("id") shouldBe Status.Pending
            }
        }
    }

    @Test
    fun `Exception if retrieving status of a non existant behov`() {
        withMigratedDb {
            shouldThrow<BehovNotFoundException> {
                PostgresSubsumsjonStore(DataSource.instance).behovStatus("hubba")
            }
        }
    }

    @Test
    fun `Succesful insert of a subsumsjon`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                insertBehov(Behov(subsumsjon.behovId, "aktorid", 1, LocalDate.now()))
                insertSubsumsjon(subsumsjon) shouldBe 1
            }
        }
    }

    @Test
    fun `Do nothing if a subsumsjon allready exist`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                insertBehov(Behov(subsumsjon.behovId, "aktorid", 1, LocalDate.now()))

                insertSubsumsjon(subsumsjon) shouldBe 1
                insertSubsumsjon(subsumsjon) shouldBe 0
            }
        }
    }

    @Test
    fun `Exception on insert of subsumsjon if no correspond behov exists`() {
        withMigratedDb {
            shouldThrow<StoreException> {
                PostgresSubsumsjonStore(DataSource.instance).insertSubsumsjon(mockk(relaxed = true))
            }
        }
    }

    @Test
    fun `Exception if retrieving a non existant subsumsjon`() {
        withMigratedDb {
            shouldThrow<SubsumsjonNotFoundException> {
                PostgresSubsumsjonStore(DataSource.instance).getSubsumsjon("notfound")
            }
        }
    }

    private val subsumsjon = Subsumsjon("id", "BEHOV_ID")


}
