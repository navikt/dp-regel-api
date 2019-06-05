package no.nav.dagpenger.regel.api.streams

import io.kotlintest.shouldBe
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verifyAll
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.PacketKeys
import no.nav.dagpenger.regel.api.models.Subsumsjon
import org.junit.jupiter.api.Test

internal class SuccessStrategyTest {

    @Test
    internal fun `Criteria for handling packets`() {
        SuccessStrategy(mockk()).shouldHandle(Packet()) shouldBe true
        SuccessStrategy(mockk()).shouldHandle(Packet().apply { addProblem(Problem(title = "problem")) }) shouldBe false
    }

    @Test
    internal fun `Insert subsumsjon if should handle`() {
        val subsumsjon = mockk<Subsumsjon>()
        mockkObject(Subsumsjon.Mapper).apply {
            every { Subsumsjon.subsumsjonFrom(any()) } returns subsumsjon
        }
        val store = mockk<SubsumsjonStore>().apply {
            every { this@apply.insertSubsumsjon(subsumsjon) } returns 1
        }

        SuccessStrategy(store).run(Packet())

        verifyAll {
            store.insertSubsumsjon(subsumsjon)
        }
    }

    @Test
    internal fun `Do nothing if should not handle`() {
        val store = mockk<SubsumsjonStore>()
        SuccessStrategy(store).run(Packet().apply { addProblem(Problem(title = "problem")) })

        verifyAll {
            store wasNot Called
        }
    }
}

internal class ManuellGrunnlagStrategyTest {
    private val thing = Any()

    @Test
    internal fun `Criteria for handling packets`() {

        ManuellGrunnlagStrategy(SuccessStrategy(mockk())).shouldHandle(Packet()) shouldBe false
        ManuellGrunnlagStrategy(SuccessStrategy(mockk())).shouldHandle(Packet().apply {
            this.putValue(PacketKeys.MANUELT_GRUNNLAG, thing)
            this.putValue(PacketKeys.GRUNNLAG_RESULTAT, thing)
            this.putValue(PacketKeys.SATS_RESULTAT, thing)
        }) shouldBe true
    }
}

internal class CompleteStrategyTest {
    private val thing = Any()

    @Test
    internal fun `Criteria for handling packets`() {

        CompleteResultStrategy(SuccessStrategy(mockk())).shouldHandle(Packet()) shouldBe false
        CompleteResultStrategy(SuccessStrategy(mockk())).shouldHandle(Packet().apply {
            this.putValue(PacketKeys.GRUNNLAG_RESULTAT, thing)
            this.putValue(PacketKeys.SATS_RESULTAT, thing)
            this.putValue(PacketKeys.MINSTEINNTEKT_RESULTAT, thing)
            this.putValue(PacketKeys.PERIODE_RESULTAT, thing)
        }) shouldBe true
    }
}
