package no.nav.dagpenger.regel.api.db

import no.nav.dagpenger.regel.api.models.BehandlingsId
import no.nav.dagpenger.regel.api.models.Behov
import no.nav.dagpenger.regel.api.models.BehovId
import no.nav.dagpenger.regel.api.models.InternBehov
import no.nav.dagpenger.regel.api.models.Kontekst
import no.nav.dagpenger.regel.api.models.RegelKontekst
import no.nav.dagpenger.regel.api.models.Status
import no.nav.dagpenger.regel.api.models.Subsumsjon
import no.nav.dagpenger.regel.api.models.SubsumsjonId
import java.time.ZoneId
import java.time.ZonedDateTime

interface SubsumsjonStore {

    fun opprettBehov(behov: Behov): InternBehov {
        val regelkontekst = behov.regelkontekst ?: RegelKontekst(behov.vedtakId.toString(), Kontekst.VEDTAK)
        val behandlingsId = hentKoblingTilRegelKontekst(regelkontekst) ?: opprettKoblingTilRegelkontekst(regelkontekst)
        val internBehov = InternBehov.fromBehov(
            behov = behov,
            behandlingsId = behandlingsId
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
    fun getSubsumsjonerByResults(subsumsjonIder: List<SubsumsjonId>): List<Subsumsjon>
    fun delete(subsumsjon: Subsumsjon)
    fun markerSomBrukt(internSubsumsjonBrukt: InternSubsumsjonBrukt)
}

internal class SubsumsjonNotFoundException(override val message: String) : RuntimeException(message)

internal class BehovNotFoundException(override val message: String) : RuntimeException(message)

internal class StoreException(override val message: String) : RuntimeException(message)
