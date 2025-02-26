package no.nav.dagpenger.regel.api.db

import com.zaxxer.hikari.HikariDataSource
import de.huxhorn.sulky.ulid.ULID
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import io.mockk.mockk
import io.prometheus.client.CollectorRegistry
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.regel.api.db.PostgresTestSetup.withCleanDb
import no.nav.dagpenger.regel.api.db.PostgresTestSetup.withMigratedDb
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
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.assertEquals

internal class PostgresTest {
    @Test
    fun `Migration scripts are applied successfully`() {
        withCleanDb {
            val migrations = PostgresDataSourceBuilder.runMigration()
            assertEquals(18, migrations, "Wrong number of migrations")
        }
    }

    @Test
    fun `Migration scripts are idempotent`() {
        withCleanDb {
            PostgresDataSourceBuilder.runMigration()

            val migrations = PostgresDataSourceBuilder.runMigration()
            assertEquals(0, migrations, "Wrong number of migrations")
        }
    }
}

class PostgresSubsumsjonStoreTest {
    @Test
    fun `Lagre behov`() {
        withMigratedDb { dataSource ->
            with(PostgresSubsumsjonStore(dataSource)) {
                val behov =
                    Behov(
                        aktørId = "1234",
                        regelkontekst = RegelKontekst("1234", Kontekst.vedtak),
                        beregningsDato = LocalDate.now(),
                        antallBarn = 1,
                        manueltGrunnlag = 11,
                        forrigeGrunnlag = 234,
                        harAvtjentVerneplikt = false,
                        lærling = false,
                        bruktInntektsPeriode =
                            InntektsPeriode(
                                YearMonth.now().minusMonths(12),
                                YearMonth.now(),
                            ),
                        regelverksdato = LocalDate.of(2020, 1, 1),
                    )
                val lagretInternBehov = getBehov(opprettBehov(behov).behovId)

                with(behov) {
                    lagretInternBehov.aktørId shouldBe aktørId
                    lagretInternBehov.beregningsDato shouldBe beregningsDato
                    lagretInternBehov.antallBarn shouldBe antallBarn
                    lagretInternBehov.manueltGrunnlag shouldBe manueltGrunnlag
                    lagretInternBehov.forrigeGrunnlag shouldBe forrigeGrunnlag
                    lagretInternBehov.harAvtjentVerneplikt shouldBe harAvtjentVerneplikt
                    lagretInternBehov.lærling shouldBe lærling
                    lagretInternBehov.bruktInntektsPeriode shouldBe bruktInntektsPeriode
                    lagretInternBehov.regelverksdato shouldBe regelverksdato
                }
            }
        }
    }

    @Test
    fun `Store health check UP`() {
        withMigratedDb { dataSource ->
            with(PostgresSubsumsjonStore(dataSource)) {
                status() shouldBe HealthStatus.UP
            }
        }
    }

    @Test
    fun `Store health check DOWN`() {
        withMigratedDb { _ ->
            PostgresSubsumsjonStore(
                HikariDataSource().apply {
                    username = PostgresTestSetup.instance.username
                    password = "BAD PASSWORD"
                    jdbcUrl = PostgresTestSetup.instance.jdbcUrl
                    connectionTimeout = 1000L
                },
            ).status() shouldBe HealthStatus.DOWN
        }
    }

    @Test
    fun `Status of behov is DONE if the behov exists and a subsumsjon for the behov exists`() {
        withMigratedDb { dataSource ->
            with(PostgresSubsumsjonStore(dataSource)) {
                val internBehov =
                    opprettBehov(
                        Behov(
                            aktørId = "aktorid",
                            regelkontekst = RegelKontekst("1", Kontekst.vedtak),
                            beregningsDato = LocalDate.now(),
                        ),
                    )
                val sub = subsumsjon.copy(behovId = internBehov.behovId)
                insertSubsumsjon(sub)
                behovStatus(internBehov.behovId) shouldBe Status.Done(sub.behovId)
            }
        }
    }

    @Test
    fun `Status of behov is pending if the behov exists but no subsumsjon for the behov exists `() {
        withMigratedDb { dataSource ->
            with(PostgresSubsumsjonStore(dataSource)) {
                val internBehov =
                    opprettBehov(
                        Behov(
                            aktørId = "aktorid",
                            regelkontekst = RegelKontekst("1", Kontekst.vedtak),
                            beregningsDato = LocalDate.now(),
                        ),
                    )
                behovStatus(internBehov.behovId) shouldBe Status.Pending
            }
        }
    }

    @Test
    fun `Exception if retrieving status of a non existant behov`() {
        withMigratedDb { dataSource ->
            shouldThrow<BehovNotFoundException> {
                PostgresSubsumsjonStore(PostgresDataSourceBuilder.dataSource).behovStatus(BehovId("01DSFGT6XCX4W1RKDXBYTAX5QH"))
            }
        }
    }

    @Test
    fun `Successful insert and extraction of a subsumsjon`() {
        withMigratedDb { dataSource ->
            with(PostgresSubsumsjonStore(dataSource)) {
                val internBehov =
                    opprettBehov(
                        Behov(
                            aktørId = "aktorid",
                            regelkontekst = RegelKontekst("1", Kontekst.vedtak),
                            beregningsDato = LocalDate.now(),
                        ),
                    )
                val sub = subsumsjon.copy(behovId = internBehov.behovId)
                insertSubsumsjon(sub) shouldBe 1
                getSubsumsjon(BehovId(sub.behovId.id)) shouldBe sub
            }
        }
    }

    @Test
    fun `Do nothing if a subsumsjon already exist`() {
        withMigratedDb { dataSource ->
            with(PostgresSubsumsjonStore(dataSource)) {
                val internBehov =
                    opprettBehov(
                        Behov(
                            aktørId = "aktorid",
                            regelkontekst = RegelKontekst("1", Kontekst.vedtak),
                            beregningsDato = LocalDate.now(),
                        ),
                    )
                val sub = subsumsjon.copy(behovId = internBehov.behovId)

                insertSubsumsjon(sub) shouldBe 1
                insertSubsumsjon(sub) shouldBe 0
            }
        }
    }

    @Test
    fun `Exception on insert of subsumsjon if no correspond behov exists`() {
        withMigratedDb { dataSource ->
            shouldThrow<StoreException> {
                PostgresSubsumsjonStore(dataSource).insertSubsumsjon(mockk(relaxed = true))
            }
        }
    }

    @Test
    fun `Exception if retrieving a non existant subsumsjon`() {
        withMigratedDb { dataSource ->
            shouldThrow<SubsumsjonNotFoundException> {
                PostgresSubsumsjonStore(dataSource).getSubsumsjon(BehovId("01DSFHD74S4DGSXYD8QFQ6RY02"))
            }
        }
    }

    @Test
    fun ` Should be able to get subsumsjon based on specific subsumsjon result id`() {
        withMigratedDb { dataSource ->
            with(PostgresSubsumsjonStore(dataSource)) {
                val minsteinntektId = ULID().nextULID()
                val satsId = ULID().nextULID()
                val grunnlagId = ULID().nextULID()
                val periodeId = ULID().nextULID()
                val internBehov =
                    opprettBehov(
                        Behov(
                            aktørId = "aktorid",
                            regelkontekst = RegelKontekst("1", Kontekst.vedtak),
                            beregningsDato = LocalDate.now(),
                        ),
                    )
                val subsumsjonWithResults =
                    subsumsjon.copy(
                        behovId = internBehov.behovId,
                        minsteinntektResultat = mapOf("subsumsjonsId" to minsteinntektId),
                        satsResultat = mapOf("subsumsjonsId" to satsId),
                        grunnlagResultat = mapOf("subsumsjonsId" to grunnlagId),
                        periodeResultat = mapOf("subsumsjonsId" to periodeId),
                    )

                insertSubsumsjon(subsumsjonWithResults) shouldBe 1

                assertSoftly {
                    getSubsumsjonByResult(SubsumsjonId(minsteinntektId)) shouldBe subsumsjonWithResults
                    getSubsumsjonByResult(SubsumsjonId(grunnlagId)) shouldBe subsumsjonWithResults
                    getSubsumsjonByResult(SubsumsjonId(satsId)) shouldBe subsumsjonWithResults
                    getSubsumsjonByResult(SubsumsjonId(periodeId)) shouldBe subsumsjonWithResults

                    shouldBeTimed()
                }
            }
        }
    }

    @Test
    fun `hits indexes for fetching results by subsumsjonsId`() {
        val kjenteResultatNøkler =
            setOf("satsResultat", "minsteinntektResultat", "periodeResultat", "grunnlagResultat")

        withMigratedDb { dataSource ->
            using(sessionOf(dataSource)) { session ->
                assertSoftly {
                    kjenteResultatNøkler.forEach {
                        session.run(
                            queryOf(
                                """EXPLAIN SELECT data FROM v2_subsumsjon
                                            WHERE data->'$it' ->> 'subsumsjonsId' = 'id' """,
                                emptyMap(),
                            ).map { r ->
                                withClue("Seq scan for resultatnøkkel  '$it'") {
                                    r.string(1).shouldNotContain("Seq Scan")
                                }
                            }.asSingle,
                        )
                    }
                }
            }
        }
    }

    private fun shouldBeTimed() {
        CollectorRegistry.defaultRegistry.metricFamilySamples().asSequence()
            .find { it.name == "subsumsjonstore_latency" }
            ?.let { metric ->
                metric.samples[0].name shouldNotBe null
            }
    }

    @Test
    fun ` Should throw not found exception if we not are able to get subsumsjon based on specific subsumsjon result id`() {
        withMigratedDb { dataSource ->
            with(PostgresSubsumsjonStore(dataSource)) {
                val internBehov =
                    opprettBehov(
                        Behov(
                            aktørId = "aktorid",
                            regelkontekst = RegelKontekst("1", Kontekst.vedtak),
                            beregningsDato = LocalDate.now(),
                        ),
                    )
                val subsumsjonWithResults =
                    subsumsjon.copy(
                        behovId = internBehov.behovId,
                        minsteinntektResultat = mapOf("subsumsjonsId" to ULID().nextULID()),
                        satsResultat = mapOf("subsumsjonsId" to ULID().nextULID()),
                        grunnlagResultat = mapOf("subsumsjonsId" to ULID().nextULID()),
                        periodeResultat = mapOf("subsumsjonsId" to ULID().nextULID()),
                    )

                insertSubsumsjon(subsumsjonWithResults) shouldBe 1

                assertThrows<SubsumsjonNotFoundException> { getSubsumsjonByResult(SubsumsjonId(ULID().nextULID())) }
            }
        }
    }

    @Test
    fun `Should generate new intern id for ekstern id`() {
        withMigratedDb { dataSource ->
            with(PostgresSubsumsjonStore(dataSource)) {
                val eksternId = RegelKontekst("1234", Kontekst.vedtak)
                val behandlingsId: BehandlingsId = opprettKoblingTilRegelkontekst(eksternId)
                ULID.parseULID(behandlingsId.id)
            }
        }
    }

    @Test
    fun `Should not generate new intern id for already existing ekstern id`() {
        withMigratedDb { dataSource ->
            with(PostgresSubsumsjonStore(dataSource)) {
                val eksternId = RegelKontekst("1234", Kontekst.vedtak)
                val behandlingsId1: BehandlingsId? = hentKoblingTilRegelKontekst(eksternId)
                val behandlingsId2: BehandlingsId? = hentKoblingTilRegelKontekst(eksternId)
                behandlingsId1 shouldBe behandlingsId2
            }
        }
    }

    @Test
    fun `Skal kunne håndtere uppercase VEDTAK `() {
        withMigratedDb { dataSource ->
            with(PostgresSubsumsjonStore(dataSource)) {
                val regelKontekst = RegelKontekst("1234", Kontekst.vedtak)
                val behandlingsId = BehandlingsId.nyBehandlingsIdFraEksternId(regelKontekst)
                using(sessionOf(PostgresDataSourceBuilder.dataSource)) { session ->
                    session.run(
                        queryOf(
                            "INSERT INTO v1_behov_behandling_mapping(id, ekstern_id, kontekst) VALUES (:id, :ekstern_id, :kontekst)",
                            mapOf(
                                "id" to behandlingsId.id,
                                "ekstern_id" to behandlingsId.regelKontekst.id,
                                "kontekst" to "VEDTAK",
                            ),
                        ).asUpdate,
                    )
                }
                val store = PostgresSubsumsjonStore(PostgresDataSourceBuilder.dataSource)
                val lagretBehandlingsId: BehandlingsId? = store.hentKoblingTilRegelKontekst(regelKontekst)
                behandlingsId shouldBe lagretBehandlingsId
            }
        }
    }

    private val subsumsjon =
        Subsumsjon(
            behovId = BehovId("01DSFST7S8HCXHRASYP9PC197W"),
            faktum = Faktum("aktorId", RegelKontekst("1", Kontekst.vedtak), LocalDate.now()),
            grunnlagResultat = emptyMap(),
            minsteinntektResultat = emptyMap(),
            periodeResultat = emptyMap(),
            satsResultat = emptyMap(),
            problem = Problem(title = "problem"),
        )
}
