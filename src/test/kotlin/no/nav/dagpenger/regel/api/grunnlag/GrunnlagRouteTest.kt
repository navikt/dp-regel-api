package no.nav.dagpenger.regel.api.grunnlag

import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.withTestApplication
import no.nav.dagpenger.regel.api.DagpengerBehovProducerDummy
import no.nav.dagpenger.regel.api.api
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektBeregninger

class GrunnlagRouteTest {

    private fun testApp(callback: TestApplicationEngine.() -> Unit) {
        //withTestApplication({ api(TasksDummy(), MinsteinntektBeregningerRedis(), GrunnlagBeregninger(), BehovProducerDummy()) }) { callback() }
    }
}