package no.nav.dagpenger.regel.api

import no.nav.dagpenger.regel.api.grunnlag.GrunnlagRequestParametere
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektRequestParametere
import no.nav.dagpenger.regel.api.periode.PeriodeRequestParametere
import java.time.LocalDate

class DagpengerBehovProducerDummy : DagpengerBehovProducer {
    override fun produceGrunnlagEvent(request: GrunnlagRequestParametere): SubsumsjonsBehov {
        return SubsumsjonsBehov("", "", 0, LocalDate.now())
    }

    override fun producePeriodeEvent(request: PeriodeRequestParametere): SubsumsjonsBehov {
        return SubsumsjonsBehov("", "", 0, LocalDate.now())
    }

    override fun produceMinsteInntektEvent(request: MinsteinntektRequestParametere): SubsumsjonsBehov {
        return SubsumsjonsBehov("", "", 0, LocalDate.now())
    }
}