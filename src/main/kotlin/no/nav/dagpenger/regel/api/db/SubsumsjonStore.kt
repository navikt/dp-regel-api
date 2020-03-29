package no.nav.dagpenger.regel.api.db

import no.finn.unleash.Unleash
import no.nav.dagpenger.regel.api.models.*
import java.time.ZoneId
import java.time.ZonedDateTime

interface SubsumsjonStore {

    fun opprettBehov(behov: Behov, unleash: Unleash): InternBehov {
        val regelkontekst = behov.regelkontekst ?: RegelKontekst(behov.vedtakId.toString(), Kontekst.VEDTAK)
        val behandlingsId = hentKoblingTilRegelKontekst(regelkontekst) ?: opprettKoblingTilRegelkontekst(regelkontekst)
        val internBehov = InternBehov.fromBehov(
            behov = behov,
            behandlingsId = behandlingsId,
            koronaToggle = unleash.isEnabled("dp.korona", true)
        )
        insertBehov(internBehov)
        return internBehov
    }

    fun insertBehov(behov: InternBehov): Int
    fun hentKoblingTilRegelKontekst(regelKontekst: RegelKontekst): BehandlingsId?
    fun opprettKoblingTilRegelkontekst(regelKontekst: RegelKontekst): BehandlingsId
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
