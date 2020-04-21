package no.nav.dagpenger.regel.api

import de.huxhorn.sulky.ulid.ULID
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.finn.unleash.FakeUnleash
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.regel.api.db.BehovNotFoundException
import no.nav.dagpenger.regel.api.db.DataSource
import no.nav.dagpenger.regel.api.db.EksternSubsumsjonBrukt
import no.nav.dagpenger.regel.api.db.PostgresBruktSubsumsjonStore
import no.nav.dagpenger.regel.api.db.PostgresSubsumsjonStore
import no.nav.dagpenger.regel.api.db.SubsumsjonNotFoundException
import no.nav.dagpenger.regel.api.db.withMigratedDb
import no.nav.dagpenger.regel.api.models.Behov
import no.nav.dagpenger.regel.api.models.BehovId
import no.nav.dagpenger.regel.api.models.Faktum
import no.nav.dagpenger.regel.api.models.Subsumsjon
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class VaktmesterTest {

    val behov = Behov(
        aktørId = "1234",
        vedtakId = 9876,
        beregningsDato = LocalDate.now()
    )

    val minsteinntektSubsumsjonId = ULID().nextULID()
    val bruktSubsumsjon = Subsumsjon(
        behovId = BehovId("01DSFT4J9SW8XDZ2ZJZMXD5XV7"),
        faktum = Faktum("aktorId", 1, LocalDate.now()),
        grunnlagResultat = emptyMap(),
        minsteinntektResultat = mapOf(
            "subsumsjonsId" to minsteinntektSubsumsjonId
        ),
        periodeResultat = emptyMap(),
        satsResultat = emptyMap(),
        problem = Problem(title = "problem")
    )
    val ubruktSubsumsjon = bruktSubsumsjon.copy(minsteinntektResultat = emptyMap())

    @Test
    fun `Skal markere subsumsjoner som brukt`() {
        withMigratedDb {
            val bruktSubsumsjonStore = PostgresBruktSubsumsjonStore(dataSource = DataSource.instance)
            val subsumsjonStore = PostgresSubsumsjonStore(dataSource = DataSource.instance)
            val internBehov = subsumsjonStore.opprettBehov(behov, FakeUnleash())
            subsumsjonStore.insertSubsumsjon(bruktSubsumsjon.copy(behovId = internBehov.behovId))
            val marker = bruktSubsumsjonStore.eksternTilInternSubsumsjon(
                EksternSubsumsjonBrukt(
                    id = minsteinntektSubsumsjonId,
                    eksternId = behov.vedtakId.toLong(),
                    arenaTs = ZonedDateTime.now(),
                    ts = ZonedDateTime.now().toEpochSecond()
                )
            )
            val vaktmester = Vaktmester(dataSource = DataSource.instance)
            vaktmester.markerSomBrukt(marker)
            using(sessionOf(DataSource.instance)) { session ->
                val brukteSubsumsjoner = session.run(
                    queryOf(
                        "SELECT * FROM v2_subsumsjon WHERE brukt = true",
                        emptyMap()
                    ).map { r -> Subsumsjon.fromJson(r.string("data")) }.asList
                )
                brukteSubsumsjoner.size shouldBe 1
                brukteSubsumsjoner.first().minsteinntektResultat?.get("subsumsjonsId") shouldBe minsteinntektSubsumsjonId
            }
        }
    }

    @Test
    fun `Skal ikke slette brukte subsumsjoner`() {

        withMigratedDb {
            val vaktmester = Vaktmester(dataSource = DataSource.instance)
            val internBehov = with(PostgresSubsumsjonStore(DataSource.instance)) {
                val internBehov = opprettBehov(behov, FakeUnleash())
                insertSubsumsjon(bruktSubsumsjon.copy(behovId = internBehov.behovId))
                return@with internBehov
            }
            with(
                PostgresBruktSubsumsjonStore(dataSource = DataSource.instance)
            ) {
                val subsumsjonBruktV2 = eksternTilInternSubsumsjon(
                    EksternSubsumsjonBrukt(
                        id = minsteinntektSubsumsjonId,
                        eksternId = behov.vedtakId.toLong(),
                        arenaTs = ZonedDateTime.now(),
                        ts = ZonedDateTime.now().toEpochSecond()

                    )
                )
                insertSubsumsjonBrukt(subsumsjonBruktV2)
                vaktmester.markerSomBrukt(subsumsjonBruktV2)
            }

            vaktmester.rydd()

            with(PostgresSubsumsjonStore(DataSource.instance)) {
                getBehov((internBehov.behovId)) shouldNotBe null
                getSubsumsjon((internBehov.behovId)) shouldNotBe null
            }
        }
    }

    @Test
    fun `Skal slette ubrukte subsumsjoner eldre enn 3 måneder`() {
        withMigratedDb {
            val (ubruktInternBehov, bruktInternBehov) = with(PostgresSubsumsjonStore(DataSource.instance)) {
                val ubruktInternBehov = opprettBehov(behov, FakeUnleash())
                val bruktInternBehov = opprettBehov(behov, FakeUnleash())

                insertSubsumsjon(
                    ubruktSubsumsjon.copy(behovId = ubruktInternBehov.behovId),
                    created = ZonedDateTime.now().minusMonths(4)
                )
                insertSubsumsjon(
                    bruktSubsumsjon.copy(behovId = bruktInternBehov.behovId),
                    ZonedDateTime.now().minusMonths(4)
                )
                return@with (ubruktInternBehov to bruktInternBehov)
            }
            val vaktmester = Vaktmester(
                dataSource = DataSource.instance,
                subsumsjonStore = PostgresSubsumsjonStore(DataSource.instance)
            )
            with(PostgresBruktSubsumsjonStore(dataSource = DataSource.instance)) {
                val bruktSub = eksternTilInternSubsumsjon(
                    EksternSubsumsjonBrukt(
                        id = minsteinntektSubsumsjonId,
                        eksternId = behov.vedtakId.toLong(),
                        arenaTs = ZonedDateTime.now(),
                        ts = ZonedDateTime.now().toEpochSecond()

                    )
                )
                insertSubsumsjonBrukt(bruktSub)
                vaktmester.markerSomBrukt(bruktSub)
            }
            vaktmester.rydd()

            with(PostgresSubsumsjonStore(DataSource.instance)) {
                assertThrows<BehovNotFoundException> { getBehov((ubruktInternBehov.behovId)) }
                getBehov((bruktInternBehov.behovId)) shouldNotBe null

                assertThrows<SubsumsjonNotFoundException> { getSubsumsjon((ubruktInternBehov.behovId)) }
                getSubsumsjon((bruktInternBehov.behovId)) shouldNotBe null
            }
        }
    }

    @Test
    fun `Skal ikke slette ubrukte subsumsjoner yngre enn 3 måneder`() {

        withMigratedDb {
            val vaktmester = Vaktmester(
                dataSource = DataSource.instance,
                subsumsjonStore = PostgresSubsumsjonStore(DataSource.instance)
            )
            val (ubruktInternBehov, bruktInternBehov) = with(PostgresSubsumsjonStore(DataSource.instance)) {
                val ubruktInternBehov = opprettBehov(behov, FakeUnleash())
                val bruktInternBehov = opprettBehov(behov, FakeUnleash())

                insertSubsumsjon(ubruktSubsumsjon.copy(behovId = ubruktInternBehov.behovId))
                insertSubsumsjon(bruktSubsumsjon.copy(behovId = bruktInternBehov.behovId))
                return@with (ubruktInternBehov to bruktInternBehov)
            }
            with(
                PostgresBruktSubsumsjonStore(dataSource = DataSource.instance)
            ) {
                val subsumsjonBruktV2 = eksternTilInternSubsumsjon(
                    EksternSubsumsjonBrukt(
                        id = minsteinntektSubsumsjonId,
                        eksternId = behov.vedtakId.toLong(),
                        arenaTs = ZonedDateTime.now(),
                        ts = ZonedDateTime.now().toEpochSecond()

                    )
                )
                insertSubsumsjonBrukt(subsumsjonBruktV2)
                vaktmester.markerSomBrukt(subsumsjonBruktV2)
            }
            vaktmester.rydd()

            with(PostgresSubsumsjonStore(DataSource.instance)) {
                getBehov(ubruktInternBehov.behovId) shouldNotBe null
                getBehov(bruktInternBehov.behovId) shouldNotBe null
                getSubsumsjon(ubruktInternBehov.behovId) shouldNotBe null
                getSubsumsjon(bruktInternBehov.behovId) shouldNotBe null
            }
        }
    }

    @Test
    fun `markerer allerede eksisterende brukte subsumsjoner`() {
        withMigratedDb {
            runBlocking {
                val vaktmester = Vaktmester(
                    dataSource = DataSource.instance,
                    subsumsjonStore = PostgresSubsumsjonStore(DataSource.instance)
                )
                with(PostgresSubsumsjonStore(DataSource.instance)) {
                    val ubruktInternBehov = opprettBehov(behov, FakeUnleash())
                    val bruktInternBehov = opprettBehov(behov, FakeUnleash())

                    insertSubsumsjon(ubruktSubsumsjon.copy(behovId = ubruktInternBehov.behovId))
                    insertSubsumsjon(bruktSubsumsjon.copy(behovId = bruktInternBehov.behovId))
                }
                with(
                    PostgresBruktSubsumsjonStore(dataSource = DataSource.instance)
                ) {
                    val subsumsjonBruktV2 = eksternTilInternSubsumsjon(
                        EksternSubsumsjonBrukt(
                            id = minsteinntektSubsumsjonId,
                            eksternId = behov.vedtakId.toLong(),
                            arenaTs = ZonedDateTime.now(),
                            ts = ZonedDateTime.now().toEpochSecond()

                        )
                    )
                    insertSubsumsjonBrukt(subsumsjonBruktV2)
                }
                vaktmester.markerBrukteSubsumsjoner()
                using(sessionOf(DataSource.instance)) { session ->
                    val brukteSubsumsjoner = session.run(
                        queryOf(
                            "SELECT * FROM v2_subsumsjon WHERE brukt = true",
                            emptyMap()
                        ).map { r -> Subsumsjon.fromJson(r.string("data")) }.asList
                    )
                    brukteSubsumsjoner.size shouldBe 1
                    brukteSubsumsjoner.first().minsteinntektResultat?.get("subsumsjonsId") shouldBe minsteinntektSubsumsjonId
                }
            }
        }
    }
}
