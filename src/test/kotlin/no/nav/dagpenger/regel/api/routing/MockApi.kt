@file:Suppress("ktlint:standard:function-naming")

package no.nav.dagpenger.regel.api.routing

import io.ktor.server.application.Application
import io.mockk.mockk
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.api
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.streams.DagpengerBehovProducer

internal fun mockApi(
    subsumsjonStore: SubsumsjonStore = mockk(),
    kafkaDagpengerBehovProducer: DagpengerBehovProducer = mockk(),
    healthChecks: List<HealthCheck> = mockk(),
): Application.() -> Unit =
    { ->
        api(
            subsumsjonStore = subsumsjonStore,
            kafkaProducer = kafkaDagpengerBehovProducer,
            healthChecks = healthChecks,
            config = Configuration,
        )
    }
