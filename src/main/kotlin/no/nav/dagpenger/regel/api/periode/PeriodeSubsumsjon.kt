package no.nav.dagpenger.regel.api.periode

import no.nav.dagpenger.regel.api.InntektResponse
import no.nav.dagpenger.regel.api.InntektsPeriode
import java.time.LocalDate
import java.time.LocalDateTime

data class PeriodeSubsumsjon(
    val subsumsjonsId: String,
    val opprettet: LocalDateTime, // todo: ZonedDateTime?
    val utfort: LocalDateTime, // todo: ZonedDateTime?,
    val parametere: PeriodeFaktum,
    val resultat: PeriodeResultat,
    val inntekt: Set<InntektResponse>
)

data class PeriodeResultat(
    val antallUker: Int
)

data class PeriodeFaktum(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: LocalDate,
    val harAvtjentVerneplikt: Boolean? = false,
    val oppfyllerKravTilFangstOgFisk: Boolean? = false,
    val bruktInntektsPeriode: InntektsPeriode? = null
)
