package no.nav.dagpenger.regel.api.minsteinntekt

import no.nav.dagpenger.regel.api.SubsumsjonNotFoundException
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.moshiInstance

class MinsteinntektSubsumsjonerPostgres(val store: SubsumsjonStore) : MinsteinntektSubsumsjoner {

    val jsonAdapter = moshiInstance.adapter(MinsteinntektSubsumsjon::class.java)!!

    override fun getMinsteinntektSubsumsjon(subsumsjonsId: String): MinsteinntektSubsumsjon {
        val json = store.get(subsumsjonsId)
        return jsonAdapter.fromJson(json) ?: throw SubsumsjonNotFoundException(
                "Could not find subsumsjon with id $subsumsjonsId")
    }

    override fun insertMinsteinntektSubsumsjon(minsteinntektSubsumsjon: MinsteinntektSubsumsjon) {
        val json = jsonAdapter.toJson(minsteinntektSubsumsjon)
        store.insert(minsteinntektSubsumsjon.subsumsjonsId, json)
    }
}
