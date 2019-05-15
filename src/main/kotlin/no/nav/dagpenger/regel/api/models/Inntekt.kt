package no.nav.dagpenger.regel.api.models

import no.bekk.bekkopen.date.NorwegianDateUtil
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.regel.api.moshiInstance
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date

internal enum class PeriodeNumber(val number: Int) {
    ONE(1), TWO(2), THREE(3)
}

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

internal fun Inntekt.harAvvik(): Boolean = this.inntektsListe.any { it.harAvvik ?: false }

internal fun senesteInntektsmåned(beregningsdato: LocalDate): YearMonth {
    val firstWorkingDayAfterFifth = firstWorkingDayAfterFifth(beregningsdato.monthValue, beregningsdato.year)

    return if (beregningsdato.isBefore(firstWorkingDayAfterFifth)) YearMonth.of(beregningsdato.minusMonths(2).year, beregningsdato.minusMonths(2).month) else YearMonth.of(beregningsdato.minusMonths(1).year, beregningsdato.minusMonths(1).month)
}

private fun firstWorkingDayAfterFifth(month: Int, year: Int): LocalDate {
    var firstWorkingDayAfterFifth: LocalDate = LocalDate.of(year, month, 6)
    while (!isWorkingDay(firstWorkingDayAfterFifth)) firstWorkingDayAfterFifth = firstWorkingDayAfterFifth.plusDays(1)
    return firstWorkingDayAfterFifth
}

private fun isWorkingDay(localDate: LocalDate): Boolean =
    NorwegianDateUtil.isWorkingDay(Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
