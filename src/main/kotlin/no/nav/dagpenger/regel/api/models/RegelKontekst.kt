package no.nav.dagpenger.regel.api.models

data class RegelKontekst(val id: String, val type: Kontekst)

@Suppress("ktlint:standard:enum-entry-name-case")
enum class Kontekst {
    soknad,
    veiledning,
    vedtak,
    revurdering,
    forskudd,
}
