package no.nav.dagpenger.regel.api.db

import mu.KotlinLogging
import no.nav.dagpenger.regel.api.moshiInstance
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

interface BruktSubsumsjonStore {
    fun insertSubsumsjonBruktV2(subsumsjonBruktV2: SubsumsjonBruktV2): Int
    fun getSubsumsjonBruktV2(subsumsjonsId: String): SubsumsjonBruktV2?
    fun listSubsumsjonBruktV2(): List<SubsumsjonBruktV2>
    fun subsumsjonBruktFraBehandlingsId(behandlingsId: String): List<SubsumsjonBruktV2>
    fun v1TilV2(v1: SubsumsjonBrukt): SubsumsjonBruktV2
    fun migrerV1TilV2()
}

data class SubsumsjonBrukt(
    val id: String,
    val eksternId: Long,
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

data class SubsumsjonBruktV2(val id: String, val behandlingsId: String, val arenaTs: ZonedDateTime, val created: ZonedDateTime? = null) {
    companion object Mapper {
        private val LOGGER = KotlinLogging.logger { }
        private val adapter = moshiInstance.adapter<SubsumsjonBruktV2>(SubsumsjonBruktV2::class.java)
        fun fromJson(json: String): SubsumsjonBruktV2? {
            return runCatching { adapter.fromJson(json) }.onFailure { e -> LOGGER.warn("Failed to convert string to object", e) }.getOrNull()
        }
    }

    fun toJson(): String {
        return adapter.toJson(this)
    }
}

val isoFormat = DateTimeFormatter.ISO_ZONED_DATE_TIME

internal class SubsumsjonBruktNotFoundException(override val message: String) : RuntimeException(message)
