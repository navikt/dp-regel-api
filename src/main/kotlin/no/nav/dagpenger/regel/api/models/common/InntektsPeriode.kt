package no.nav.dagpenger.regel.api.models.common

import java.time.YearMonth

data class InntektsPeriode(
    val foersteMaaned: YearMonth, // todo: rename and create test that verifies the name
    val sisteMaaned: YearMonth // todo: rename and create test that verifies the name
)
