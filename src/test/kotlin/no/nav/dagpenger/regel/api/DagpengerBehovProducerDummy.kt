package no.nav.dagpenger.regel.api

import no.nav.dagpenger.regel.api.grunnlag.DagpengegrunnlagParametere
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektRequestParametere
import java.time.LocalDate

class DagpengerBehovProducerDummy : DagpengerBehovProducer {
    override fun produceDagpengegrunnlagEvent(request: DagpengegrunnlagParametere): SubsumsjonsBehov {
        return SubsumsjonsBehov("", "", 0, LocalDate.now())
    }

    override fun produceMinsteInntektEvent(request: MinsteinntektRequestParametere): SubsumsjonsBehov {
        return SubsumsjonsBehov("", "", 0, LocalDate.now())
    }
}