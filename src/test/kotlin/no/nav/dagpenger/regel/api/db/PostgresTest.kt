package no.nav.dagpenger.regel.api.db

import com.zaxxer.hikari.HikariDataSource
import de.huxhorn.sulky.ulid.ULID
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.mockk.mockk
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.models.Behov
import no.nav.dagpenger.regel.api.models.EksternId
import no.nav.dagpenger.regel.api.models.Faktum
import no.nav.dagpenger.regel.api.models.InntektsPeriode
import no.nav.dagpenger.regel.api.models.InternId
import no.nav.dagpenger.regel.api.models.Kontekst
import no.nav.dagpenger.regel.api.models.Status
import no.nav.dagpenger.regel.api.models.Subsumsjon
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
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
            assertEquals(10, migrations, "Wrong number of migrations")
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
                opprettBehov(behov)
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

                val internBehov = opprettBehov(Behov("aktorid", 1, LocalDate.now()))
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
                val internBehov = opprettBehov(Behov("aktorid", 1, LocalDate.now()))
                behovStatus(internBehov.behovId) shouldBe Status.Pending
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
    fun `Successful insert and extraction of a subsumsjon`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {

                val internBehov = opprettBehov(Behov("aktorid", 1, LocalDate.now()))
                val sub = subsumsjon.copy(behovId = internBehov.behovId)
                insertSubsumsjon(sub) shouldBe 1
                getSubsumsjon(sub.behovId) shouldBe sub
            }
        }
    }

    @Test
    fun `Do nothing if a subsumsjon already exist`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                val internBehov = opprettBehov(Behov("aktorid", 1, LocalDate.now()))
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
                PostgresSubsumsjonStore(DataSource.instance).getSubsumsjon("notfound")
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
                val internBehov = opprettBehov(Behov("aktorid", 1, LocalDate.now()))
                val subsumsjonWithResults = subsumsjon.copy(
                    behovId = internBehov.behovId,
                    minsteinntektResultat = mapOf("subsumsjonsId" to minsteinntektId),
                    satsResultat = mapOf("subsumsjonsId" to satsId),
                    grunnlagResultat = mapOf("subsumsjonsId" to grunnlagId),
                    periodeResultat = mapOf("subsumsjonsId" to periodeId)
                )

                insertSubsumsjon(subsumsjonWithResults) shouldBe 1
                getSubsumsjonByResult(SubsumsjonId(minsteinntektId)) shouldBe subsumsjonWithResults
                getSubsumsjonByResult(SubsumsjonId(grunnlagId)) shouldBe subsumsjonWithResults
                getSubsumsjonByResult(SubsumsjonId(satsId)) shouldBe subsumsjonWithResults
                getSubsumsjonByResult(SubsumsjonId(periodeId)) shouldBe subsumsjonWithResults
            }
        }
    }

    @Test
    fun `Can convert a row from v1_behov to InternBehov `() {
        val inntektsPeriode =
            InntektsPeriode(førsteMåned = YearMonth.now().minusMonths(3), sisteMåned = YearMonth.now())
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                val behov = Behov(
                    aktørId = "1",
                    vedtakId = 1,
                    beregningsDato = LocalDate.now(),
                    harAvtjentVerneplikt = true,
                    oppfyllerKravTilFangstOgFisk = true,
                    bruktInntektsPeriode = inntektsPeriode,
                    antallBarn = 2,
                    manueltGrunnlag = 124
                )
                val behovId = ULID().nextULID()
                insertBehovV1(behov, behovId)
                val internBehov = hentBehovV1TilInternBehov(behovId)
                internBehov.size shouldBe 1
                val konvertertBehov = internBehov.first()
                konvertertBehov.aktørId shouldBe "1"
                konvertertBehov.behovId shouldBe behovId
                konvertertBehov.internId.eksternId.id shouldBe "1"
                konvertertBehov.bruktInntektsPeriode shouldBe inntektsPeriode
            }
        }
    }

    @Test
    fun `Should fetch all behov`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                val behov = Behov(
                    aktørId = "1",
                    vedtakId = 1,
                    beregningsDato = LocalDate.now(),
                    harAvtjentVerneplikt = true,
                    oppfyllerKravTilFangstOgFisk = true,
                    bruktInntektsPeriode = InntektsPeriode(førsteMåned = YearMonth.now().minusMonths(3), sisteMåned = YearMonth.now()),
                    antallBarn = 2,
                    manueltGrunnlag = 124
                )
                insertBehovV1(behov)
                val behov2 = behov.copy(vedtakId = 2)
                insertBehovV1(behov2)
                val behov3 = behov.copy(vedtakId = 3)
                insertBehovV1(behov3)
                val behov4 = behov.copy(vedtakId = 4)
                insertBehovV1(behov4)
                val internBehov = hentBehovV1TilInternBehov()
                internBehov.size shouldBe 4
                internBehov[3].internId.eksternId.id shouldBe "4"
            }
        }
    }

    @Test
    fun `Should handle migrate of data from v1_behov to v2_behov`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                val behov = Behov(
                    aktørId = "1",
                    vedtakId = 1,
                    beregningsDato = LocalDate.now(),
                    harAvtjentVerneplikt = true,
                    oppfyllerKravTilFangstOgFisk = true,
                    bruktInntektsPeriode = InntektsPeriode(førsteMåned = YearMonth.now().minusMonths(3), sisteMåned = YearMonth.now()),
                    antallBarn = 2,
                    manueltGrunnlag = 124
                )
                insertBehovV1(behov)
                val behov2 = behov.copy(vedtakId = 2)
                insertBehovV1(behov2)
                val behov3 = behov.copy(vedtakId = 3)
                insertBehovV1(behov3)
                val behov4 = behov.copy(vedtakId = 4)
                insertBehovV1(behov4)
                val internBehov = hentBehovV1TilInternBehov()

                migrerBehovV1TilV2()

                internBehov.forEach {
                    behovStatus(it.behovId) shouldBe Status.Pending
                }
            }
        }
    }

    @Test
    fun `Should handle partial migrations of data from v1_behov to v2_behov`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                val behov = Behov(
                    aktørId = "1",
                    vedtakId = 1,
                    beregningsDato = LocalDate.now(),
                    harAvtjentVerneplikt = true,
                    oppfyllerKravTilFangstOgFisk = true,
                    bruktInntektsPeriode = InntektsPeriode(førsteMåned = YearMonth.now().minusMonths(3), sisteMåned = YearMonth.now()),
                    antallBarn = 2,
                    manueltGrunnlag = 124
                )
                insertBehovV1(behov)
                val behov2 = behov.copy(vedtakId = 2)
                insertBehovV1(behov2)

                migrerBehovV1TilV2()

                val behov3 = behov.copy(vedtakId = 3)
                insertBehovV1(behov3)
                val behov4 = behov.copy(vedtakId = 4)
                insertBehovV1(behov4)

                migrerBehovV1TilV2()

                val internBehov = hentBehovV1TilInternBehov()

                internBehov.forEach {
                    behovStatus(it.behovId) shouldBe Status.Pending
                }
            }
        }
    }

    @Test
    fun `Should preserve ids from v1_behov to v2_behov`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                val behov = Behov(
                    aktørId = "1",
                    vedtakId = 1,
                    beregningsDato = LocalDate.now(),
                    harAvtjentVerneplikt = true,
                    oppfyllerKravTilFangstOgFisk = true,
                    bruktInntektsPeriode = InntektsPeriode(
                        førsteMåned = YearMonth.now().minusMonths(3),
                        sisteMåned = YearMonth.now()
                    ),
                    antallBarn = 2,
                    manueltGrunnlag = 124
                )
                insertBehovV1(behov)
                val behov2 = behov.copy(vedtakId = 2)
                insertBehovV1(behov2)
                val behov3 = behov.copy(vedtakId = 3)
                insertBehovV1(behov3)
                val behov4 = behov.copy(vedtakId = 4)
                insertBehovV1(behov4)

                migrerBehovV1TilV2()
                using(sessionOf(DataSource.instance)) { s ->
                    val v1Ids =
                        s.run(queryOf("""SELECT id FROM v1_behov""", emptyMap()).map { r -> r.string("id") }.asList)
                            .sorted()
                    val v2Ids = s.run(
                        queryOf(
                            """SELECT id FROM v2_behov""",
                            emptyMap()
                        ).map { r -> r.string("id") }.asList
                    ).sorted()
                    v1Ids shouldBe v2Ids
                }
            }
        }
    }

    @Test
    fun `Should handle migrate of data from v1_subsumsjon to v2_subsumsjon`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                val behov = Behov(
                        aktørId = "1",
                        vedtakId = 1,
                        beregningsDato = LocalDate.now(),
                        harAvtjentVerneplikt = true,
                        oppfyllerKravTilFangstOgFisk = true,
                        bruktInntektsPeriode = InntektsPeriode(førsteMåned = YearMonth.now().minusMonths(3), sisteMåned = YearMonth.now()),
                        antallBarn = 2,
                        manueltGrunnlag = 124
                )

                val subsumsjon = Subsumsjon(
                        behovId = "placeholder",
                        faktum = Faktum(
                                aktorId = behov.aktørId,
                                vedtakId = behov.vedtakId,
                                beregningsdato = behov.beregningsDato
                        ),
                        grunnlagResultat = emptyMap(),
                        satsResultat = emptyMap(),
                        periodeResultat = emptyMap(),
                        minsteinntektResultat = emptyMap(),
                        problem = null
                )

                insertBehovV1(behov)
                val behov2 = behov.copy(vedtakId = 2)
                insertBehovV1(behov2)
                val behov3 = behov.copy(vedtakId = 3)
                insertBehovV1(behov3)
                val behov4 = behov.copy(vedtakId = 4)
                insertBehovV1(behov4)

                val internBehov = hentBehovV1TilInternBehov()

                internBehov.forEach {
                    insertSubumsjonV1(
                            subsumsjon.copy(behovId = it.behovId)
                    )
                }

                migrerBehovV1TilV2()

                internBehov.forEach {
                    behovStatus(it.behovId) shouldBe Status.Pending
                }

                migrerSubsumsjonV1TilV2()

                internBehov.forEach {
                    behovStatus(it.behovId) shouldBe Status.Done(it.behovId)
                }

                internBehov.forEach {
                    getSubsumsjon(it.behovId) shouldBe subsumsjon.copy(behovId = it.behovId)
                }
            }
        }
    }

    @Test
    fun `Should generate new intern id for ekstern id`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                val eksternId = EksternId("1234", Kontekst.VEDTAK)
                val internId: InternId = hentKoblingTilEkstern(eksternId)
                ULID.parseULID(internId.id)
            }
        }
    }

    @Test
    fun `Should not generate new intern id for already existing ekstern id`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                val eksternId = EksternId("1234", Kontekst.VEDTAK)
                val internId1: InternId = hentKoblingTilEkstern(eksternId)
                val internId2: InternId = hentKoblingTilEkstern(eksternId)
                internId1 shouldBe internId2
            }
        }
    }

    private val subsumsjon = Subsumsjon(
        behovId = "behovId",
        faktum = Faktum("aktorId", 1, LocalDate.now()),
        grunnlagResultat = emptyMap(),
        minsteinntektResultat = emptyMap(),
        periodeResultat = emptyMap(),
        satsResultat = emptyMap(),
        problem = Problem(title = "problem")
    )
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
    private val subsumsjon = Subsumsjon(
        behovId = "behovId",
        faktum = Faktum("aktorId", 1, LocalDate.now()),
        grunnlagResultat = emptyMap(),
        minsteinntektResultat = emptyMap(),
        periodeResultat = emptyMap(),
        satsResultat = emptyMap(),
        problem = Problem(title = "problem")
    )
    private val bruktSubsumsjon =
        SubsumsjonBrukt(subsumsjon.behovId, "Arena", exampleDate, ts = Instant.now().toEpochMilli())
}
