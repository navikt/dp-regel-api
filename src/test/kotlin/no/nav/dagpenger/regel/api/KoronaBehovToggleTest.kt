package no.nav.dagpenger.regel.api

import io.kotlintest.shouldBe
import no.finn.unleash.FakeUnleash
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.BehandlingsId
import no.nav.dagpenger.regel.api.models.BehovId
import no.nav.dagpenger.regel.api.models.EksternId
import no.nav.dagpenger.regel.api.models.InntektsPeriode
import no.nav.dagpenger.regel.api.models.InternBehov
import no.nav.dagpenger.regel.api.models.Kontekst
import no.nav.dagpenger.regel.api.models.PacketKeys
import no.nav.dagpenger.regel.api.models.Status
import no.nav.dagpenger.regel.api.models.Subsumsjon
import no.nav.dagpenger.regel.api.models.SubsumsjonId
import no.nav.dagpenger.regel.api.routing.BehovRequest
import no.nav.dagpenger.regel.api.routing.mapRequestToBehov
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime

class KoronaBehovToggleTest {

    val subsumsjonStore: SubsumsjonStore = object : SubsumsjonStore {
        override fun insertSubsumsjon(subsumsjon: Subsumsjon, created: ZonedDateTime): Int {
            return 0
        }

        override fun delete(subsumsjon: Subsumsjon) {
            TODO("not implemented")
        }

        override fun getBehov(behovId: BehovId): InternBehov {
            TODO("not implemented")
        }

        override fun insertBehov(behov: InternBehov): Int {
            return 1
        }

        override fun hentKoblingTilEkstern(eksternId: EksternId): BehandlingsId? {
            return BehandlingsId.nyBehandlingsIdFraEksternId(eksternId)
        }

        override fun opprettKoblingTilEkstern(eksternId: EksternId): BehandlingsId {
            TODO("not implemented")
        }

        override fun behovStatus(behovId: BehovId): Status {
            TODO("not implemented")
        }

        override fun getSubsumsjon(behovId: BehovId): Subsumsjon {
            TODO("not implemented")
        }

        override fun getSubsumsjonByResult(subsumsjonId: SubsumsjonId): Subsumsjon {
            TODO("not implemented")
        }
    }

    @Test
    fun `Internbehov skal ha koronatoggle på hvis unleash toggle er på`() {
        val unleash = FakeUnleash()
        unleash.enable("dp.korona")

        val behov = mapRequestToBehov(
            BehovRequest(
                aktorId = "aktorId",
                vedtakId = 1,
                beregningsdato = LocalDate.of(2019, 11, 7),
                harAvtjentVerneplikt = null,
                oppfyllerKravTilFangstOgFisk = null,
                bruktInntektsPeriode = null,
                manueltGrunnlag = null,
                antallBarn = null
            )
        )

        val internbehov = subsumsjonStore.opprettBehov(behov, unleash)

        assertNotNull(internbehov.koronaToggle)
        assertTrue(internbehov.koronaToggle)
    }

    @Test
    fun `Internbehov skal ha koronatoggle av hvis unleash toggle er av`() {
        val unleash = FakeUnleash()
        unleash.disable("dp.korona")

        val behov = mapRequestToBehov(
            BehovRequest(
                aktorId = "aktorId",
                vedtakId = 1,
                beregningsdato = LocalDate.of(2019, 11, 7),
                harAvtjentVerneplikt = null,
                oppfyllerKravTilFangstOgFisk = null,
                bruktInntektsPeriode = null,
                manueltGrunnlag = null,
                antallBarn = null
            )
        )

        val internbehov = subsumsjonStore.opprettBehov(behov, unleash)

        assertNotNull(internbehov.koronaToggle)
        assertFalse(internbehov.koronaToggle)
    }

    @Test
    fun `Packet skal ha koronatoggle fra internbehov`() {
        val behov = InternBehov(BehovId("01DSFVQ4NQQ64SNT4Z16TJXXE7"), "aktørId", BehandlingsId.nyBehandlingsIdFraEksternId(EksternId("1234", Kontekst.VEDTAK)), LocalDate.now(), true, true, InntektsPeriode(
            YearMonth.now(), YearMonth.now()), 1, 1, koronaToggle = false)
        val packet = InternBehov.toPacket(behov)

        packet.getBoolean(PacketKeys.KORONA_TOGGLE) shouldBe behov.koronaToggle
    }
}