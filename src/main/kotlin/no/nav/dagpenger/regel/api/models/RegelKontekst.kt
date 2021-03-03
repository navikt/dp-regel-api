package no.nav.dagpenger.regel.api.models

import com.squareup.moshi.Json

data class RegelKontekst(val id: String, val type: Kontekst)

enum class Kontekst {
    @Json(name = "soknad")
    SOKNAD,
    @Json(name = "veiledning")
    VEILEDNING,
    @Json(name = "vedtak")
    VEDTAK,
    @Json(name = "revurdering")
    REVURDERING,
    @Json(name = "forskudd")
    FORSKUDD,
    @Json(name = "corona")
    CORONA // Todo: fjern når forskudd er ute
}
