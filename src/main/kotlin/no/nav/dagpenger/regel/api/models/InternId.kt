package no.nav.dagpenger.regel.api.models

import de.huxhorn.sulky.ulid.ULID

private val ulid = ULID()

data class InternId(val id: String, val eksternId: EksternId) {

    companion object {
        fun nyInternIdFraEksternId(eksternId: EksternId): InternId {
            return InternId(ulid.nextULID(), eksternId)
        }
    }
}
