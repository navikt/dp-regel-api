package no.nav.dagpenger.regel.api.models

import de.huxhorn.sulky.ulid.ULID

private val ulid = ULID()

data class BehandlingsId(val id: String, val regelKontekst: RegelKontekst) {
    companion object {
        fun nyBehandlingsIdFraEksternId(regelKontekst: RegelKontekst): BehandlingsId {
            return BehandlingsId(ulid.nextULID(), regelKontekst)
        }
    }
}
