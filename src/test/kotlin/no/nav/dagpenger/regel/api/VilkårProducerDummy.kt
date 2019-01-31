package no.nav.dagpenger.regel.api

import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektParametere

class VilkårProducerDummy : VilkårProducer {
    override fun produceMinsteInntektEvent(request: MinsteinntektParametere) {
    }
}