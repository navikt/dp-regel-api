package no.nav.dagpenger.regel.api.v1.models

import de.huxhorn.sulky.ulid.ULID
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.regel.api.moshiInstance

private val ulidGenerator = ULID()

internal fun subsumsjonFrom(packet: Packet): Subsumsjon =
    Subsumsjon(
        ulidGenerator.nextULID(),
        behovId = packet.getStringValue(PacketKeys.BEHOV_ID),
        faktum = faktumFrom(packet),
        minsteinntektResultat = minsteinntektResultatFrom(packet),
        grunnlagResultat = grunnlagResultatFrom(packet),
        periodeResultat = mapFrom(PacketKeys.PERIODE_RESULTAT, packet),
        satsResultat = mapFrom(PacketKeys.SATS_RESULTAT, packet)
    )

internal fun faktumFrom(packet: Packet): Faktum =
    packet.getObjectValue(PacketKeys.FAKTUM) { string ->
        val faktum = (moshiInstance.adapter<Faktum>(Faktum::class.java).fromJsonValue(string)
            ?: throw SerDerException("Unable to map: $string to Faktum"))
        faktum.copy(inntektsId = inntektsIdFrom(packet))
    }

private fun inntektsIdFrom(packet: Packet): String? = packet.getNullableObjectValue(PacketKeys.INNTEKT) { json ->
    moshiInstance.adapter<Inntekt>(Inntekt::class.java).fromJsonValue(json)?.inntektsId
}

//todo Remove once "minsteinntektInntektsPerioder" is part of the result and return the map
internal fun minsteinntektResultatFrom(packet: Packet): Map<String, Any>? =
    mapFrom(PacketKeys.MINSTEINNTEKT_RESULTAT, packet)?.toMutableMap()?.apply {
        put(PacketKeys.MINSTEINNTEKT_INNTEKTSPERIODER, packet.getMapValue(PacketKeys.MINSTEINNTEKT_INNTEKTSPERIODER))
    }?.toMap()

//todo Remove once "grunnlagInntektsPerioder" is part of the result and return the map
internal fun grunnlagResultatFrom(packet: Packet): Map<String, Any>? =
    mapFrom(PacketKeys.GRUNNLAG_RESULTAT, packet)?.toMutableMap()?.apply {
        put(PacketKeys.GRUNNLAG_INNTEKTSPERIODER, packet.getMapValue(PacketKeys.GRUNNLAG_INNTEKTSPERIODER))
    }?.toMap()

internal fun mapFrom(packetKey: String, packet: Packet): Map<String, Any>? =
    packet.hasField(packetKey).takeIf { it }.let { packet.getMapValue(packetKey) }


internal class SerDerException(message: String) : RuntimeException(message)