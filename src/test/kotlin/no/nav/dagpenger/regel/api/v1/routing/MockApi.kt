package no.nav.dagpenger.regel.api.v1.routing

import io.ktor.application.Application
import io.mockk.mockk
import no.nav.dagpenger.regel.api.apiv1
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.v1.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.v1.streams.DagpengerBehovProducer

internal fun MockApi(
    subsumsjonStore: SubsumsjonStore = mockk(),
    kafkaDagpengerBehovProducer: DagpengerBehovProducer = mockk(),
    healthChecks: List<HealthCheck> = mockk()

): Application.() -> Unit {
    return fun Application.() {
        apiv1(
            subsumsjonStore,
            kafkaDagpengerBehovProducer,
            healthChecks
        )
    }
}
