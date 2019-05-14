package no.nav.dagpenger.regel.api.models

import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.regel.api.moshiInstance
import java.math.BigDecimal
import java.time.YearMonth

internal enum class PeriodeNumber(val number: Int) {
    ONE(1), TWO(2), THREE(3)
}

internal data class InntektResponse(
    val inntekt: BigDecimal,
    val periode: PeriodeNumber,
    val inntektsPeriode: InntektsPeriode,
    val inneholderFangstOgFisk: Boolean,
    val andel: BigDecimal? //todo: Give better name
)

internal data class InntektsPeriode(
    val førsteMåned: YearMonth, // todo: rename and create test that verifies the name
    val sisteMåned: YearMonth // todo: rename and create test that verifies the name
) {
    companion object Mapper {
        private val adapter = moshiInstance.adapter<InntektsPeriode>(InntektsPeriode::class.java)

        fun fromPacket(packet: Packet): InntektsPeriode? = packet.getNullableObjectValue(PacketKeys.BRUKT_INNTEKTSPERIODE) { json ->
            adapter.fromJson(json as String)
        }

        fun toJson(inntektsPeriode: InntektsPeriode) = adapter.toJson(inntektsPeriode)
    }

    fun toJson(): String = toJson(this)
}

internal fun inntektFrom(packet: Packet): Inntekt? = packet.getNullableObjectValue(PacketKeys.INNTEKT) { json ->
    moshiInstance.adapter<Inntekt>(Inntekt::class.java).fromJson(json as String)
}
