package no.nav.dagpenger.regel.api.v1.models

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
        val adapter = moshiInstance.adapter<InntektsPeriode>(InntektsPeriode::class.java)
    }
}

