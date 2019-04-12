package no.nav.dagpenger.regel.api.v1.models

import com.squareup.moshi.JsonAdapter
import no.nav.dagpenger.regel.api.moshiInstance

val subsumsjonJsonAdapter: JsonAdapter<Subsumsjon> = moshiInstance.adapter<Subsumsjon>(Subsumsjon::class.java)

data class Subsumsjon(val id: String, val behovId: String)

class SubsumsjonSerDerException(message: String) : RuntimeException(message)
