package no.nav.dagpenger.regel.api.models

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.events.inntekt.v1.KlassifisertInntektMåned
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class InntektsPeriodeTest {

    @Test
    fun `Mapping to JSON`() {
        InntektsPeriode(YearMonth.of(2019, 1), YearMonth.of(2019, 2)).toJson().let {
            val jsonMap = it as? Map<*, *>

            jsonMap shouldNotBe null
            jsonMap?.get("førsteMåned") shouldBe "2019-01"
            jsonMap?.get("sisteMåned") shouldBe "2019-02"
        }
    }

    @Test
    fun `Mapping from Packet`() {
        val inntektsPeriode = Packet().apply {
            putValue(PacketKeys.BRUKT_INNTEKTSPERIODE,
                mapOf(
                    Pair("førsteMåned", "2019-01"),
                    Pair("sisteMåned", "2019-02")
                ))
        }.let { InntektsPeriode.fromPacket(it) }

        inntektsPeriode shouldNotBe null
        inntektsPeriode?.førsteMåned shouldBe YearMonth.of(2019, 1)
        inntektsPeriode?.sisteMåned shouldBe YearMonth.of(2019, 2)
    }
}

internal class InntektTest {

    @Test
    fun `Extension function harAvvik`() {
        val avvik = KlassifisertInntektMåned(YearMonth.now(), listOf(), true)
        val notAvvik = KlassifisertInntektMåned(YearMonth.now(), listOf(), false)

        Inntekt("id", listOf(avvik, notAvvik)).harAvvik() shouldBe true
        Inntekt("id", listOf(notAvvik, notAvvik)).harAvvik() shouldBe false
        Inntekt("id", listOf()).harAvvik() shouldBe false
    }

    @Test
    fun `Mapping from Packet`() {
        val inntekt = inntektFrom(Packet().apply {
            putValue(PacketKeys.INNTEKT, mapOf(
                Pair("inntektsId", "id"),
                Pair("inntektsListe", listOf<String>()),
                Pair("manueltRedigert", true))
            )
        })

        inntekt shouldNotBe null
        inntekt?.let {
            it.inntektsId shouldBe "id"
            it.inntektsListe shouldBe listOf()
            it.manueltRedigert shouldBe true
        }
    }
}