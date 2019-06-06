package no.nav.dagpenger.regel.api.streams

import io.kotlintest.shouldBe
import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verifyAll
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.regel.api.db.BehovNotFoundException
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.PacketKeys
import no.nav.dagpenger.regel.api.models.Status
import no.nav.dagpenger.regel.api.models.Subsumsjon
import no.nav.dagpenger.regel.api.models.behovId
import org.junit.jupiter.api.Test

private val pendingBehov = Packet().apply { putValue(PacketKeys.BEHOV_ID, "behovId") }
private val doneBehov = Packet().apply { putValue(PacketKeys.BEHOV_ID, "invalid") }
private val notFoundBehov = Packet().apply { putValue(PacketKeys.BEHOV_ID, "notfound") }
private val problemPacket = Packet().apply {
    addProblem(Problem(title = "problem"))
    putValue(PacketKeys.BEHOV_ID, "behovId")
}

private val storeMock
    get() = mockk<SubsumsjonStore>().apply {
        every { this@apply.behovStatus(pendingBehov.behovId) } returns Status.Pending
        every { this@apply.behovStatus(doneBehov.behovId) } returns Status.Done("id")
        every { this@apply.behovStatus(notFoundBehov.behovId) } throws BehovNotFoundException("notfound")
    }

internal class SuccessStrategyTest {

    @Test
    internal fun `Criteria for handling packets`() {
        SuccessStrategy(storeMock).shouldHandle(problemPacket) shouldBe false
        SuccessStrategy(storeMock).shouldHandle(doneBehov) shouldBe false
        SuccessStrategy(storeMock).shouldHandle(notFoundBehov) shouldBe false
        SuccessStrategy(storeMock).shouldHandle(pendingBehov) shouldBe true
    }

    @Test
    internal fun `Insert subsumsjon if should handle`() {
        val subsumsjon = mockk<Subsumsjon>()
        mockkObject(Subsumsjon.Mapper).apply {
            every { Subsumsjon.subsumsjonFrom(any()) } returns subsumsjon
        }
        val subsumsjonStore = storeMock.apply { every { this@apply.insertSubsumsjon(subsumsjon) } returns 1 }

        SuccessStrategy(subsumsjonStore).run(pendingBehov)

        verifyAll {
            subsumsjonStore.insertSubsumsjon(subsumsjon)
            subsumsjonStore.behovStatus(pendingBehov.behovId)
        }

        unmockkAll()
    }

    @Test
    internal fun `Do nothing if should not handle`() {
        val mock = storeMock
        SuccessStrategy(mock).run(problemPacket)

        verifyAll {
            mock wasNot Called
        }
    }
}

internal class ManuellGrunnlagStrategyTest {
    private val thing = Any()

    @Test
    internal fun `Should delegate to SuccessStrategy if criterias are matched`() {
        val packet = Packet().apply {
            this.putValue(PacketKeys.MANUELT_GRUNNLAG, thing)
            this.putValue(PacketKeys.GRUNNLAG_RESULTAT, thing)
            this.putValue(PacketKeys.SATS_RESULTAT, thing)
        }
        val successStrategy = mockk<SuccessStrategy>().apply {
            every { this@apply.handle(packet) } just Runs
            every { this@apply.shouldHandle(packet) } returns true
        }

        ManuellGrunnlagStrategy(successStrategy).run(packet)

        verifyAll {
            successStrategy.handle(packet)
            successStrategy.shouldHandle(packet)
        }
    }

    @Test
    internal fun `Do nothing if criterias are not met`() {
        val successStrategy = mockk<SuccessStrategy>()

        ManuellGrunnlagStrategy(successStrategy).run(Packet())

        verifyAll { successStrategy wasNot Called }
    }
}

internal class CompleteStrategyTest {
    private val thing = Any()

    @Test
    internal fun `Should delegate to SuccessStrategy if criterias are matched`() {
        val packet = Packet().apply {
            this.putValue(PacketKeys.GRUNNLAG_RESULTAT, thing)
            this.putValue(PacketKeys.SATS_RESULTAT, thing)
            this.putValue(PacketKeys.PERIODE_RESULTAT, thing)
            this.putValue(PacketKeys.MINSTEINNTEKT_RESULTAT, thing)
        }
        val successStrategy = mockk<SuccessStrategy>().apply {
            every { this@apply.shouldHandle(packet) } returns true
            every { this@apply.handle(packet) } just Runs
        }

        CompleteResultStrategy(successStrategy).run(packet)

        verifyAll {
            successStrategy.handle(packet)
            successStrategy.shouldHandle(packet)
        }
    }

    @Test
    internal fun `Do nothing if criterias are not met`() {
        val successStrategy = mockk<SuccessStrategy>()

        CompleteResultStrategy(successStrategy).run(Packet())

        verifyAll { successStrategy wasNot Called }
    }
}
