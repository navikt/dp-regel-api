package no.nav.dagpenger.regel.api.db

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.models.SubsumsjonId
import no.nav.dagpenger.regel.api.moshiInstance

interface BruktSubsumsjonStore {
    fun insertSubsumsjonBrukt(internSubsumsjonBrukt: InternSubsumsjonBrukt): Int
    fun getSubsumsjonBrukt(subsumsjonId: SubsumsjonId): InternSubsumsjonBrukt?
    fun listSubsumsjonBrukt(): List<InternSubsumsjonBrukt>
    fun subsumsjonBruktFraBehandlingsId(behandlingsId: String): List<InternSubsumsjonBrukt>
    fun eksternTilInternSubsumsjon(eksternSubsumsjonBrukt: EksternSubsumsjonBrukt): InternSubsumsjonBrukt
}

data class EksternSubsumsjonBrukt(
    val id: String,
    val eksternId: Long,
    val arenaTs: ZonedDateTime,
    val ts: Long,
    val utfall: String? = null,
    val vedtakStatus: String? = null
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
