package no.nav.dagpenger.regel.api.minsteinntekt.model
import java.time.YearMonth

data class InntektsPeriode(
    val foersteMaaned: YearMonth,
    val sisteMaaned: YearMonth
)
