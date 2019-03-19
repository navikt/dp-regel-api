package no.nav.dagpenger.regel.api.sats

import no.nav.dagpenger.regel.api.SubsumsjonNotFoundException
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.moshiInstance

class SatsSubsumsjonerPostgres(val store: SubsumsjonStore) : SatsSubsumsjoner {

    val jsonAdapter = moshiInstance.adapter(SatsSubsumsjon::class.java)!!

    override fun getSatsSubsumsjon(subsumsjonsId: String): SatsSubsumsjon {
        val json = store.get(subsumsjonsId)
        return jsonAdapter.fromJson(json) ?: throw SubsumsjonNotFoundException(
                "Could not find subsumsjon with id $subsumsjonsId")
    }

    override fun insertSatsSubsumsjon(satsSubsumsjon: SatsSubsumsjon) {
        val json = jsonAdapter.toJson(satsSubsumsjon)
        store.insert(satsSubsumsjon.subsumsjonsId, json)
    }
}
