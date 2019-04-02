package no.nav.dagpenger.regel.api.db

import com.zaxxer.hikari.HikariDataSource
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.Regel
import no.nav.dagpenger.regel.api.Status
import no.nav.dagpenger.regel.api.SubsumsjonsBehov
import no.nav.dagpenger.regel.api.models.PeriodeFaktum
import no.nav.dagpenger.regel.api.models.PeriodeResultat
import no.nav.dagpenger.regel.api.models.PeriodeSubsumsjon
import org.junit.Test
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDate
import java.time.LocalDateTime
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
            assertEquals(1, migrations, "Wrong number of migrations")
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
                insertBehov(SubsumsjonsBehov("behovId", "aktorid", 1, LocalDate.now()))

                behovStatus("behovId") shouldBe Status.Pending
            }
        }
    }

    @Test
    fun `Exception if behov status is not found`() {
        withMigratedDb {
            shouldThrow<BehovNotFoundException> {
                PostgresSubsumsjonStore(DataSource.instance).behovStatus("behovId") shouldBe Status.Pending
            }
        }
    }

    @Test
    fun `Succesful insert of a subsumsjon is retrievable and updates behov to status done`() {

        val behov = mockk<SubsumsjonsBehov>(relaxed = true).apply {
            every { this@apply.behovId } returns "behovId"
        }

        val subsumsjon = PeriodeSubsumsjon("subsumsjonsId", "behovId", Regel.PERIODE, LocalDateTime.now(), LocalDateTime.now(),
            PeriodeFaktum("aktorId", 1, LocalDate.now(), "inntektsId"),
            PeriodeResultat(1))

        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {

                insertBehov(behov)
                insertSubsumsjon(subsumsjon)

                getSubsumsjon("subsumsjonsId") shouldBe subsumsjon
                behovStatus("behovId") shouldBe Status.Done("subsumsjonsId")
            }
        }
    }

    @Test
    fun `Do nothing if a subsumsjon allready exist`() {

        val behov = mockk<SubsumsjonsBehov>(relaxed = true).apply {
            every { this@apply.behovId } returns "behovId"
        }

        val subsumsjon = PeriodeSubsumsjon("subsumsjonsId", "behovId", Regel.PERIODE, LocalDateTime.now(), LocalDateTime.now(),
            PeriodeFaktum("aktorId", 1, LocalDate.now(), "inntektsId"),
            PeriodeResultat(1))

        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {

                insertBehov(behov)
                insertSubsumsjon(subsumsjon)
                insertSubsumsjon(subsumsjon)

                getSubsumsjon("subsumsjonsId") shouldBe subsumsjon
                behovStatus("behovId") shouldBe Status.Done("subsumsjonsId")
            }
        }
    }

    @Test
    fun `Exception on insert of subsumsjon if no correspond behov exists`() {
        withMigratedDb {
            shouldThrow<StoreException> {
                PostgresSubsumsjonStore(DataSource.instance).insertSubsumsjon(mockk<PeriodeSubsumsjon>(relaxed = true))
            }
        }
    }

    @Test
    fun `Exception if retrieving a non existant subsumsjon`() {
        withMigratedDb {
            shouldThrow<SubsumsjonNotFoundException> {
                PostgresSubsumsjonStore(DataSource.instance).getSubsumsjon("notexist")
            }
        }
    }
}
