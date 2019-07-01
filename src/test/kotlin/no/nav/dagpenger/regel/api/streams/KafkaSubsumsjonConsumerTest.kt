package no.nav.dagpenger.regel.api.streams

import io.kotlintest.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.models.PacketKeys
import no.nav.dagpenger.regel.api.models.behovId
import org.junit.jupiter.api.Test

internal class KafkaSubsumsjonConsumerTest {

    @Test
    fun `has behov`() {
        Packet().hasBehovId() shouldBe false
        Packet().apply { putValue(PacketKeys.BEHOV_ID, "id") }.hasBehovId() shouldBe true
    }

    @Test
    fun `OnPacket should run all strategies`() {
        val packet = Packet().apply { this.putValue(PacketKeys.BEHOV_ID, "behovId") }
        val mock = mockk<SubsumsjonPacketStrategy>().apply {
            every { this@apply.run(match { it.behovId == "behovId" }) } just Runs
        }
        SubsumsjonPond(listOf(mock, mock), Configuration()).onPacket(packet)
        verify(exactly = 2) { mock.run(match { it.behovId == "behovId" }) }
    }
}