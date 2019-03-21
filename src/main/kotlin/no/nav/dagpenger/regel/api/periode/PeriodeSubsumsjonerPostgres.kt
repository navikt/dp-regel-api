package no.nav.dagpenger.regel.api.periode

import no.nav.dagpenger.regel.api.SubsumsjonNotFoundException
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.moshiInstance

class PeriodeSubsumsjonerPostgres(val store: SubsumsjonStore) : PeriodeSubsumsjoner {

    val jsonAdapter = moshiInstance.adapter(PeriodeSubsumsjon::class.java)!!

    override fun getPeriodeSubsumsjon(subsumsjonsId: String): PeriodeSubsumsjon {
        val json = store.get(subsumsjonsId)
        return jsonAdapter.fromJson(json) ?: throw SubsumsjonNotFoundException(
            "Could not find subsumsjon with id $subsumsjonsId")
    }

    override fun insertPeriodeSubsumsjon(periodeSubsumsjon: PeriodeSubsumsjon) {
        val json = jsonAdapter.toJson(periodeSubsumsjon)
        store.insert(periodeSubsumsjon.subsumsjonsId, json)
    }
}
