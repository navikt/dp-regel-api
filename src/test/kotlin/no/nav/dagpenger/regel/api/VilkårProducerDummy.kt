package no.nav.dagpenger.regel.api

import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektBeregningsRequest

class VilkårProducerDummy : VilkårProducer {
    override fun produceMinsteInntektEvent(request: MinsteinntektBeregningsRequest) {
    }
}