package no.nav.dagpenger.regel.api

import de.huxhorn.sulky.ulid.ULID
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.regel.api.db.BehovNotFoundException
import no.nav.dagpenger.regel.api.db.EksternSubsumsjonBrukt
import no.nav.dagpenger.regel.api.db.JsonAdapter
import no.nav.dagpenger.regel.api.db.PostgresBruktSubsumsjonStore
import no.nav.dagpenger.regel.api.db.PostgresSubsumsjonStore
import no.nav.dagpenger.regel.api.db.PostgresTestSetup.withMigratedDb
import no.nav.dagpenger.regel.api.db.SubsumsjonNotFoundException
import no.nav.dagpenger.regel.api.models.Behov
import no.nav.dagpenger.regel.api.models.BehovId
import no.nav.dagpenger.regel.api.models.Faktum
import no.nav.dagpenger.regel.api.models.Kontekst
import no.nav.dagpenger.regel.api.models.RegelKontekst
import no.nav.dagpenger.regel.api.models.Subsumsjon
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.ZonedDateTime

internal class VaktmesterTest {
    val behov =
        Behov(
            aktørId = "1234",
            beregningsDato = LocalDate.now(),
            regelkontekst = RegelKontekst("9876", Kontekst.vedtak),
        )

    val minsteinntektSubsumsjonId = ULID().nextULID()
    val bruktSubsumsjon =
        Subsumsjon(
            behovId = BehovId("01DSFT4J9SW8XDZ2ZJZMXD5XV7"),
            faktum = Faktum("aktorId", RegelKontekst("1", Kontekst.vedtak), LocalDate.now()),
            grunnlagResultat = emptyMap(),
            minsteinntektResultat =
                mapOf(
                    "subsumsjonsId" to minsteinntektSubsumsjonId,
                ),
            periodeResultat = emptyMap(),
            satsResultat = emptyMap(),
            problem = Problem(title = "problem"),
        )
    val ubruktSubsumsjon = bruktSubsumsjon.copy(minsteinntektResultat = emptyMap())

    @Test
    fun `Skal markere subsumsjoner som brukt`() {
        withMigratedDb { dataSource ->
            val bruktSubsumsjonStore = PostgresBruktSubsumsjonStore(dataSource = dataSource)
            val subsumsjonStore = PostgresSubsumsjonStore(dataSource = dataSource)
            val internBehov = subsumsjonStore.opprettBehov(behov)
            subsumsjonStore.insertSubsumsjon(bruktSubsumsjon.copy(behovId = internBehov.behovId))
            val marker =
                bruktSubsumsjonStore.eksternTilInternSubsumsjon(
                    EksternSubsumsjonBrukt(
                        id = minsteinntektSubsumsjonId,
                        eksternId = behov.regelkontekst.id.toLong(),
                        arenaTs = ZonedDateTime.now(),
                        ts = ZonedDateTime.now().toEpochSecond(),
                    ),
                )
            val vaktmester = Vaktmester(dataSource = dataSource)
            vaktmester.markerSomBrukt(marker)
            using(sessionOf(dataSource)) { session ->
                val brukteSubsumsjoner =
                    session.run(
                        queryOf(
                            "SELECT * FROM v2_subsumsjon WHERE brukt = true",
                            emptyMap(),
                        ).map { r -> JsonAdapter.fromJson(r.string("data")) }.asList,
                    )
                brukteSubsumsjoner.size shouldBe 1
                brukteSubsumsjoner.first().minsteinntektResultat?.get("subsumsjonsId") shouldBe minsteinntektSubsumsjonId
            }
        }
    }

    @Test
    fun `Skal ikke slette brukte subsumsjoner`() {
        withMigratedDb { dataSource ->
            val vaktmester = Vaktmester(dataSource = dataSource)
            val internBehov =
                with(PostgresSubsumsjonStore(dataSource)) {
                    val internBehov = opprettBehov(behov)
                    insertSubsumsjon(bruktSubsumsjon.copy(behovId = internBehov.behovId))
                    return@with internBehov
                }
            with(
                PostgresBruktSubsumsjonStore(dataSource = dataSource),
            ) {
                val subsumsjonBruktV2 =
                    eksternTilInternSubsumsjon(
                        EksternSubsumsjonBrukt(
                            id = minsteinntektSubsumsjonId,
                            eksternId = behov.regelkontekst.id.toLong(),
                            arenaTs = ZonedDateTime.now(),
                            ts = ZonedDateTime.now().toEpochSecond(),
                        ),
                    )
                insertSubsumsjonBrukt(subsumsjonBruktV2)
                vaktmester.markerSomBrukt(subsumsjonBruktV2)
            }

            vaktmester.rydd()

            with(PostgresSubsumsjonStore(dataSource)) {
                getBehov((internBehov.behovId)) shouldNotBe null
                getSubsumsjon((internBehov.behovId)) shouldNotBe null
            }
        }
    }

    @Test
    fun `Skal slette ubrukte subsumsjoner eldre enn 6 måneder`() {
        withMigratedDb { dataSource ->
            val (ubruktInternBehov, bruktInternBehov) =
                with(PostgresSubsumsjonStore(dataSource)) {
                    val ubruktInternBehov = opprettBehov(behov)
                    val bruktInternBehov = opprettBehov(behov)

                    insertSubsumsjon(
                        ubruktSubsumsjon.copy(behovId = ubruktInternBehov.behovId),
                        created = ZonedDateTime.now().minusMonths(7),
                    )
                    insertSubsumsjon(
                        bruktSubsumsjon.copy(behovId = bruktInternBehov.behovId),
                        ZonedDateTime.now().minusMonths(7),
                    )
                    return@with (ubruktInternBehov to bruktInternBehov)
                }
            val vaktmester =
                Vaktmester(
                    dataSource = dataSource,
                    subsumsjonStore = PostgresSubsumsjonStore(dataSource),
                )
            with(PostgresBruktSubsumsjonStore(dataSource = dataSource)) {
                val bruktSub =
                    eksternTilInternSubsumsjon(
                        EksternSubsumsjonBrukt(
                            id = minsteinntektSubsumsjonId,
                            eksternId = behov.regelkontekst.id.toLong(),
                            arenaTs = ZonedDateTime.now(),
                            ts = ZonedDateTime.now().toEpochSecond(),
                        ),
                    )
                insertSubsumsjonBrukt(bruktSub)
                vaktmester.markerSomBrukt(bruktSub)
            }
            vaktmester.rydd()

            with(PostgresSubsumsjonStore(dataSource)) {
                assertThrows<BehovNotFoundException> { getBehov((ubruktInternBehov.behovId)) }
                getBehov((bruktInternBehov.behovId)) shouldNotBe null

                assertThrows<SubsumsjonNotFoundException> { getSubsumsjon((ubruktInternBehov.behovId)) }
                getSubsumsjon((bruktInternBehov.behovId)) shouldNotBe null
            }
        }
    }

    @Test
    fun `Skal ikke slette ubrukte subsumsjoner yngre enn 3 måneder`() {
        withMigratedDb { dataSource ->
            val vaktmester =
                Vaktmester(
                    dataSource = dataSource,
                    subsumsjonStore = PostgresSubsumsjonStore(dataSource),
                )
            val (ubruktInternBehov, bruktInternBehov) =
                with(PostgresSubsumsjonStore(dataSource)) {
                    val ubruktInternBehov = opprettBehov(behov)
                    val bruktInternBehov = opprettBehov(behov)

                    insertSubsumsjon(ubruktSubsumsjon.copy(behovId = ubruktInternBehov.behovId))
                    insertSubsumsjon(bruktSubsumsjon.copy(behovId = bruktInternBehov.behovId))
                    return@with (ubruktInternBehov to bruktInternBehov)
                }
            with(
                PostgresBruktSubsumsjonStore(dataSource = dataSource),
            ) {
                val subsumsjonBruktV2 =
                    eksternTilInternSubsumsjon(
                        EksternSubsumsjonBrukt(
                            id = minsteinntektSubsumsjonId,
                            eksternId = behov.regelkontekst.id.toLong(),
                            arenaTs = ZonedDateTime.now(),
                            ts = ZonedDateTime.now().toEpochSecond(),
                        ),
                    )
                insertSubsumsjonBrukt(subsumsjonBruktV2)
                vaktmester.markerSomBrukt(subsumsjonBruktV2)
            }
            vaktmester.rydd()

            with(PostgresSubsumsjonStore(dataSource)) {
                getBehov(ubruktInternBehov.behovId) shouldNotBe null
                getBehov(bruktInternBehov.behovId) shouldNotBe null
                getSubsumsjon(ubruktInternBehov.behovId) shouldNotBe null
                getSubsumsjon(bruktInternBehov.behovId) shouldNotBe null
            }
        }
    }

    @Test
    fun `markerer allerede eksisterende brukte subsumsjoner`() {
        withMigratedDb { dataSource ->
            runBlocking {
                val vaktmester =
                    Vaktmester(
                        dataSource = dataSource,
                        subsumsjonStore = PostgresSubsumsjonStore(dataSource),
                    )
                with(PostgresSubsumsjonStore(dataSource)) {
                    val ubruktInternBehov = opprettBehov(behov)
                    val bruktInternBehov = opprettBehov(behov)

                    insertSubsumsjon(ubruktSubsumsjon.copy(behovId = ubruktInternBehov.behovId))
                    insertSubsumsjon(bruktSubsumsjon.copy(behovId = bruktInternBehov.behovId))
                }
                with(
                    PostgresBruktSubsumsjonStore(dataSource = dataSource),
                ) {
                    val subsumsjonBruktV2 =
                        eksternTilInternSubsumsjon(
                            EksternSubsumsjonBrukt(
                                id = minsteinntektSubsumsjonId,
                                eksternId = behov.regelkontekst.id.toLong(),
                                arenaTs = ZonedDateTime.now(),
                                ts = ZonedDateTime.now().toEpochSecond(),
                            ),
                        )
                    insertSubsumsjonBrukt(subsumsjonBruktV2)
                }
                vaktmester.markerBrukteSubsumsjoner()
                using(sessionOf(dataSource)) { session ->
                    val brukteSubsumsjoner =
                        session.run(
                            queryOf(
                                "SELECT * FROM v2_subsumsjon WHERE brukt = true",
                                emptyMap(),
                            ).map { r -> JsonAdapter.fromJson(r.string("data")) }.asList,
                        )
                    brukteSubsumsjoner.size shouldBe 1
                    brukteSubsumsjoner.first().minsteinntektResultat?.get("subsumsjonsId") shouldBe minsteinntektSubsumsjonId
                }
            }
        }
    }
}
