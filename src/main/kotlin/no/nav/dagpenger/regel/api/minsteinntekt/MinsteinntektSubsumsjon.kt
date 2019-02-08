package no.nav.dagpenger.regel.api.minsteinntekt

import no.nav.dagpenger.regel.api.InntektResponse
import no.nav.dagpenger.regel.api.InntektsPeriode
import java.time.LocalDate
import java.time.LocalDateTime

data class MinsteinntektSubsumsjon(
    val subsumsjonsId: String,
    val opprettet: LocalDateTime, // todo: ZonedDateTime?
    val utfort: LocalDateTime, // todo: ZonedDateTime?,
    val parametere: MinsteinntektFaktum,
    val resultat: MinsteinntektResultat,
    val inntekt: Set<InntektResponse>
)

data class MinsteinntektResultat(
    val oppfyllerKravTilMinsteArbeidsinntekt: Boolean
)

data class MinsteinntektFaktum(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: LocalDate,
    val harAvtjentVerneplikt: Boolean? = false,
    val oppfyllerKravTilFangstOgFisk: Boolean? = false,
    val bruktInntektsPeriode: InntektsPeriode? = null
)
