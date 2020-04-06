package no.nav.dagpenger.regel.api.db

import com.zaxxer.hikari.HikariDataSource
import de.huxhorn.sulky.ulid.ULID
import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.mockk.mockk
import no.finn.unleash.FakeUnleash
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.models.BehandlingsId
import no.nav.dagpenger.regel.api.models.Behov
import no.nav.dagpenger.regel.api.models.BehovId
import no.nav.dagpenger.regel.api.models.Faktum
import no.nav.dagpenger.regel.api.models.InntektsPeriode
import no.nav.dagpenger.regel.api.models.Kontekst
import no.nav.dagpenger.regel.api.models.RegelKontekst
import no.nav.dagpenger.regel.api.models.Status
import no.nav.dagpenger.regel.api.models.Subsumsjon
import no.nav.dagpenger.regel.api.models.SubsumsjonId
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.assertEquals

internal object PostgresContainer {
    val instance by lazy {
        PostgreSQLContainer<Nothing>("postgres:11.2").apply {
            start()
        }
    }
}

internal object DataSource {
    val instance: HikariDataSource by lazy {
        HikariDataSource().apply {
            username = PostgresContainer.instance.username
            password = PostgresContainer.instance.password
            jdbcUrl = PostgresContainer.instance.jdbcUrl
            connectionTimeout = 1000L
        }
    }
}

internal fun withCleanDb(test: () -> Unit) = DataSource.instance.also { clean(it) }.run { test() }

internal fun withMigratedDb(test: () -> Unit) =
    DataSource.instance.also { clean(it) }.also { migrate(it) }.run { test() }

internal class PostgresTest {

    @Test
    fun `Migration scripts are applied successfully`() {
        withCleanDb {
            val migrations = migrate(DataSource.instance)
            assertEquals(14, migrations, "Wrong number of migrations")
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
    fun `Successful opprett of behov`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                val behov = Behov(
                    aktørId = "1234",
                    vedtakId = 1234,
                    beregningsDato = LocalDate.now(),
                    antallBarn = 1,
                    manueltGrunnlag = 11,
                    harAvtjentVerneplikt = false,
                    bruktInntektsPeriode = InntektsPeriode(
                        YearMonth.now().minusMonths(12),
                        YearMonth.now()
                    )
                )
                opprettBehov(behov, FakeUnleash())
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

                val internBehov = opprettBehov(
                    Behov(aktørId = "aktorid", vedtakId = 1, beregningsDato = LocalDate.now()),
                    FakeUnleash()
                )
                val sub = subsumsjon.copy(behovId = internBehov.behovId)
                insertSubsumsjon(sub)
                behovStatus(internBehov.behovId) shouldBe Status.Done(sub.behovId)
            }
        }
    }

    @Test
    fun `Status of behov is pending if the behov exists but no subsumsjon for the behov exists `() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                val internBehov = opprettBehov(
                    Behov(aktørId = "aktorid", vedtakId = 1, beregningsDato = LocalDate.now()),
                    FakeUnleash()
                )
                behovStatus(internBehov.behovId) shouldBe Status.Pending
            }
        }
    }

    @Test
    fun `Exception if retrieving status of a non existant behov`() {
        withMigratedDb {
            shouldThrow<BehovNotFoundException> {
                PostgresSubsumsjonStore(DataSource.instance).behovStatus(BehovId("01DSFGT6XCX4W1RKDXBYTAX5QH"))
            }
        }
    }

    @Test
    fun `Successful insert and extraction of a subsumsjon`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {

                val internBehov = opprettBehov(
                    Behov(aktørId = "aktorid", vedtakId = 1, beregningsDato = LocalDate.now()),
                    FakeUnleash()
                )
                val sub = subsumsjon.copy(behovId = internBehov.behovId)
                insertSubsumsjon(sub) shouldBe 1
                getSubsumsjon(BehovId(sub.behovId.id)) shouldBe sub
            }
        }
    }

    @Test
    fun `Do nothing if a subsumsjon already exist`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                val internBehov = opprettBehov(
                    Behov(aktørId = "aktorid", vedtakId = 1, beregningsDato = LocalDate.now()),
                    FakeUnleash()
                )
                val sub = subsumsjon.copy(behovId = internBehov.behovId)

                insertSubsumsjon(sub) shouldBe 1
                insertSubsumsjon(sub) shouldBe 0
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
                PostgresSubsumsjonStore(DataSource.instance).getSubsumsjon(BehovId("01DSFHD74S4DGSXYD8QFQ6RY02"))
            }
        }
    }

    @Test
    fun ` Should be able to get subsumsjon based on specific subsumsjon result id`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                val minsteinntektId = ULID().nextULID()
                val satsId = ULID().nextULID()
                val grunnlagId = ULID().nextULID()
                val periodeId = ULID().nextULID()
                val internBehov = opprettBehov(
                    Behov(aktørId = "aktorid", vedtakId = 1, beregningsDato = LocalDate.now()),
                    FakeUnleash()
                )
                val subsumsjonWithResults = subsumsjon.copy(
                    behovId = internBehov.behovId,
                    minsteinntektResultat = mapOf("subsumsjonsId" to minsteinntektId),
                    satsResultat = mapOf("subsumsjonsId" to satsId),
                    grunnlagResultat = mapOf("subsumsjonsId" to grunnlagId),
                    periodeResultat = mapOf("subsumsjonsId" to periodeId)
                )

                insertSubsumsjon(subsumsjonWithResults) shouldBe 1

                assertSoftly {
                    getSubsumsjonByResult(SubsumsjonId(minsteinntektId)) shouldBe subsumsjonWithResults
                    getSubsumsjonByResult(SubsumsjonId(grunnlagId)) shouldBe subsumsjonWithResults
                    getSubsumsjonByResult(SubsumsjonId(satsId)) shouldBe subsumsjonWithResults
                    getSubsumsjonByResult(SubsumsjonId(periodeId)) shouldBe subsumsjonWithResults
                }
            }
        }
    }

    @Test
    fun ` Should throw not found exception if we not are able to get subsumsjon based on specific subsumsjon result id`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                val internBehov = opprettBehov(
                    Behov(aktørId = "aktorid", vedtakId = 1, beregningsDato = LocalDate.now()),
                    FakeUnleash()
                )
                val subsumsjonWithResults = subsumsjon.copy(
                    behovId = internBehov.behovId,
                    minsteinntektResultat = mapOf("subsumsjonsId" to ULID().nextULID()),
                    satsResultat = mapOf("subsumsjonsId" to ULID().nextULID()),
                    grunnlagResultat = mapOf("subsumsjonsId" to ULID().nextULID()),
                    periodeResultat = mapOf("subsumsjonsId" to ULID().nextULID())
                )

                insertSubsumsjon(subsumsjonWithResults) shouldBe 1

                assertThrows<SubsumsjonNotFoundException> { getSubsumsjonByResult(SubsumsjonId(ULID().nextULID())) }
            }
        }
    }

    @Test
    fun `Should generate new intern id for ekstern id`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                val eksternId = RegelKontekst("1234", Kontekst.VEDTAK)
                val behandlingsId: BehandlingsId = opprettKoblingTilRegelkontekst(eksternId)
                ULID.parseULID(behandlingsId.id)
            }
        }
    }

    @Test
    fun `Should not generate new intern id for already existing ekstern id`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                val eksternId = RegelKontekst("1234", Kontekst.VEDTAK)
                val behandlingsId1: BehandlingsId? = hentKoblingTilRegelKontekst(eksternId)
                val behandlingsId2: BehandlingsId? = hentKoblingTilRegelKontekst(eksternId)
                behandlingsId1 shouldBe behandlingsId2
            }
        }
    }

    private val subsumsjon = Subsumsjon(
        behovId = BehovId("01DSFST7S8HCXHRASYP9PC197W"),
        faktum = Faktum("aktorId", 1, LocalDate.now()),
        grunnlagResultat = emptyMap(),
        minsteinntektResultat = emptyMap(),
        periodeResultat = emptyMap(),
        satsResultat = emptyMap(),
        problem = Problem(title = "problem")
    )
}
