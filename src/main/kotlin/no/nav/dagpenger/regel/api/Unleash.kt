package no.nav.dagpenger.regel.api

import no.finn.unleash.DefaultUnleash
import no.finn.unleash.util.UnleashConfig

fun setupUnleash(unleashApiUrl: String): DefaultUnleash {
    val appName = "dp-regel-api"
    val unleashconfig = UnleashConfig.builder()
        .appName(appName)
        .instanceId(appName)
        .unleashAPI(unleashApiUrl)
        .build()

    return DefaultUnleash(unleashconfig)
}