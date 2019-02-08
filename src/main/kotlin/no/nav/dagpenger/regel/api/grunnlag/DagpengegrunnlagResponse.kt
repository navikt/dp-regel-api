package no.nav.dagpenger.regel.api.grunnlag

import java.time.LocalDate
import java.time.LocalDateTime

data class DagpengegrunnlagResponse(
    val subsumsjonsId: String,
    val opprettet: LocalDateTime, // todo: ZonedDateTime?
    val utfort: LocalDateTime, // todo: ZonedDateTime?,
    val parametere: DagpengegrunnlagResultatParametere,
    val resultat: DagpengegrunnlagResultat
)

data class DagpengegrunnlagResultat(
    val avkortet: Int,
    val uavkortet: Int
)

data class DagpengegrunnlagResultatParametere(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: LocalDate,
    val inntektsId: String,
    val harAvtjentVerneplikt: Boolean? = false,
    val oppfyllerKravTilFangstOgFisk: Boolean? = false,
    val antallBarn: Int? = 0,
    val grunnlag: Int? = null
)