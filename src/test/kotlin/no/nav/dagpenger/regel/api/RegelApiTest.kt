package no.nav.dagpenger.regel.api

import io.kotlintest.extensions.system.SystemPropertyTestListener
import io.kotlintest.specs.BehaviorSpec
import io.restassured.RestAssured
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import java.net.ServerSocket


private object postgresContainer {
    val instance by lazy {
        PostgreSQLContainer<Nothing>("postgres:11.2").apply {
            withEnv("POSTGRES_DB", "dp-regel-api")
            start()
        }
    }
}

private object kafkaContainer {
    val instance by lazy {
        KafkaContainer("5.2.0").apply {
            start()
        }
    }
}

class RegelApiTest : BehaviorSpec() {
    val freePort = ServerSocket(0).use { it.localPort }

    override fun listeners() = listOf(SystemPropertyTestListener(newProperties = mapOf(
        "database.port" to postgresContainer.instance.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT).toString(),
        "database.user" to postgresContainer.instance.username,
        "database.password" to postgresContainer.instance.password,
        "application.httpPort" to "$freePort",
        "kafka.bootstrap.servers" to kafkaContainer.instance.bootstrapServers
    )))

    init {
        Given("The application is running") {
            main()

            Then("It should be ready") {



            }


        }


    }

}

