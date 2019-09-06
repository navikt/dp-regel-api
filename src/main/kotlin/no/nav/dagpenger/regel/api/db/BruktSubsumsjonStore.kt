package no.nav.dagpenger.regel.api.db

import mu.KotlinLogging
import no.nav.dagpenger.regel.api.moshiInstance
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

interface BruktSubsumsjonStore {
    fun insertSubsumsjonBrukt(internSubsumsjonBrukt: InternSubsumsjonBrukt): Int
    fun getSubsumsjonBrukt(subsumsjonsId: String): InternSubsumsjonBrukt?
    fun listSubsumsjonBrukt(): List<InternSubsumsjonBrukt>
    fun subsumsjonBruktFraBehandlingsId(behandlingsId: String): List<InternSubsumsjonBrukt>
    fun internTilEksternSubsumsjonBrukt(v1: EksternSubsumsjonBrukt): InternSubsumsjonBrukt
}

data class EksternSubsumsjonBrukt(
    val id: String,
    val eksternId: Long,
    val arenaTs: ZonedDateTime,
    val ts: Long
) {
    companion object Mapper {
        private val LOGGER = KotlinLogging.logger { }
        private val adapter = moshiInstance.adapter<EksternSubsumsjonBrukt>(EksternSubsumsjonBrukt::class.java)
        fun fromJson(json: String): EksternSubsumsjonBrukt? {
            return runCatching { adapter.fromJson(json) }.onFailure { e -> LOGGER.warn("Failed to convert string to object", e) }.getOrNull()
        }
    }

    fun toJson(): String {
        return adapter.toJson(this)
    }
}

data class InternSubsumsjonBrukt(val id: String, val behandlingsId: String, val arenaTs: ZonedDateTime, val created: ZonedDateTime? = null) {
    companion object Mapper {
        private val LOGGER = KotlinLogging.logger { }
        private val adapter = moshiInstance.adapter<InternSubsumsjonBrukt>(InternSubsumsjonBrukt::class.java)
        fun fromJson(json: String): InternSubsumsjonBrukt? {
            return runCatching { adapter.fromJson(json) }.onFailure { e -> LOGGER.warn("Failed to convert string to object", e) }.getOrNull()
        }
    }

    fun toJson(): String {
        return adapter.toJson(this)
    }
}

val isoFormat = DateTimeFormatter.ISO_ZONED_DATE_TIME

internal class SubsumsjonBruktNotFoundException(override val message: String) : RuntimeException(message)
