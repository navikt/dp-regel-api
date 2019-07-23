package no.nav.dagpenger.regel.api.db

import com.zaxxer.hikari.HikariDataSource
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.mockk.mockk
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.models.Behov
import no.nav.dagpenger.regel.api.models.Faktum
import no.nav.dagpenger.regel.api.models.Status
import no.nav.dagpenger.regel.api.models.Subsumsjon
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
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

private fun withMigratedDb(test: () -> Unit) =
    DataSource.instance.also { clean(it) }.also { migrate(it) }.run { test() }

class PostgresTest {

    @Test
    fun `Migration scripts are applied successfully`() {
        withCleanDb {
            val migrations = migrate(DataSource.instance)
            assertEquals(7, migrations, "Wrong number of migrations")
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
    fun `Succesful insert and extraction of a subsumsjon`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                insertBehov(Behov(subsumsjon.behovId, "aktorid", 1, LocalDate.now()))
                insertSubsumsjon(subsumsjon) shouldBe 1
                getSubsumsjon(subsumsjon.id) shouldBe subsumsjon
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

    private val subsumsjon = Subsumsjon("subsumsjonId", "behovId", Faktum("aktorId", 1, LocalDate.now()), mapOf(), mapOf(), mapOf(), mapOf(), Problem(title = "problem"))
}

class PostgresBruktSubsumsjonsStoreTest {
    @Test
    fun `successfully inserts BruktSubsumsjon`() {
        withMigratedDb {
            with(PostgresBruktSubsumsjonStore(DataSource.instance)) {
                insertSubsumsjonBrukt(bruktSubsumsjon) shouldBe 1
            }
        }
    }

    @Test
    fun `successfully fetches inserted BruktSubsumsjon`() {
        withMigratedDb {
            with(PostgresBruktSubsumsjonStore(DataSource.instance)) {
                insertSubsumsjonBrukt(bruktSubsumsjon) shouldBe 1
                getSubsumsjonBrukt(bruktSubsumsjon.id)?.arenaTs?.format(secondFormatter) shouldBe exampleDate.format(
                    secondFormatter
                )
            }
        }
    }

    @Test
    fun `trying to insert duplicate ids keeps what's already in the db`() {
        withMigratedDb {
            with(PostgresBruktSubsumsjonStore(DataSource.instance)) {
                insertSubsumsjonBrukt(bruktSubsumsjon) shouldBe 1
                insertSubsumsjonBrukt(bruktSubsumsjon.copy(eksternId = "arena")) shouldBe 0
                getSubsumsjonBrukt(bruktSubsumsjon.id)?.eksternId shouldBe "Arena"
            }
        }
    }

    val secondFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    val oslo = ZoneId.of("Europe/Oslo")
    val exampleDate = ZonedDateTime.now(oslo).minusHours(6)
    private val subsumsjon = Subsumsjon("subsumsjonid", "behovId", Faktum("aktorId", 1, LocalDate.now()), mapOf(), mapOf(), mapOf(), mapOf(), Problem(title = "problem"))
    private val bruktSubsumsjon = SubsumsjonBrukt(subsumsjon.id, "Arena", exampleDate, ts = Instant.now().toEpochMilli())
}
