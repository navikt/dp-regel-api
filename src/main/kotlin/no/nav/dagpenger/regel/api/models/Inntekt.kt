package no.nav.dagpenger.regel.api.models

import java.time.YearMonth
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.regel.api.moshiInstance

internal enum class PeriodeNumber(val number: Int) {
    ONE(1), TWO(2), THREE(3)
}

data class InntektsPeriode(
    val førsteMåned: YearMonth, // todo: rename and create test that verifies the name
    val sisteMåned: YearMonth // todo: rename and create test that verifies the name
) {
    companion object Mapper {
        private val adapter = moshiInstance.adapter<InntektsPeriode>(InntektsPeriode::class.java)

        fun fromPacket(packet: Packet): InntektsPeriode? = packet.getNullableObjectValue(PacketKeys.BRUKT_INNTEKTSPERIODE) { json ->
            adapter.fromJsonValue(json)
        }

        fun toJson(inntektsPeriode: InntektsPeriode) = adapter.toJsonValue(inntektsPeriode)!!
    }

    fun toJson(): Any = toJson(this)
}

internal fun inntektFrom(packet: Packet): Inntekt? = packet.getNullableObjectValue(PacketKeys.INNTEKT) { json ->
    moshiInstance.adapter<Inntekt>(Inntekt::class.java).fromJsonValue(json)
}

internal fun Inntekt.harAvvik(): Boolean = this.inntektsListe.any { it.harAvvik ?: false }
