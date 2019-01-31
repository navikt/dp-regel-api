package no.nav.dagpenger.regel.api.minsteinntekt.model
import java.time.LocalDateTime

data class MinsteinntektBeregning(
    val beregningsId: String,
    val opprettet: LocalDateTime, // todo: ZonedDateTime?
    val utfort: LocalDateTime, // todo: ZonedDateTime?,
    val parametere: MinsteinntektResultatParametere,
    val resultat: MinsteinntektResultat,
    val inntekt: Set<Inntekt>
)