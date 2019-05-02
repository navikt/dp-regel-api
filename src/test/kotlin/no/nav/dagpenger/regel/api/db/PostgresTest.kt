package no.nav.dagpenger.regel.api.db

import com.zaxxer.hikari.HikariDataSource
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.mockk.mockk
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.Regel
import no.nav.dagpenger.regel.api.Status
import no.nav.dagpenger.regel.api.SubsumsjonsBehov
import no.nav.dagpenger.regel.api.models.PeriodeFaktum
import no.nav.dagpenger.regel.api.models.PeriodeResultat
import no.nav.dagpenger.regel.api.models.PeriodeSubsumsjon
import no.nav.dagpenger.regel.api.models.SatsFaktum
import no.nav.dagpenger.regel.api.models.SatsResultat
import no.nav.dagpenger.regel.api.models.SatsSubsumsjon
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
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
                insertBehov(SubsumsjonsBehov("BEHOV_ID", "aktorid", 1, LocalDate.now()), Regel.SATS) shouldBe 1
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
    fun `Status of behov`() {
        val subsumsjon = periodeSubsumsjon()

        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                insertBehov(SubsumsjonsBehov(subsumsjon.behovId, "aktorid", 1, LocalDate.now()), Regel.SATS)
                insertSubsumsjon(subsumsjon)

                behovStatus(subsumsjon.behovId, Regel.PERIODE) shouldBe Status.Done(subsumsjon.subsumsjonsId)
                behovStatus("notexist", Regel.PERIODE) shouldBe Status.Pending
                behovStatus(subsumsjon.behovId, Regel.SATS) shouldBe Status.Pending
            }
        }
    }

    @Test
    fun `Succesful insert of a subsumsjon`() {
        val subsumsjon = periodeSubsumsjon()
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                insertBehov(SubsumsjonsBehov(subsumsjon.behovId, "aktorid", 1, LocalDate.now()), Regel.SATS)

                insertSubsumsjon(subsumsjon) shouldBe 1
            }
        }
    }

    @Test
    fun `Do nothing if a subsumsjon allready exist`() {

        val subsumsjon = periodeSubsumsjon()

        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                insertBehov(SubsumsjonsBehov(subsumsjon.behovId, "aktorid", 1, LocalDate.now()), Regel.SATS)

                insertSubsumsjon(subsumsjon) shouldBe 1
                insertSubsumsjon(subsumsjon) shouldBe 0
            }
        }
    }

    @Test
    fun `Retrieve subsumsjon on composite primary key`() {

        val periodeSubsumsjon = PeriodeSubsumsjon("id", "BEHOV_ID", Regel.PERIODE, LocalDateTime.now(), LocalDateTime.now(),
            PeriodeFaktum("aktorId", 1, LocalDate.now(), "inntektsId"),
            PeriodeResultat(1))

        val satsSubsumsjon = SatsSubsumsjon("id", "BEHOV_ID", Regel.SATS, LocalDateTime.now(), LocalDateTime.now(), SatsFaktum("aktorId", 1, LocalDate.now()),
            SatsResultat(10, 10, true))

        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                insertBehov(SubsumsjonsBehov("BEHOV_ID", "aktorid", 1, LocalDate.now()), Regel.SATS)

                insertSubsumsjon(periodeSubsumsjon)
                insertSubsumsjon(satsSubsumsjon)

                getSubsumsjon("id", Regel.PERIODE) shouldBe periodeSubsumsjon
                getSubsumsjon("id", Regel.SATS) shouldBe satsSubsumsjon
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
        val subsumsjon = periodeSubsumsjon()

        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                insertBehov(SubsumsjonsBehov(subsumsjon.behovId, "aktorid", 1, LocalDate.now()), Regel.SATS)
                insertSubsumsjon(subsumsjon)

                shouldThrow<SubsumsjonNotFoundException> {
                    PostgresSubsumsjonStore(DataSource.instance).getSubsumsjon("notfound", Regel.PERIODE)
                }

                shouldThrow<SubsumsjonNotFoundException> {
                    PostgresSubsumsjonStore(DataSource.instance).getSubsumsjon(subsumsjon.subsumsjonsId, Regel.SATS)
                }
            }
        }
    }

    private fun periodeSubsumsjon(): PeriodeSubsumsjon {
        val subsumsjon = PeriodeSubsumsjon("subsumsjonsId", "BEHOV_ID", Regel.PERIODE, LocalDateTime.now(), LocalDateTime.now(),
            PeriodeFaktum("aktorId", 1, LocalDate.now(), "inntektsId"),
            PeriodeResultat(1))
        return subsumsjon
    }
}
