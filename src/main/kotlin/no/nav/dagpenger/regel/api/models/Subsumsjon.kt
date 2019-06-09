package no.nav.dagpenger.regel.api.models

import de.huxhorn.sulky.ulid.ULID
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.regel.api.models.Faktum.Mapper.faktumFrom
import no.nav.dagpenger.regel.api.moshiInstance

internal data class Subsumsjon(
    val id: String,
    val behovId: String,
    val faktum: Faktum,
    val grunnlagResultat: Map<String, Any>?,
    val minsteinntektResultat: Map<String, Any>?,
    val periodeResultat: Map<String, Any>?,
    val satsResultat: Map<String, Any>?,
    val problem: Map<String, Any>?

) {
    companion object Mapper {
        private val ulidGenerator = ULID()

        private val adapter = moshiInstance.adapter<Subsumsjon>(Subsumsjon::class.java)
        fun toJson(subsumsjon: Subsumsjon): String = adapter.toJson(subsumsjon)
        fun fromJson(json: String): Subsumsjon? = adapter.fromJson(json)

        fun subsumsjonFrom(packet: Packet): Subsumsjon =
            Subsumsjon(
                ulidGenerator.nextULID(),
                behovId = packet.getStringValue(PacketKeys.BEHOV_ID),
                faktum = faktumFrom(packet),
                minsteinntektResultat = minsteinntektResultatFrom(packet),
                grunnlagResultat = grunnlagResultatFrom(packet),
                periodeResultat = mapFrom(PacketKeys.PERIODE_RESULTAT, packet),
                satsResultat = mapFrom(PacketKeys.SATS_RESULTAT, packet),
                problem = mapFrom(PacketKeys.PROBLEM, packet)
            )
    }

    fun toJson(): String = toJson(this)
}

// todo Remove once "minsteinntektInntektsPerioder" is part of the result and return the map
internal fun minsteinntektResultatFrom(packet: Packet): Map<String, Any>? =
    mapFrom(PacketKeys.MINSTEINNTEKT_RESULTAT, packet)?.toMutableMap()?.apply {
        packet.getNullableObjectValue(PacketKeys.MINSTEINNTEKT_INNTEKTSPERIODER) { any -> any }?.let {
            put(PacketKeys.MINSTEINNTEKT_INNTEKTSPERIODER, it)
        }
    }?.toMap()

// todo Remove once "grunnlagInntektsPerioder" is part of the result and return the map
internal fun grunnlagResultatFrom(packet: Packet): Map<String, Any>? =
    mapFrom(PacketKeys.GRUNNLAG_RESULTAT, packet)?.toMutableMap()?.apply {
        packet.getNullableObjectValue(PacketKeys.GRUNNLAG_INNTEKTSPERIODER) { any -> any }?.let {
            put(PacketKeys.GRUNNLAG_INNTEKTSPERIODER, it)
        }
    }?.toMap()

internal fun mapFrom(packetKey: String, packet: Packet): Map<String, Any>? =
    packet.hasField(packetKey).takeIf { it }?.let { packet.getMapValue(packetKey) }

internal class SubsumsjonSerDerException(message: String) : RuntimeException(message)
