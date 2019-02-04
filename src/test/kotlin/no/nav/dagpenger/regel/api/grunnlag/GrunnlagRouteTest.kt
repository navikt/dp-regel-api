package no.nav.dagpenger.regel.api.grunnlag

import io.ktor.server.testing.TestApplicationEngine

class GrunnlagRouteTest {

    private fun testApp(callback: TestApplicationEngine.() -> Unit) {
        //withTestApplication({ api(TasksDummy(), MinsteinntektBeregningerRedis(), GrunnlagBeregninger(), BehovProducerDummy()) }) { callback() }
    }
}