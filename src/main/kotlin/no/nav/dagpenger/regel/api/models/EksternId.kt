package no.nav.dagpenger.regel.api.models

data class EksternId(val id: String, val kontekst: Kontekst)

enum class Kontekst {
    VEDTAK, ESTIMASJON
}
