package no.nav.dagpenger.regel.api.db

import de.huxhorn.sulky.ulid.ULID
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import io.prometheus.client.CollectorRegistry
import no.nav.dagpenger.events.Problem
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
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class SubsumsjonStoreTest {

    @Test
    fun `Kaster ikke feil ved lagring av behov`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                val behov = Behov(
                    aktørId = "1234",
                    vedtakId = 1234,
                    beregningsDato = LocalDate.now(),
                    antallBarn = 1,
                    manueltGrunnlag = 11,
                    harAvtjentVerneplikt = false,
                    lærling = false,
                    bruktInntektsPeriode = InntektsPeriode(
                        YearMonth.now().minusMonths(12),
                        YearMonth.now()
                    ),
                )
                val internBehov = opprettBehov(behov)
                val lagretInternBehov = getBehov(internBehov.behovId)

                with(behov) {
                    aktørId shouldBe lagretInternBehov.aktørId
                    beregningsDato shouldBe lagretInternBehov.beregningsDato
                    antallBarn shouldBe lagretInternBehov.antallBarn
                    manueltGrunnlag shouldBe lagretInternBehov.manueltGrunnlag
                    harAvtjentVerneplikt shouldBe lagretInternBehov.harAvtjentVerneplikt
                    lærling shouldBe lagretInternBehov.lærling
                    bruktInntektsPeriode shouldBe lagretInternBehov.bruktInntektsPeriode
                    regelverksdato shouldBe lagretInternBehov.regelverksdato
                }
            }
        }
    }

    @Test
    fun `Status of behov is DONE if the behov exists and a subsumsjon for the behov exists`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {

                val internBehov = opprettBehov(
                    Behov(aktørId = "aktorid", vedtakId = 1, beregningsDato = LocalDate.now())
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
                    Behov(aktørId = "aktorid", vedtakId = 1, beregningsDato = LocalDate.now())
                )
                behovStatus(internBehov.behovId) shouldBe Status.Pending
            }
        }
    }

    @Test
    fun `Successful insert and extraction of a subsumsjon`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {

                val internBehov = opprettBehov(
                    Behov(aktørId = "aktorid", vedtakId = 1, beregningsDato = LocalDate.now())
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
                    Behov(aktørId = "aktorid", vedtakId = 1, beregningsDato = LocalDate.now())
                )
                val sub = subsumsjon.copy(behovId = internBehov.behovId)

                insertSubsumsjon(sub) shouldBe 1
                insertSubsumsjon(sub) shouldBe 0
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
                    Behov(aktørId = "aktorid", vedtakId = 1, beregningsDato = LocalDate.now())
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

                    shouldBeTimed()
                }
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

    @Test
    fun `Exception if retrieving status of a non existent behov`() {
        withMigratedDb {
            shouldThrow<BehovNotFoundException> {
                PostgresSubsumsjonStore(DataSource.instance).behovStatus(BehovId("01DSFGT6XCX4W1RKDXBYTAX5QH"))
            }
        }
    }

    @Test
    fun `Exception on insert of subsumsjon if no corresponding behov exists`() {
        withMigratedDb {
            shouldThrow<StoreException> {
                PostgresSubsumsjonStore(DataSource.instance).insertSubsumsjon(mockk(relaxed = true))
            }
        }
    }

    @Test
    fun `Exception if retrieving a non existent subsumsjon`() {
        withMigratedDb {
            shouldThrow<SubsumsjonNotFoundException> {
                PostgresSubsumsjonStore(DataSource.instance).getSubsumsjon(BehovId("01DSFHD74S4DGSXYD8QFQ6RY02"))
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
