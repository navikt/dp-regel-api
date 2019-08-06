package no.nav.dagpenger.regel.api.db

import mu.KotlinLogging
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
        private val LOGGER = KotlinLogging.logger { }
        private val adapter = moshiInstance.adapter<SubsumsjonBrukt>(SubsumsjonBrukt::class.java)
        fun fromJson(json: String): SubsumsjonBrukt? {
            return runCatching { adapter.fromJson(json) }.onFailure { e -> LOGGER.warn("Failed to convert string to object", e) }.getOrNull()
        }
    }

    fun toJson(): String {
        return adapter.toJson(this)
    }
}

val isoFormat = DateTimeFormatter.ISO_ZONED_DATE_TIME

internal class SubsumsjonBruktNotFoundException(override val message: String) : RuntimeException(message)
