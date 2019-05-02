package no.nav.dagpenger.regel.api.v1.streams

import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.regel.api.APPLICATION_NAME
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import no.nav.dagpenger.regel.api.v1.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.v1.models.subsumsjonFrom
import no.nav.dagpenger.streams.River
import no.nav.dagpenger.streams.streamConfig
import org.apache.kafka.streams.kstream.Predicate
import java.util.*


internal class KafkaSubsumsjonConsumer(
    private val config: Configuration,
    private val subsumsjonStore: SubsumsjonStore
) : HealthCheck, River() {

    companion object {
        const val GRUNNLAG_RESULTAT = "grunnlagResultat"
        const val MANUELT_GRUNNLAG = "manueltGrunnlag"
        const val MINSTEINNTEKT_RESULTAT = "minsteinntektResultat"
        const val SATS_RESULTAT = "satsResultat"
        const val PERIODE_RESULTAT = "periodeResultat"

        val isManuellGrunnlag = { packet: Packet -> packet.hasField(MANUELT_GRUNNLAG) && hasGrunnlagAndSatsResult(packet) }

        val hasGrunnlagAndSatsResult = { packet: Packet -> packet.hasField(GRUNNLAG_RESULTAT) && packet.hasField(SATS_RESULTAT) }

        val hasCompleteResult = { packet: Packet -> hasGrunnlagAndSatsResult(packet) && packet.hasField(MINSTEINNTEKT_RESULTAT) && packet.hasField(PERIODE_RESULTAT) }

    }

    override val SERVICE_APP_ID: String
        get() = APPLICATION_NAME

    override fun status(): HealthStatus {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun filterPredicates(): List<Predicate<String, Packet>> {
        return listOf(Predicate { _: String, packet: Packet ->
            isManuellGrunnlag(packet) || hasCompleteResult(packet)
        })
    }


    override fun onPacket(packet: Packet): Packet {
        subsumsjonStore.insertSubsumsjon(subsumsjonFrom(packet))
    }


    override fun getConfig(): Properties {
        return streamConfig(
            appId = SERVICE_APP_ID,
            bootStapServerUrl = config.kafka.brokers,
            credential = config.kafka.credential()
        )
    }

}


