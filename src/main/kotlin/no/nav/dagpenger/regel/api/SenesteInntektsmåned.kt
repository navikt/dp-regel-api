package no.nav.dagpenger.regel.api

import no.bekk.bekkopen.date.NorwegianDateUtil
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date

internal fun senesteInntektsmåned(beregningsdato: LocalDate): YearMonth {
    return Opptjeningsperiode(beregningsdato).sisteAvsluttendeKalenderMåned
}

// TODO: temporary - replaced by functionality from dp-inntekt-api - need a two step operation for now
data class Opptjeningsperiode(val beregningsdato: LocalDate) {
    private val antattRapporteringsFrist = LocalDate.of(beregningsdato.year, beregningsdato.month, 5)
    private val reellRapporteringsFrist: LocalDate =
        finnFørsteArbeidsdagEtterRapporterteringsFrist(antattRapporteringsFrist)
    private val månedSubtraksjon: Long = when {
        beregningsdato.isBefore(reellRapporteringsFrist) || beregningsdato.isEqual(reellRapporteringsFrist) -> 2
        else -> 1
    }

    val sisteAvsluttendeKalenderMåned: YearMonth = beregningsdato.minusMonths(månedSubtraksjon).toYearMonth()

    private fun finnFørsteArbeidsdagEtterRapporterteringsFrist(rapporteringsFrist: LocalDate): LocalDate {
        return if (rapporteringsFrist.erArbeidsdag()) rapporteringsFrist else finnFørsteArbeidsdagEtterRapporterteringsFrist(
            rapporteringsFrist.plusDays(1)
        )
    }

    private fun LocalDate.erArbeidsdag(): Boolean =
        NorwegianDateUtil.isWorkingDay(Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant()))

    private fun LocalDate.toYearMonth(): YearMonth = YearMonth.of(this.year, this.month)
}
