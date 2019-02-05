package no.nav.dagpenger.regel.api

import java.time.LocalDate

data class SubsumsjonsBehov (
    val behovId: String,
    val aktørId: String,
    val vedtakId: Int,
    val beregningsDato: LocalDate,
    val avtjentVerneplikt: Boolean? = false,
    val eøs: Boolean? = false,
    val antallBarn: Int? = 0,
    val inntekt: Int? = null,

    var tasks: List<String>? = null,

    var minsteinntektSubsumsjon: MinsteinntektSubsumsjon? = null
)

data class MinsteinntektSubsumsjon(
    val sporingsId: String,
    val subsumsjonsId: String,
    val regelIdentifikator: String,
    val oppfyllerMinsteinntektKrav: Boolean
)