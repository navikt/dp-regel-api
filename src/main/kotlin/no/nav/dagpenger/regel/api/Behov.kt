package no.nav.dagpenger.regel.api

import java.time.LocalDate

data class SubsumsjonsBehov (
    val aktørId: String,
    val vedtakId: Int,
    val beregningsDato: LocalDate,
    val avtjentVerneplikt: Boolean? = false,
    val eøs: Boolean? = false,
    val antallBarn: Int? = 0
)