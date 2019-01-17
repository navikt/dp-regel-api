package no.nav.dagpenger.regel.api

import com.google.gson.Gson
import io.lettuce.core.api.sync.RedisCommands
import mu.KotlinLogging
import no.nav.dagpenger.events.avro.MinsteinntektParametere
import no.nav.dagpenger.events.avro.MinsteinntektResultat
import no.nav.dagpenger.events.avro.RegelType
import no.nav.dagpenger.events.avro.Vilkår
import no.nav.dagpenger.regel.api.grunnlag.Utfall
import no.nav.dagpenger.regel.api.minsteinntekt.InntektsPeriode
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektBeregningsRequest
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektBeregningsResult
import no.nav.dagpenger.regel.api.tasks.Tasks
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.Topics
import no.nav.dagpenger.streams.consumeTopic
import no.nav.dagpenger.streams.streamConfig
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import java.util.Properties

private val LOGGER = KotlinLogging.logger {}

class VilkårKafkaConsumer(val env: Environment, val redisCommands: RedisCommands<String, String>, val tasks: Tasks) {

    val SERVICE_APP_ID = "dp-regel-api"

    fun start() {
        val builder = StreamsBuilder()
        val vilkårEvents = builder.consumeTopic(Topics.VILKÅR_EVENT, env.schemaRegistryUrl)

        vilkårEvents
            .peek { key, value -> LOGGER.info("Processing vilkår with id ${value.getId()}") }
            .filter { _, vilkår -> shouldBeProcessed(vilkår) }
            .foreach { _, vilkår -> storeResult(vilkår) }

        val streams = KafkaStreams(builder.build(), this.getConfig())
        streams.start()
    }

    fun getConfig(): Properties {
        val props = streamConfig(
            appId = SERVICE_APP_ID,
            bootStapServerUrl = env.bootstrapServersUrl,
            credential = KafkaCredential(env.username, env.password))
        return props
    }

    fun shouldBeProcessed(vilkår: Vilkår): Boolean {
        return vilkår.getRegler().any { regel -> regel.getResultat() != null }
    }

    fun storeResult(vilkår: Vilkår) {
        vilkår.getRegler().forEach { regel ->
            val result = when (regel.getType()) {
                RegelType.FIRE_FIRE -> mapToMinsteinntekt(vilkår, regel)
                else -> throw UnsupportedRegelException("Unsupported regel: ${regel.getType()}")
            }

            redisCommands.setResult(regel.getBeregningsId(), result)
        }
    }

    fun mapToMinsteinntekt(vilkår: Vilkår, regel: no.nav.dagpenger.events.avro.Regel): MinsteinntektBeregningsResult {
        val parametere = regel.getParametere() as MinsteinntektParametere
        val resultat = regel.getResultat() as MinsteinntektResultat
        return MinsteinntektBeregningsResult(
            regel.getBeregningsId(),
            Utfall(resultat.getOppfyllerKravetTilMinsteArbeidsinntekt(), resultat.getPeriodeAntallUker()),
            "2018-12-26T14:42:09Z",
            "2018-12-26T14:42:09Z",
            MinsteinntektBeregningsRequest(
                parametere.getAktorId(),
                parametere.getVedtakId().toInt(),
                parametere.getBeregningsdato(),
                parametere.getInntektsId(),
                InntektsPeriode(
                    parametere.getBruktinntektsPeriode().getFørsteMåned(),
                    parametere.getBruktinntektsPeriode().getSisteMåned()),
                parametere.getHarAvtjentVerneplikt(),
                parametere.getOppfyllerKravTilFangstOgFisk(),
                parametere.getHarArbeidsperiodeEøsSiste12Måneder()
            )
        )
    }
}

fun RedisCommands<String, String>.setResult(id: String, data: Any) {
    set("result:$id", Gson().toJson(data))
}

class UnsupportedRegelException(override val message: String) : RuntimeException(message)
