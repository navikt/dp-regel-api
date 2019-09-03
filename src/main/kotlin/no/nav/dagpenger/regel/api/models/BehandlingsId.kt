package no.nav.dagpenger.regel.api.models

import de.huxhorn.sulky.ulid.ULID

private val ulid = ULID()

data class BehandlingsId(val id: String, val eksternId: EksternId) {

    companion object {
        fun nyBehandlingsIdFraEksternId(eksternId: EksternId): BehandlingsId {
            return BehandlingsId(ulid.nextULID(), eksternId)
        }
    }
}
