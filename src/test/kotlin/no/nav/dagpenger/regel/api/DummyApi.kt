package no.nav.dagpenger.regel.api

import io.ktor.application.Application
import no.nav.dagpenger.regel.api.grunnlag.GrunnlagSubsumsjonerDummy
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektSubsumsjonerDummy
import no.nav.dagpenger.regel.api.periode.PeriodeSubsumsjonerDummy
import no.nav.dagpenger.regel.api.sats.SatsSubsumsjonerDummy
import no.nav.dagpenger.regel.api.tasks.TasksDummy

fun Application.dummyApi() {
    api(
        TasksDummy(),
        MinsteinntektSubsumsjonerDummy(),
        PeriodeSubsumsjonerDummy(),
        GrunnlagSubsumsjonerDummy(),
        SatsSubsumsjonerDummy(),
        DagpengerBehovProducerDummy()
    )
}