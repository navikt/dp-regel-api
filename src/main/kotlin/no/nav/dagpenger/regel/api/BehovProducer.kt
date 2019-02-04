package no.nav.dagpenger.regel.api

import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektParametere

interface BehovProducer {
    fun produceMinsteInntektEvent(request: MinsteinntektParametere): SubsumsjonsBehov
}