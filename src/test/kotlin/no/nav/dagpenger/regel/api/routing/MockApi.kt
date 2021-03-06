package no.nav.dagpenger.regel.api.routing

import io.ktor.application.Application
import io.mockk.mockk
import io.prometheus.client.CollectorRegistry
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.api
import no.nav.dagpenger.regel.api.auth.AuthApiKeyVerifier
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.streams.DagpengerBehovProducer

internal fun MockApi(
    subsumsjonStore: SubsumsjonStore = mockk(),
    kafkaDagpengerBehovProducer: DagpengerBehovProducer = mockk(),
    authVerifier: AuthApiKeyVerifier = authApiKeyVerifier,
    healthChecks: List<HealthCheck> = mockk(),
): Application.() -> Unit {
    return fun Application.() {
        api(
            subsumsjonStore = subsumsjonStore,
            kafkaProducer = kafkaDagpengerBehovProducer,
            apiAuthApiKeyVerifier = authVerifier,
            healthChecks = healthChecks,
            config = Configuration(),
            prometheusMeterRegistry = CollectorRegistry(true)
        )
    }
}
