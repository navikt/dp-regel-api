package no.nav.dagpenger.regel.api.models.common

import java.time.YearMonth

data class InntektsPeriode(
    val førsteMåned: YearMonth, // todo: rename and create test that verifies the name
    val sisteMåned: YearMonth // todo: rename and create test that verifies the name
)
