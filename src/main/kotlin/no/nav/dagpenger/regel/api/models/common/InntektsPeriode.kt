package no.nav.dagpenger.regel.api.arena.adapter.v1.models.common

import java.time.YearMonth

data class InntektsPeriode(
    val foersteMaaned: YearMonth? = null,
    val sisteMaaned: YearMonth
)
