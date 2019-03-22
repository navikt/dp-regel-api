package no.nav.dagpenger.regel.api

import no.bekk.bekkopen.date.NorwegianDateUtil
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date

internal fun senesteInntektsmåned(beregningsdato: LocalDate): YearMonth {
    val firstWorkingDayAfterFifth = firstWorkingDayAfterFifth(beregningsdato.monthValue, beregningsdato.year)

    return if (beregningsdato.isBefore(firstWorkingDayAfterFifth)) YearMonth.of(beregningsdato.minusMonths(2).year, beregningsdato.minusMonths(2).month) else YearMonth.of(beregningsdato.minusMonths(1).year, beregningsdato.minusMonths(1).month)
}

internal fun firstWorkingDayAfterFifth(month: Int, year: Int): LocalDate {
    var firstWorkingDayAfterFifth: LocalDate = LocalDate.of(year, month, 6)
    while (!isWorkingDay(firstWorkingDayAfterFifth)) firstWorkingDayAfterFifth = firstWorkingDayAfterFifth.plusDays(1)
    return firstWorkingDayAfterFifth
}

internal fun isWorkingDay(localDate: LocalDate): Boolean =
        NorwegianDateUtil.isWorkingDay(Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))