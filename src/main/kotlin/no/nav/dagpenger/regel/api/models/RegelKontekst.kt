package no.nav.dagpenger.regel.api.models

data class RegelKontekst(val id: String, val type: Kontekst)

enum class Kontekst {
    soknad,
    veiledning,
    vedtak,
    revurdering,
    forskudd,
}
