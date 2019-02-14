package no.nav.dagpenger.regel.api.grunnlag

import io.lettuce.core.api.sync.RedisCommands
import no.nav.dagpenger.regel.api.SubsumsjonNotFoundException
import no.nav.dagpenger.regel.api.moshiInstance

class GrunnlagSubsumsjonerRedis(val redisCommands: RedisCommands<String, String>) : GrunnlagSubsumsjoner {

    val jsonAdapter = moshiInstance.adapter(GrunnlagSubsumsjon::class.java)

    override fun getGrunnlagSubsumsjon(subsumsjonsId: String): GrunnlagSubsumsjon {
        val json = redisCommands.getResult(subsumsjonsId)
        return jsonAdapter.fromJson(json) ?: throw SubsumsjonNotFoundException(
            "Could not find subsumsjon with id $subsumsjonsId")
    }

    override fun insertGrunnlagSubsumsjon(grunnlagSubsumsjon: GrunnlagSubsumsjon) {
        val json = jsonAdapter.toJson(grunnlagSubsumsjon)
        redisCommands.setResult(grunnlagSubsumsjon.subsumsjonsId, json)
    }
}

fun RedisCommands<String, String>.getResult(subsumsjonsId: String): String {
    return get("result:$subsumsjonsId")
}

fun RedisCommands<String, String>.setResult(subsumsjonsId: String, json: String) {
    set("result:$subsumsjonsId", json)
}