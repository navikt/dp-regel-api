package no.nav.dagpenger.regel.api.db

import no.nav.dagpenger.regel.api.models.*
import java.time.ZoneId
import java.time.ZonedDateTime

interface SubsumsjonStore {

    fun opprettBehov(behov: Behov, eksternId: EksternId = EksternId(behov.vedtakId.toString(), Kontekst.VEDTAK)): InternBehov {
        val behandlingsId = hentKoblingTilEkstern(eksternId) ?: opprettKoblingTilEkstern(eksternId)
        val internBehov = InternBehov.fromBehov(behov, behandlingsId)
        insertBehov(internBehov)
        return internBehov
    }

    fun insertBehov(behov: InternBehov): Int
    fun hentKoblingTilEkstern(eksternId: EksternId): BehandlingsId?
    fun opprettKoblingTilEkstern(eksternId: EksternId): BehandlingsId
    fun getBehov(behovId: BehovId): InternBehov
    fun behovStatus(behovId: BehovId): Status
    fun insertSubsumsjon(subsumsjon: Subsumsjon, created: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"))): Int
    fun getSubsumsjon(behovId: BehovId): Subsumsjon
    fun getSubsumsjonByResult(subsumsjonId: SubsumsjonId): Subsumsjon
    fun delete(subsumsjon: Subsumsjon)
}

internal class SubsumsjonNotFoundException(override val message: String) : RuntimeException(message)

internal class BehovNotFoundException(override val message: String) : RuntimeException(message)

internal class StoreException(override val message: String) : RuntimeException(message)
