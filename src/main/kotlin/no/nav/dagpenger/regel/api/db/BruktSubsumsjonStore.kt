package no.nav.dagpenger.regel.api.db

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.regel.api.models.Subsumsjon
import no.nav.dagpenger.regel.api.models.SubsumsjonId
import no.nav.dagpenger.regel.api.serder.EksternIdDeserializer
import no.nav.dagpenger.regel.api.serder.jacksonObjectMapper
import tools.jackson.databind.annotation.JsonDeserialize
import java.time.ZonedDateTime

interface BruktSubsumsjonStore {
    fun insertSubsumsjonBrukt(internSubsumsjonBrukt: InternSubsumsjonBrukt): Int

    fun getSubsumsjonBrukt(subsumsjonId: SubsumsjonId): InternSubsumsjonBrukt?

    fun listSubsumsjonBrukt(): List<InternSubsumsjonBrukt>

    fun subsumsjonBruktFraBehandlingsId(behandlingsId: String): List<InternSubsumsjonBrukt>

    fun eksternTilInternSubsumsjon(eksternSubsumsjonBrukt: EksternSubsumsjonBrukt): InternSubsumsjonBrukt

    fun getSubsumsjonByResult(subsumsjonId: SubsumsjonId): Subsumsjon
}

data class EksternSubsumsjonBrukt(
    val id: String,
    @field:JsonDeserialize(using = EksternIdDeserializer::class)
    val eksternId: Long,
    val arenaTs: ZonedDateTime,
    val ts: Long,
    val utfall: String? = null,
    val vedtakStatus: String? = null,
) {
    companion object Mapper {
        private val LOGGER = KotlinLogging.logger { }

        fun fromJson(json: String): EksternSubsumsjonBrukt {
            return runCatching<EksternSubsumsjonBrukt> {
                jacksonObjectMapper.readValue(json, EksternSubsumsjonBrukt::class.java)
            }.onFailure { e -> LOGGER.warn(e) { "Failed to convert string to object" } }.getOrThrow()
        }
    }

    fun toJson(): String {
        return jacksonObjectMapper.writeValueAsString(this)
    }
}

data class InternSubsumsjonBrukt(
    val id: String,
    val behandlingsId: String,
    val arenaTs: ZonedDateTime,
    val created: ZonedDateTime? = null,
) {
    companion object Mapper {
        private val LOGGER = KotlinLogging.logger { }

        fun fromJson(json: String): InternSubsumsjonBrukt? {
            return runCatching<InternSubsumsjonBrukt> {
                jacksonObjectMapper.readValue(
                    json,
                    InternSubsumsjonBrukt::class.java,
                )
            }.onFailure { e -> LOGGER.warn(e) { "Failed to convert string to object" } }.getOrNull()
        }
    }

    fun toJson(): String {
        return jacksonObjectMapper.writeValueAsString(this)
    }
}

internal class SubsumsjonBruktNotFoundException(override val message: String) : RuntimeException(message)
