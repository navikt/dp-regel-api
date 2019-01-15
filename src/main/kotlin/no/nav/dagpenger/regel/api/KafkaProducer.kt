package no.nav.dagpenger.regel.api

import no.nav.dagpenger.streams.Service

class KafkaProducer : Service {
    override val SERVICE_APP_ID = "dp-regel-api"

    override fun setupStreams(): org.apache.kafka.streams.KafkaStreams {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}