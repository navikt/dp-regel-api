package no.nav.dagpenger.regel.api

import no.nav.dagpenger.regel.api.grunnlag.GrunnlagRequestParametere
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektRequestParametere
import no.nav.dagpenger.regel.api.periode.PeriodeRequestParametere
import no.nav.dagpenger.regel.api.sats.SatsRequestParametere

interface DagpengerBehovProducer {
    fun produceMinsteInntektEvent(request: MinsteinntektRequestParametere): String
    fun producePeriodeEvent(request: PeriodeRequestParametere): String
    fun produceGrunnlagEvent(request: GrunnlagRequestParametere): String
    fun produceSatsEvent(request: SatsRequestParametere): String
}