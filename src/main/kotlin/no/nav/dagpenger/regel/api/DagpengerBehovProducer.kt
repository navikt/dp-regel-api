package no.nav.dagpenger.regel.api

import no.nav.dagpenger.regel.api.grunnlag.DagpengegrunnlagParametere
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektRequestParametere

interface DagpengerBehovProducer {
    fun produceMinsteInntektEvent(request: MinsteinntektRequestParametere): SubsumsjonsBehov
    fun produceDagpengegrunnlagEvent(request: DagpengegrunnlagParametere): SubsumsjonsBehov
}