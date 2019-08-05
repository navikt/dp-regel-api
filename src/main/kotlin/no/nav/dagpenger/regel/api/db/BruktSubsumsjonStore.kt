package no.nav.dagpenger.regel.api.db

import no.nav.dagpenger.regel.api.moshiInstance
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

interface BruktSubsumsjonStore {
    fun insertSubsumsjonBrukt(subsumsjonBrukt: SubsumsjonBrukt): Int
    fun getSubsumsjonBrukt(subsumsjonsId: String): SubsumsjonBrukt?
    fun subsumsjonBruktFraVedtak(vedtakId: String): SubsumsjonBrukt?
}

data class SubsumsjonBrukt(
    val id: String,
    val eksternId: String,
    val arenaTs: ZonedDateTime,
    val ts: Long
) {
    companion object Mapper {
        private val adapter = moshiInstance.adapter<SubsumsjonBrukt>(SubsumsjonBrukt::class.java)
        fun fromJson(json: String): SubsumsjonBrukt? {
            return adapter.fromJson(json)
        }
    }

    fun toJson(): String {
        return adapter.toJson(this)
    }
}

val isoFormat = DateTimeFormatter.ISO_ZONED_DATE_TIME

internal class SubsumsjonBruktNotFoundException(override val message: String) : RuntimeException(message)
