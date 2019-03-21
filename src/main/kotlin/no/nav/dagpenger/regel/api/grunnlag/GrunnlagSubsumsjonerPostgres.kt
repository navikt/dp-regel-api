package no.nav.dagpenger.regel.api.grunnlag

import no.nav.dagpenger.regel.api.SubsumsjonNotFoundException
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.moshiInstance

class GrunnlagSubsumsjonerPostgres(val store: SubsumsjonStore) : GrunnlagSubsumsjoner {

    val jsonAdapter = moshiInstance.adapter(GrunnlagSubsumsjon::class.java)!!

    override fun getGrunnlagSubsumsjon(subsumsjonsId: String): GrunnlagSubsumsjon {
        val json = store.get(subsumsjonsId)
        return jsonAdapter.fromJson(json) ?: throw SubsumsjonNotFoundException(
                "Could not find subsumsjon with id $subsumsjonsId")
    }

    override fun insertGrunnlagSubsumsjon(grunnlagSubsumsjon: GrunnlagSubsumsjon) {
        val json = jsonAdapter.toJson(grunnlagSubsumsjon)
        store.insert(grunnlagSubsumsjon.subsumsjonsId, json)
    }
}
