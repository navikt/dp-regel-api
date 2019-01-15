package no.nav.dagpenger.regel.api

import mu.KotlinLogging
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.Service
import no.nav.dagpenger.streams.streamConfig
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import java.util.Properties

private val LOGGER = KotlinLogging.logger {}

class KafkaProducer(val env: Environment) : Service() {
    override val SERVICE_APP_ID = "dp-regel-api"

//    companion object {
//        @JvmStatic
//        fun main(args: Array<String>) {
//            val env = Environment()
//            val journalpostArkiv: JournalpostArkiv = JournalpostArkivJoark(
//                env.journalfoerinngaaendeV1Url,
//                StsOidcClient(env.oicdStsUrl, env.username, env.password)
//            )
//            val service = JoarkMottak(env, journalpostArkiv)
//            service.start()
//        }
//    }

    override fun setupStreams(): KafkaStreams {
        LOGGER.info { "Initiating start of $SERVICE_APP_ID" }
        val builder = StreamsBuilder()

//        val inngåendeJournalposter = builder.consumeGenericTopic(
//            JOARK_EVENTS.copy(
//                name = if (env.fasitEnvironmentName.isBlank()) JOARK_EVENTS.name else JOARK_EVENTS.name + "-" + env.fasitEnvironmentName
//            ), env.schemaRegistryUrl
//        )

//        inngåendeJournalposter
//            .peek { _, value ->
//                LOGGER.info(
//                    "Received journalpost with journalpost id: ${value.get("journalpostId")} and tema: ${value.get(
//                        "temaNytt"
//                    )}, hendelsesType: ${value.get("hendelsesType")}"
//                )
//            }
//            .peek { key, value -> LOGGER.info("Producing ${value.javaClass.simpleName} with key '$key' ") }
//            .toTopic(INNGÅENDE_JOURNALPOST, env.schemaRegistryUrl)

        return KafkaStreams(builder.build(), this.getConfig())
    }

    fun processRegel(request: Any) {
        // TODO
    }

    override fun getConfig(): Properties {
        return streamConfig(
            appId = SERVICE_APP_ID,
            bootStapServerUrl = env.bootstrapServersUrl,
            credential = KafkaCredential(env.username, env.password)
        )
    }
}