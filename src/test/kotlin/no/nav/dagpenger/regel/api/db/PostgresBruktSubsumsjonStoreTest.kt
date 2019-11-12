package no.nav.dagpenger.regel.api.db

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.regel.api.models.BehovId
import no.nav.dagpenger.regel.api.models.EksternId
import no.nav.dagpenger.regel.api.models.Faktum
import no.nav.dagpenger.regel.api.models.Kontekst
import no.nav.dagpenger.regel.api.models.Subsumsjon
import no.nav.dagpenger.regel.api.models.SubsumsjonId
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class PostgresBruktSubsumsjonStoreTest {

    @Test
    fun `successfully inserts BruktSubsumsjon`() {
        withMigratedDb {
            with(PostgresBruktSubsumsjonStore(dataSource = DataSource.instance)) {
                insertSubsumsjonBrukt(eksternTilInternSubsumsjon(bruktSubsumsjon)) shouldBe 1
            }
        }
    }

    @Test
    fun `inserting intern subsumsjon brukt also works`() {
        withMigratedDb {
            with(PostgresBruktSubsumsjonStore(dataSource = DataSource.instance)) {
                val internSubsumsjonBrukt = InternSubsumsjonBrukt(
                    id = subsumsjon.behovId.id,
                    behandlingsId = PostgresSubsumsjonStore(DataSource.instance).hentKoblingTilEkstern(eksternId).id,
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
        withMigratedDb {
            with(PostgresBruktSubsumsjonStore(dataSource = DataSource.instance)) {
                insertSubsumsjonBrukt(eksternTilInternSubsumsjon(bruktSubsumsjon)) shouldBe 1
                getSubsumsjonBrukt(SubsumsjonId(bruktSubsumsjon.id))?.arenaTs?.format(secondFormatter) shouldBe exampleDate.format(
                    secondFormatter
                )
            }
        }
    }

    @Test
    fun `successfully fetches inserted BruktSubsumsjonV2`() {
        withMigratedDb {
            with(PostgresBruktSubsumsjonStore(dataSource = DataSource.instance)) {
                val internSubsumsjonBrukt = eksternTilInternSubsumsjon(bruktSubsumsjon)
                insertSubsumsjonBrukt(internSubsumsjonBrukt)
                getSubsumsjonBrukt(SubsumsjonId(bruktSubsumsjon.id))?.arenaTs?.format(secondFormatter) shouldBe exampleDate.format(
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
                val internSubsumsjonBrukt1 = eksternTilInternSubsumsjon(bruktSubsumsjon)
                insertSubsumsjonBrukt(internSubsumsjonBrukt1) shouldBe 1
                insertSubsumsjonBrukt(internSubsumsjonBrukt1) shouldBe 0
                getSubsumsjonBrukt(SubsumsjonId(bruktSubsumsjon.id))?.behandlingsId shouldBe internSubsumsjonBrukt1.behandlingsId
            }
        }
    }

    val secondFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    val oslo = ZoneId.of("Europe/Oslo")
    val exampleDate = ZonedDateTime.now(oslo).minusHours(6)
    val subsumsjon = Subsumsjon(
        behovId = BehovId("01DSFT25TF56A7J8HBGDMEXAZB"),
        faktum = Faktum("aktorId", 1, LocalDate.now()),
        grunnlagResultat = emptyMap(),
        minsteinntektResultat = emptyMap(),
        periodeResultat = emptyMap(),
        satsResultat = emptyMap(),
        problem = Problem(title = "problem")
    )
    val eksternId = EksternId(id = "1234", kontekst = Kontekst.VEDTAK)
    val bruktSubsumsjon =
        EksternSubsumsjonBrukt(
            id = subsumsjon.behovId.id,
            eksternId = 1231231,
            arenaTs = exampleDate,
            ts = Instant.now().toEpochMilli()
        )
}
