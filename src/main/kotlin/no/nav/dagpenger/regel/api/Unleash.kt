package no.nav.dagpenger.regel.api

import no.finn.unleash.DefaultUnleash
import no.finn.unleash.Unleash
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

const val FORHØYA_SATS_TOGGLE = "dp-regel-api.forhoyaSats"

fun Unleash.forhøyaSats() = isEnabled(FORHØYA_SATS_TOGGLE)
/*
import no.finn.unleash.strategy.Strategy;
import java.util.Map;

public class IsNotProdStrategy implements Strategy {
    private final String env;

    public IsNotProdStrategy(String env) {
        this.env = env;
    }

    @Override
    public String getName() {
        return "isNotProd";
    }

    @Override
    public boolean isEnabled(Map<String, String> map) {
        return !isProd(this.env);
    }

    private boolean isProd(String environment) {
        return "p".equalsIgnoreCase(environment);
    }
}*/
