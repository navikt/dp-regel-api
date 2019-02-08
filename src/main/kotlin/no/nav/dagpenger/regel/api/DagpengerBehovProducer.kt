package no.nav.dagpenger.regel.api

import no.nav.dagpenger.regel.api.grunnlag.GrunnlagRequestParametere
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektRequestParametere
import no.nav.dagpenger.regel.api.periode.PeriodeRequestParametere

interface DagpengerBehovProducer {
    fun produceMinsteInntektEvent(request: MinsteinntektRequestParametere): SubsumsjonsBehov
    fun producePeriodeEvent(request: PeriodeRequestParametere): SubsumsjonsBehov
    fun produceGrunnlagEvent(request: GrunnlagRequestParametere): SubsumsjonsBehov
}