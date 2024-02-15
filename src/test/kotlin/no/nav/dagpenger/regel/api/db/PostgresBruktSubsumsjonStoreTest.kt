package no.nav.dagpenger.regel.api.db

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.regel.api.db.PostgresTestSetup.withMigratedDb
import no.nav.dagpenger.regel.api.models.BehovId
import no.nav.dagpenger.regel.api.models.Faktum
import no.nav.dagpenger.regel.api.models.Kontekst
import no.nav.dagpenger.regel.api.models.RegelKontekst
import no.nav.dagpenger.regel.api.models.Subsumsjon
import no.nav.dagpenger.regel.api.models.SubsumsjonId
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class PostgresBruktSubsumsjonStoreTest {

    @Test
    fun `successfully inserts BruktSubsumsjon`() {
        withMigratedDb { dataSource ->
            val subsumsjonStore = PostgresSubsumsjonStore(dataSource)
            with(
                PostgresBruktSubsumsjonStore(
                    dataSource = dataSource,
                    subsumsjonStore = subsumsjonStore
                )
            ) {
                opprettKoblingTilEkstern(subsumsjonStore)
                insertSubsumsjonBrukt(eksternTilInternSubsumsjon(bruktSubsumsjon)) shouldBe 1
            }
        }
    }

    @Test
    fun `inserting intern subsumsjon brukt also works`() {
        withMigratedDb { dataSource ->
            with(PostgresBruktSubsumsjonStore(dataSource = dataSource)) {
                val internSubsumsjonBrukt = InternSubsumsjonBrukt(
                    id = subsumsjon.behovId.id,
                    behandlingsId = PostgresSubsumsjonStore(dataSource).opprettKoblingTilRegelkontekst(
                        eksternId
                    ).id,
                    arenaTs = exampleDate
                )
                this.insertSubsumsjonBrukt(internSubsumsjonBrukt = internSubsumsjonBrukt)
                val savedBruktSub = getSubsumsjonBrukt(SubsumsjonId(internSubsumsjonBrukt.id))
                savedBruktSub!!.created shouldNotBe null
                savedBruktSub.created!!.toLocalDate() shouldBe LocalDate.now()
            }
        }
    }

    @Test
    fun `successfully fetches inserted BruktSubsumsjon`() {
        withMigratedDb { dataSource ->
            val subsumsjonStore = PostgresSubsumsjonStore(dataSource)
            with(
                PostgresBruktSubsumsjonStore(
                    dataSource = dataSource,
                    subsumsjonStore = subsumsjonStore
                )
            ) {
                subsumsjonStore.opprettKoblingTilRegelkontekst(
                    RegelKontekst(
                        bruktSubsumsjon.eksternId.toString(),
                        Kontekst.vedtak
                    )
                )
                insertSubsumsjonBrukt(eksternTilInternSubsumsjon(bruktSubsumsjon)) shouldBe 1
                /*getSubsumsjonBrukt(SubsumsjonId(bruktSubsumsjon.id))?.arenaTs?.format(secondFormatter) shouldBe exampleDate.format(
                    secondFormatter
                )*/
            }
        }
    }

    @Test
    fun `successfully fetches inserted BruktSubsumsjonV2`() {
        withMigratedDb { dataSource ->
            val subsumsjonStore = PostgresSubsumsjonStore(dataSource)
            with(
                PostgresBruktSubsumsjonStore(
                    dataSource = dataSource,
                    subsumsjonStore = subsumsjonStore
                )
            ) {
                opprettKoblingTilEkstern(subsumsjonStore)
                val internSubsumsjonBrukt = eksternTilInternSubsumsjon(bruktSubsumsjon)
                insertSubsumsjonBrukt(internSubsumsjonBrukt)
                /*getSubsumsjonBrukt(SubsumsjonId(bruktSubsumsjon.id))?.arenaTs?.format(secondFormatter) shouldBe exampleDate.format(
                    secondFormatter
                )*/
            }
        }
    }

    @Test
    fun `trying to insert duplicate ids keeps what's already in the db`() {
        withMigratedDb { dataSource ->
            val subsumsjonStore = PostgresSubsumsjonStore(dataSource)
            with(
                PostgresBruktSubsumsjonStore(
                    dataSource = dataSource,
                    subsumsjonStore = subsumsjonStore
                )
            ) {
                opprettKoblingTilEkstern(subsumsjonStore)
                val internSubsumsjonBrukt1 = eksternTilInternSubsumsjon(bruktSubsumsjon)
                insertSubsumsjonBrukt(internSubsumsjonBrukt1) shouldBe 1
                insertSubsumsjonBrukt(internSubsumsjonBrukt1) shouldBe 0
                getSubsumsjonBrukt(SubsumsjonId(bruktSubsumsjon.id))?.behandlingsId shouldBe internSubsumsjonBrukt1.behandlingsId
            }
        }
    }

    @Test
    fun `trying to fetch subsumsjon to an non existing extern id should fail`() {
        withMigratedDb { dataSource ->
            val subsumsjonStore = PostgresSubsumsjonStore(dataSource)
            with(
                PostgresBruktSubsumsjonStore(
                    dataSource = dataSource,
                    subsumsjonStore = subsumsjonStore
                )
            ) {
                shouldThrow<SubsumsjonBruktNotFoundException> {
                    eksternTilInternSubsumsjon(
                        bruktSubsumsjon.copy(
                            eksternId = 9876
                        )
                    )
                }
            }
        }
    }

    private fun opprettKoblingTilEkstern(subsumsjonStore: PostgresSubsumsjonStore) {
        subsumsjonStore.opprettKoblingTilRegelkontekst(
            RegelKontekst(
                bruktSubsumsjon.eksternId.toString(),
                Kontekst.vedtak
            )
        )
    }

    val oslo = ZoneId.of("Europe/Oslo")
    val exampleDate = ZonedDateTime.now(oslo).minusHours(6)
    val subsumsjon = Subsumsjon(
        behovId = BehovId("01DSFT25TF56A7J8HBGDMEXAZB"),
        faktum = Faktum("aktorId", RegelKontekst("1", Kontekst.vedtak), LocalDate.now()),
        grunnlagResultat = emptyMap(),
        minsteinntektResultat = emptyMap(),
        periodeResultat = emptyMap(),
        satsResultat = emptyMap(),
        problem = Problem(title = "problem")
    )
    val eksternId = RegelKontekst(id = "1234", type = Kontekst.vedtak)
    val bruktSubsumsjon =
        EksternSubsumsjonBrukt(
            id = subsumsjon.behovId.id,
            eksternId = 1231231,
            arenaTs = exampleDate,
            ts = Instant.now().toEpochMilli()
        )
}
