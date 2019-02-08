package no.nav.dagpenger.regel.api.grunnlag

import io.lettuce.core.api.sync.RedisCommands
import no.nav.dagpenger.regel.api.moshiInstance

class DagpengegrunnlagBeregningerRedis(val redisCommands: RedisCommands<String, String>) : DagpengegrunnlagBeregninger {

    val jsonAdapter = moshiInstance.adapter(DagpengegrunnlagResponse::class.java)

    override fun getGrunnlagBeregning(subsumsjonsId: String): DagpengegrunnlagResponse {
        val json = redisCommands.getResult(subsumsjonsId)
        return jsonAdapter.fromJson(json) ?: throw GrunnlagBeregningNotFoundException(
            "Could not find subsumsjon with id $subsumsjonsId")
    }

    override fun setGrunnlagBeregning(dagpengegrunnlagBeregning: DagpengegrunnlagResponse) {
        val json = jsonAdapter.toJson(dagpengegrunnlagBeregning)
        redisCommands.setResult(dagpengegrunnlagBeregning.subsumsjonsId, json)
    }
}

fun RedisCommands<String, String>.getResult(subsumsjonsId: String): String {
    return get("result:$subsumsjonsId")
}

fun RedisCommands<String, String>.setResult(subsumsjonsId: String, json: String) {
    set("result:$subsumsjonsId", json)
}