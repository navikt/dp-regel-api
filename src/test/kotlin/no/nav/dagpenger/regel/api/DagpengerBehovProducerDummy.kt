package no.nav.dagpenger.regel.api

import no.nav.dagpenger.regel.api.grunnlag.GrunnlagParametere
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektRequestParametere
import java.time.LocalDate

class DagpengerBehovProducerDummy : DagpengerBehovProducer {
    override fun produceGrunnlagEvent(request: GrunnlagParametere): SubsumsjonsBehov {
        return SubsumsjonsBehov("", "", 0, LocalDate.now())
    }

    override fun produceMinsteInntektEvent(request: MinsteinntektRequestParametere): SubsumsjonsBehov {
        return SubsumsjonsBehov("", "", 0, LocalDate.now())
    }
}