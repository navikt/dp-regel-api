package no.nav.dagpenger.regel.api

import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektParametere

class DagpengerBehovProducerDummy : DagpengerBehovProducer {
    override fun produceMinsteInntektEvent(request: MinsteinntektParametere) {
    }
}