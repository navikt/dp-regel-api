package no.nav.dagpenger.regel.api.streams

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.regel.api.Vaktmester
import no.nav.dagpenger.regel.api.db.BruktSubsumsjonStore
import no.nav.dagpenger.regel.api.db.EksternSubsumsjonBrukt
import no.nav.dagpenger.regel.api.db.InternSubsumsjonBrukt
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

internal class BruktSubsumsjonStrategyTest {
    @Test
    fun `Should handle list of brukte subsumsjoner`() {
        val now = ZonedDateTime.now()
        val bruktSubsumsjon =
            EksternSubsumsjonBrukt(
                id = "test",
                eksternId = 1234678L,
                arenaTs = now,
                ts = now.toInstant().toEpochMilli()
            )

        val storeMock = mockk<BruktSubsumsjonStore>(relaxed = false).apply {
            every { this@apply.eksternTilInternSubsumsjon(any()) } returns InternSubsumsjonBrukt(
                id = "test",
                behandlingsId = "b",
                arenaTs = now.minusMinutes(5)
            )
            every { this@apply.insertSubsumsjonBrukt(any()) } returns 1
        }
        val vaktmester = mockk<Vaktmester>(relaxed = true).apply {
            every { this@apply.markerSomBrukt(any()) } returns Unit
        }

        val bb = BruktSubsumsjonStrategy(vaktmester = vaktmester, bruktSubsumsjonStore = storeMock)

        bb.handle(sequenceOf(bruktSubsumsjon))

        verify(exactly = 1) {
            vaktmester.markerSomBrukt(any())
        }

        verify(exactly = 1) {
            storeMock.insertSubsumsjonBrukt(any())
        }
    }

    @Test
    fun `Should filter out vedtak with status 'AVSLU' (avsluttet) og utfall 'Avbrutt'`() {
        val now = ZonedDateTime.now()

        val storeMock = mockk<BruktSubsumsjonStore>(relaxed = false).apply {
            every { this@apply.eksternTilInternSubsumsjon(any()) } returns InternSubsumsjonBrukt(
                id = "test",
                behandlingsId = "b",
                arenaTs = now.minusMinutes(5)
            )
            every { this@apply.insertSubsumsjonBrukt(any()) } returns 1
        }
        val vaktmester = mockk<Vaktmester>(relaxed = true).apply {
            every { this@apply.markerSomBrukt(any()) } returns Unit
        }

        val bruktSubsumsjonStrategy = BruktSubsumsjonStrategy(vaktmester = vaktmester, bruktSubsumsjonStore = storeMock)

        val brukteSubsumsjoner = sequenceOf(
            EksternSubsumsjonBrukt(
                id = "test",
                eksternId = 1234678L,
                arenaTs = now,
                ts = now.toInstant().toEpochMilli(),
                utfall = "AVBRUTT",
                vedtakStatus = "AVSLU"
            ),
            EksternSubsumsjonBrukt(
                id = "test",
                eksternId = 1234678L,
                arenaTs = now,
                ts = now.toInstant().toEpochMilli(),
                utfall = "NEI",
                vedtakStatus = "AVSLU"
            ),
            EksternSubsumsjonBrukt(
                id = "test",
                eksternId = 1234678L,
                arenaTs = now,
                ts = now.toInstant().toEpochMilli(),
                utfall = "JA",
                vedtakStatus = "IVERK"
            )
        )

        bruktSubsumsjonStrategy.handle(brukteSubsumsjoner)

        verify(exactly = 2) {
            vaktmester.markerSomBrukt(any())
        }

        verify(exactly = 2) {
            storeMock.insertSubsumsjonBrukt(any())
        }
    }
}
