package no.nav.dagpenger.regel.api

import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektBeregningsRequest

interface VilkårProducer {
    fun produceMinsteInntektEvent(request: MinsteinntektBeregningsRequest)
}