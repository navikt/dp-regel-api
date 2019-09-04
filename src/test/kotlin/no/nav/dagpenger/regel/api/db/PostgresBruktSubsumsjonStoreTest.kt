package no.nav.dagpenger.regel.api.db

import com.zaxxer.hikari.HikariDataSource
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.regel.api.models.EksternId
import no.nav.dagpenger.regel.api.models.Faktum
import no.nav.dagpenger.regel.api.models.Kontekst
import no.nav.dagpenger.regel.api.models.Subsumsjon
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class PostgresBruktSubsumsjonStoreTest {
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

    @Test
    fun `successfully inserts BruktSubsumsjon`() {
        withMigratedDb {
            with(PostgresBruktSubsumsjonStore(dataSource = DataSource.instance)) {
                insertSubsumsjonBrukt(bruktSubsumsjon) shouldBe 1
            }
        }
    }

    @Test
    fun `inserting v2 also works`() {
        withMigratedDb {
            with(PostgresBruktSubsumsjonStore(dataSource = DataSource.instance)) {
                val bruktSubsumsjonV2 = SubsumsjonBruktV2(
                    id = subsumsjon.behovId,
                    behandlingsId = PostgresSubsumsjonStore(DataSource.instance).hentKoblingTilEkstern(eksternId).id,
                    arenaTs = exampleDate)
                insertSubsumsjonBruktV2(subsumsjonBruktV2 = bruktSubsumsjonV2)
                val savedBruktSub = getSubsumsjonBruktV2(bruktSubsumsjonV2.id)
                savedBruktSub!!.created shouldNotBe null
                savedBruktSub.created!!.toLocalDate() shouldBe LocalDate.now()
            }
        }
    }

    @Test
    fun `successfully fetches inserted BruktSubsumsjon`() {
        withMigratedDb {
            with(PostgresBruktSubsumsjonStore(dataSource = DataSource.instance)) {
                insertSubsumsjonBrukt(bruktSubsumsjon) shouldBe 1
                getSubsumsjonBrukt(bruktSubsumsjon.id)?.arenaTs?.format(secondFormatter) shouldBe exampleDate.format(
                    secondFormatter
                )
            }
        }
    }

    @Test
    fun `successfully fetches inserted BruktSubsumsjonV2`() {
        withMigratedDb {
            with(PostgresBruktSubsumsjonStore(dataSource = DataSource.instance)) {
                val v2Subsumsjon = v1TilV2(bruktSubsumsjon)
                insertSubsumsjonBruktV2(v2Subsumsjon)
                getSubsumsjonBruktV2(bruktSubsumsjon.id)?.arenaTs?.format(secondFormatter) shouldBe exampleDate.format(
                    secondFormatter
                )
            }
        }
    }

    @Test
    fun `trying to insert duplicate ids keeps what's already in the db`() {
        withMigratedDb {
            with(
                PostgresBruktSubsumsjonStore(
                    dataSource = DataSource.instance,
                    subsumsjonStore = PostgresSubsumsjonStore(DataSource.instance)
                )
            ) {
                insertSubsumsjonBrukt(bruktSubsumsjon) shouldBe 1
                insertSubsumsjonBrukt(bruktSubsumsjon.copy(eksternId = 21312312)) shouldBe 0
                getSubsumsjonBrukt(bruktSubsumsjon.id)?.eksternId shouldBe bruktSubsumsjon.eksternId
            }
        }
    }

    @Test
    fun `migration from v1 to v2 goes ok`() {
        withMigratedDb {
            with(
                PostgresBruktSubsumsjonStore(
                    dataSource = DataSource.instance,
                    subsumsjonStore = PostgresSubsumsjonStore(DataSource.instance)
                )
            ) {
                val eksternId = subsumsjonStore.hentKoblingTilEkstern(
                    EksternId(
                        id = bruktSubsumsjon.eksternId.toString(),
                        kontekst = Kontekst.VEDTAK
                    )
                )
                insertSubsumsjonBrukt(bruktSubsumsjon) shouldBe 1
                migrerV1TilV2()
                val subV2 = getSubsumsjonBruktV2(bruktSubsumsjon.id)
                subV2 shouldNotBe null
                subV2!!.behandlingsId shouldBe eksternId.id
            }
        }
    }

    @Test
    fun `Should be able to run migration multiple times without conflicts`() {
        withMigratedDb {
            with(
                PostgresBruktSubsumsjonStore(
                    dataSource = DataSource.instance,
                    subsumsjonStore = PostgresSubsumsjonStore(DataSource.instance)
                )
            ) {
                insertSubsumsjonBrukt(bruktSubsumsjon)
                migrerV1TilV2()
                var allSubsumsjonV2 = listSubsumsjonBruktV2()
                allSubsumsjonV2.size shouldBe 1
                insertSubsumsjonBrukt(bruktSubsumsjon.copy(id = "behovId2", eksternId = 1232121, arenaTs = ZonedDateTime.now().minusDays(3)))
                migrerV1TilV2()
                allSubsumsjonV2 = listSubsumsjonBruktV2()
                allSubsumsjonV2.size shouldBe 2
            }
        }
    }

    val secondFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    val oslo = ZoneId.of("Europe/Oslo")
    val exampleDate = ZonedDateTime.now(oslo).minusHours(6)
    val subsumsjon = Subsumsjon(
        behovId = "behovId",
        faktum = Faktum("aktorId", 1, LocalDate.now()),
        grunnlagResultat = emptyMap(),
        minsteinntektResultat = emptyMap(),
        periodeResultat = emptyMap(),
        satsResultat = emptyMap(),
        problem = Problem(title = "problem")
    )
    val eksternId = EksternId(id = "1234", kontekst = Kontekst.VEDTAK)
    val bruktSubsumsjon =
        SubsumsjonBrukt(id = subsumsjon.behovId, eksternId = 1231231, arenaTs = exampleDate, ts = Instant.now().toEpochMilli())
}