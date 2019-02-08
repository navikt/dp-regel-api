package no.nav.dagpenger.regel.api.periode

import io.lettuce.core.api.sync.RedisCommands
import no.nav.dagpenger.regel.api.SubsumsjonNotFoundException
import no.nav.dagpenger.regel.api.moshiInstance

class PeriodeSubsumsjonerRedis(val redisCommands: RedisCommands<String, String>) : PeriodeSubsumsjoner {

    val jsonAdapter = moshiInstance.adapter(PeriodeSubsumsjon::class.java)

    override fun getPeriodeSubsumsjon(subsumsjonsId: String): PeriodeSubsumsjon {
        val json = redisCommands.getResult(subsumsjonsId)
        return jsonAdapter.fromJson(json) ?: throw SubsumsjonNotFoundException(
            "Could not find subsumsjon with id $subsumsjonsId")
    }

    override fun insertPeriodeSubsumsjon(periodeSubsumsjon: PeriodeSubsumsjon) {
        val json = jsonAdapter.toJson(periodeSubsumsjon)
        redisCommands.setResult(periodeSubsumsjon.subsumsjonsId, json)
    }
}

fun RedisCommands<String, String>.getResult(subsumsjonsId: String): String {
    return get("result:$subsumsjonsId")
}

fun RedisCommands<String, String>.setResult(subsumsjonsId: String, json: String) {
    set("result:$subsumsjonsId", json)
}
