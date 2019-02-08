package no.nav.dagpenger.regel.api.minsteinntekt

import io.lettuce.core.api.sync.RedisCommands
import no.nav.dagpenger.regel.api.SubsumsjonNotFoundException
import no.nav.dagpenger.regel.api.moshiInstance

class MinsteinntektSubsumsjonerRedis(val redisCommands: RedisCommands<String, String>) : MinsteinntektSubsumsjoner {

    val jsonAdapter = moshiInstance.adapter(MinsteinntektSubsumsjon::class.java)

    override fun getMinsteinntektSubsumsjon(subsumsjonsId: String): MinsteinntektSubsumsjon {
        val json = redisCommands.getResult(subsumsjonsId)
        return jsonAdapter.fromJson(json) ?: throw SubsumsjonNotFoundException(
            "Could not find subsumsjon with id $subsumsjonsId")
    }

    override fun insertMinsteinntektSubsumsjon(minsteinntektSubsumsjon: MinsteinntektSubsumsjon) {
        val json = jsonAdapter.toJson(minsteinntektSubsumsjon)
        redisCommands.setResult(minsteinntektSubsumsjon.subsumsjonsId, json)
    }
}

fun RedisCommands<String, String>.getResult(subsumsjonsId: String): String {
    return get("result:$subsumsjonsId")
}

fun RedisCommands<String, String>.setResult(subsumsjonsId: String, json: String) {
    set("result:$subsumsjonsId", json)
}
