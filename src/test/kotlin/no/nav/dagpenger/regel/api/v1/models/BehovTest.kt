package no.nav.dagpenger.regel.api.v1.models

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import no.nav.dagpenger.regel.api.v1.models.Behov.Mapper.toJson
import no.nav.dagpenger.regel.api.v1.models.Behov.Mapper.toPacket
import org.junit.jupiter.api.Test
import java.time.LocalDate


internal class BehovTest {

    @Test
    fun `Mapping from Behov to Packet with all fields`() {
        val behov = Behov("behovId", "aktørId", 1, LocalDate.now(), true, 1, 1)
        val packet = toPacket(behov)

        packet.getStringValue(PacketKeys.BEHOV_ID) shouldBe behov.behovId
        packet.getStringValue(PacketKeys.AKTØR_ID) shouldBe behov.aktørId
        packet.getIntValue(PacketKeys.VEDTAK_ID) shouldBe behov.vedtakId
        packet.getLocalDate(PacketKeys.BEREGNINGS_DATO) shouldBe behov.beregningsDato
        packet.getBoolean(PacketKeys.HAR_AVTJENT_VERNE_PLIKT) shouldBe behov.harAvtjentVerneplikt
        packet.getIntValue(PacketKeys.ANTALL_BARN) shouldBe behov.antallBarn
        packet.getIntValue(PacketKeys.MANUELT_GRUNNLAG) shouldBe behov.manueltGrunnlag
    }

    @Test
    fun `Mapping from Behov to Packet with nullable fields`() {
        val behov = Behov("behovId", "aktørId", 1, LocalDate.now(), null, null, null)
        val packet = toPacket(behov)

        packet.getStringValue(PacketKeys.BEHOV_ID) shouldBe behov.behovId
        packet.getStringValue(PacketKeys.AKTØR_ID) shouldBe behov.aktørId
        packet.getIntValue(PacketKeys.VEDTAK_ID) shouldBe behov.vedtakId
        packet.getLocalDate(PacketKeys.BEREGNINGS_DATO) shouldBe behov.beregningsDato
        packet.getNullableBoolean(PacketKeys.HAR_AVTJENT_VERNE_PLIKT) shouldBe null
        packet.getNullableIntValue(PacketKeys.ANTALL_BARN) shouldBe null
        packet.getNullableIntValue(PacketKeys.MANUELT_GRUNNLAG) shouldBe null
    }

    @Test
    fun `Mapping from Behov to JSON `() {
        Behov("behovId", "aktørId", 1, LocalDate.of(2011, 7, 22), true, 1, 1).apply {
            toJson(this) shouldBe """{"behovId":"behovId","aktørId":"aktørId","vedtakId":1,"beregningsDato":"2011-07-22","harAvtjentVerneplikt":true,"antallBarn":1,"manueltGrunnlag":1}"""
        }
    }

    @Test
    fun `Mapping from JSON to Behov `() {
        val behov = Behov.fromJson("""{"behovId":"behovId","aktørId":"aktørId","vedtakId":1,"beregningsDato":"2011-07-22","harAvtjentVerneplikt":true,"antallBarn":1,"manueltGrunnlag":1}""")

        behov shouldNotBe null
        behov?.let {
            behov.behovId shouldBe "behovId"
            behov.aktørId shouldBe "aktørId"
            behov.vedtakId shouldBe 1
            behov.beregningsDato shouldBe LocalDate.of(2011, 7, 22)
            behov.harAvtjentVerneplikt shouldBe true
            behov.antallBarn shouldBe 1
            behov.manueltGrunnlag shouldBe 1
        }
    }

    @Test
    fun `JSON serialization and deserialization of optional fields `() {
        Behov("behovId", "aktørId", 1, LocalDate.of(2011, 7, 22)).apply {
            val json = toJson(this)

            json shouldBe """{"behovId":"behovId","aktørId":"aktørId","vedtakId":1,"beregningsDato":"2011-07-22"}"""
            Behov.fromJson(json) shouldBe this
        }
    }
}
