package no.nav.dagpenger.regel.api

import no.finn.unleash.DefaultUnleash
import no.finn.unleash.Unleash
import no.finn.unleash.strategy.Strategy
import no.finn.unleash.util.UnleashConfig

const val FORHØYA_SATS_TOGGLE = "dp-regel-api.forhoyaSats"
fun Unleash.forhøyaSats() = isEnabled(FORHØYA_SATS_TOGGLE)

fun setupUnleash(unleashApiUrl: String): DefaultUnleash {
    val appName = "dp-regel-api"
    val unleashconfig = UnleashConfig.builder()
        .appName(appName)
        .instanceId(appName)
        .unleashAPI(unleashApiUrl)
        .build()

    return DefaultUnleash(unleashconfig, ByClusterStrategy(Cluster.current))
}

class ByClusterStrategy(private val currentCluster: Cluster) : Strategy {
    override fun getName(): String = "byCluster"

    override fun isEnabled(parameters: Map<String, String>?): Boolean {
        val clustersParameter = parameters?.get("cluster") ?: return false
        val alleClustere = clustersParameter.split(",").map { it.trim() }.map { it.toLowerCase() }.toList()
        return alleClustere.contains(currentCluster.asString())
    }
}

enum class Cluster {
    DEV_FSS, PROD_FSS, ANNET;

    companion object {
        val current: Cluster by lazy {
            when (System.getenv("NAIS_CLUSTER_NAME")) {
                "dev-fss" -> DEV_FSS
                "prod-fss" -> PROD_FSS
                else -> ANNET
            }
        }
    }

    fun asString(): String = name.toLowerCase().replace("_", "-")
}
