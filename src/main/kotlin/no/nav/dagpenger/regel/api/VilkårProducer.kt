package no.nav.dagpenger.regel.api

import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektParametere

interface VilkårProducer {
    fun produceMinsteInntektEvent(request: MinsteinntektParametere)
}