package no.nav.dagpenger.regel.api

import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals

class ConfigurationTest {
    private fun withProps(props: Map<String, String>, test: () -> Unit) {
        for ((k, v) in props) {
            System.getProperties()[k] = v
        }
        test()
        for ((k, _) in props) {
            System.getProperties().remove(k)
        }
    }

    @Test
    fun `Configuration is loaded based on application profile`() {
        withProps(mapOf("NAIS_CLUSTER_NAME" to "dev-fss")) {
            with(Configuration()) {
                assertEquals(Profile.DEV, this.application.profile)
            }
        }

        withProps(mapOf("NAIS_CLUSTER_NAME" to "prod-fss")) {
            with(Configuration()) {
                assertEquals(Profile.PROD, this.application.profile)
            }
        }
    }

    @Test
    fun `Default configuration is LOCAL `() {
        with(Configuration()) {
            assertEquals(Profile.LOCAL, this.application.profile)
        }
    }

    @Test
    fun `System properties overrides hard coded properties`() {
        withProps(mapOf("database.host" to "SYSTEM_DB")) {
            with(Configuration()) {
                assertEquals("SYSTEM_DB", this.database.host)
            }
        }
    }

    @Test
    @Ignore
    fun `Overrides when enviroment variables are set`() {
        TODO("On the JVM it is hard to overidde environment variables.. Maybe use KotlinTest")
    }

    @Test
    @Ignore
    fun `Document mapping conventions when using environment variables`() {
        TODO("On the JVM it is hard to overidde environment variables.. Maybe use KotlinTest")
    }
}