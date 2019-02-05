package no.nav.dagpenger.regel.api

import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektParametere

interface DagpengerBehovProducer {
    fun produceMinsteInntektEvent(request: MinsteinntektParametere): SubsumsjonsBehov
}