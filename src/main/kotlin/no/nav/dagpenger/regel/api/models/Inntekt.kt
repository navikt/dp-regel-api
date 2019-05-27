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

// TODO: temporary - replaced by functionallity from dp-inntekt-api
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
