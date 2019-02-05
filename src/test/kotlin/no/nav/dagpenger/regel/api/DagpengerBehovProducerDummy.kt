package no.nav.dagpenger.regel.api

import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektParametere
import java.time.LocalDate

class DagpengerBehovProducerDummy : DagpengerBehovProducer {
    override fun produceMinsteInntektEvent(request: MinsteinntektParametere): SubsumsjonsBehov {
        return SubsumsjonsBehov("", "", 0, LocalDate.now())
    }
}