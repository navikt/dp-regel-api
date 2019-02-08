package no.nav.dagpenger.regel.api

import no.nav.dagpenger.regel.api.grunnlag.GrunnlagParametere
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektRequestParametere

interface DagpengerBehovProducer {
    fun produceMinsteInntektEvent(request: MinsteinntektRequestParametere): SubsumsjonsBehov
    fun produceGrunnlagEvent(request: GrunnlagParametere): SubsumsjonsBehov
}