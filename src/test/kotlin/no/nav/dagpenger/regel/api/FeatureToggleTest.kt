package no.nav.dagpenger.regel.api

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class FeatureToggleTest {

    @Test
    fun `ByClusterStrategy skal tolke respons fra Unleash riktig`() {
        val byClusterStrategy = ByClusterStrategy(Cluster.DEV_FSS)
        val clusterSlåttPå = mapOf(Pair("cluster", "dev-fss,prod-fss"))
        byClusterStrategy.isEnabled(clusterSlåttPå) shouldBe true

        val clusterSlåttAv = mapOf(Pair("cluster", "prod-fss"))
        byClusterStrategy.isEnabled(clusterSlåttAv) shouldBe false
    }
}
