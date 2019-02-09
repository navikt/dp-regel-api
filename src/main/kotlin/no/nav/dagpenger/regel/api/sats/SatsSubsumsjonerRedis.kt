package no.nav.dagpenger.regel.api.sats

import io.lettuce.core.api.sync.RedisCommands
import no.nav.dagpenger.regel.api.SubsumsjonNotFoundException
import no.nav.dagpenger.regel.api.moshiInstance

class SatsSubsumsjonerRedis(val redisCommands: RedisCommands<String, String>) : SatsSubsumsjoner {

    val jsonAdapter = moshiInstance.adapter(SatsSubsumsjon::class.java)

    override fun getSatsSubsumsjon(subsumsjonsId: String): SatsSubsumsjon {
        val json = redisCommands.getResult(subsumsjonsId)
        return jsonAdapter.fromJson(json) ?: throw SubsumsjonNotFoundException(
            "Could not find subsumsjon with id $subsumsjonsId")
    }

    override fun insertSatsSubsumsjon(satsSubsumsjon: SatsSubsumsjon) {
        val json = jsonAdapter.toJson(satsSubsumsjon)
        redisCommands.setResult(satsSubsumsjon.subsumsjonsId, json)
    }
}

fun RedisCommands<String, String>.getResult(subsumsjonsId: String): String {
    return get("result:$subsumsjonsId")
}

fun RedisCommands<String, String>.setResult(subsumsjonsId: String, json: String) {
    set("result:$subsumsjonsId", json)
}
