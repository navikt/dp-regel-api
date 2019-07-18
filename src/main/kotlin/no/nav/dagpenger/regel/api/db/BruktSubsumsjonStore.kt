package no.nav.dagpenger.regel.api.db

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal interface BruktSubsumsjonStore {
    fun insertSubsumsjonBrukt(subsumsjonBrukt: SubsumsjonBrukt): Int
    fun getSubsumsjonBrukt(subsumsjonsId: String): SubsumsjonBrukt?
    fun subsumsjonBruktFraVedtak(vedtakId: String): SubsumsjonBrukt?
}

data class SubsumsjonBrukt(
    val id: String,
    val eksternId: String,
    val arenaTs: ZonedDateTime,
    val ts: Long
)
val isoFormat = DateTimeFormatter.ISO_ZONED_DATE_TIME

internal class SubsumsjonBruktNotFoundException(override val message: String) : RuntimeException(message)
