package no.nav.dagpenger.regel.api.sats

import java.time.LocalDate
import java.time.LocalDateTime

data class SatsSubsumsjon(
    val subsumsjonsId: String,
    val opprettet: LocalDateTime, // todo: ZonedDateTime?
    val utfort: LocalDateTime, // todo: ZonedDateTime?,
    val faktum: SatsFaktum,
    val resultat: SatsResultat
)

data class SatsResultat(
    val dagsats: Int,
    val ukesats: Int
)

data class SatsFaktum(
    val aktorId: String,
    val vedtakId: Int,
    val beregningsdato: LocalDate,
    val grunnlag: Int,
    val antallBarn: Int? = 0
)
