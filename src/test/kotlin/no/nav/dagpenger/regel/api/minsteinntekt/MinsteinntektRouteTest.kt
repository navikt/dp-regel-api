package no.nav.dagpenger.regel.api.minsteinntekt

import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.withTestApplication
import no.nav.dagpenger.regel.api.VilkårProducerDummy
import no.nav.dagpenger.regel.api.api
import no.nav.dagpenger.regel.api.grunnlag.GrunnlagBeregninger
import no.nav.dagpenger.regel.api.tasks.TasksDummy

class MinsteinntektRouteTest {

    private fun testApp(callback: TestApplicationEngine.() -> Unit) {
        withTestApplication({ api(TasksDummy(), MinsteinntektBeregninger(), GrunnlagBeregninger(), VilkårProducerDummy()) }) { callback() }
    }
}
