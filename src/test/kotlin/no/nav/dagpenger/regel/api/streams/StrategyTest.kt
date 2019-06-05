package no.nav.dagpenger.regel.api.streams

import io.kotlintest.shouldBe
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
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
    internal fun `Criteria for handling packets`() {
        ManuellGrunnlagStrategy(SuccessStrategy(storeMock)).shouldHandle(pendingBehov) shouldBe false
        ManuellGrunnlagStrategy(SuccessStrategy(storeMock)).shouldHandle(Packet().apply {
            this.putValue(PacketKeys.BEHOV_ID, pendingBehov.behovId)
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
        CompleteResultStrategy(SuccessStrategy(storeMock)).shouldHandle(pendingBehov) shouldBe false
        CompleteResultStrategy(SuccessStrategy(storeMock)).shouldHandle(Packet().apply {
            this.putValue(PacketKeys.BEHOV_ID, pendingBehov.behovId)
            this.putValue(PacketKeys.GRUNNLAG_RESULTAT, thing)
            this.putValue(PacketKeys.SATS_RESULTAT, thing)
            this.putValue(PacketKeys.MINSTEINNTEKT_RESULTAT, thing)
            this.putValue(PacketKeys.PERIODE_RESULTAT, thing)
        }) shouldBe true
    }
}
