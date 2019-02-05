package no.nav.dagpenger.regel.api.minsteinntekt

import io.lettuce.core.api.sync.RedisCommands
import no.nav.dagpenger.regel.api.moshiInstance

class MinsteinntektBeregningerRedis(val redisCommands: RedisCommands<String, String>) : MinsteinntektBeregninger {

    val jsonAdapter = moshiInstance.adapter(MinsteinntektBeregning::class.java)

    override fun getMinsteinntektBeregning(subsumsjonsId: String): MinsteinntektBeregning {
        val json = redisCommands.getResult(subsumsjonsId)
        return jsonAdapter.fromJson(json) ?: throw MinsteinntektBeregningNotFoundException(
            "Could not find subsumsjon with id $subsumsjonsId")
    }

    override fun setMinsteinntektBeregning(minsteinntektBeregning: MinsteinntektBeregning) {
        val json = jsonAdapter.toJson(minsteinntektBeregning)
        redisCommands.setResult(minsteinntektBeregning.subsumsjonsId, json)
    }
}

fun RedisCommands<String, String>.getResult(subsumsjonsId: String): String {
    return get("result:$subsumsjonsId")
}

fun RedisCommands<String, String>.setResult(subsumsjonsId: String, json: String) {
    set("result:$subsumsjonsId", json)
}
