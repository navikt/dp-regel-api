package no.nav.dagpenger.regel.api.models

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.events.Packet
import org.junit.jupiter.api.Test

internal class PacketTest {
    @Test
    fun `Safe guard against missing Kontekst key on packcet `() {
        Packet().apply { this.putValue(PacketKeys.KONTEKST_TYPE, Kontekst.CORONA.name) }.kontekst shouldBe Kontekst.CORONA

        Packet().kontekst shouldBe Kontekst.VEDTAK
    }
}
