package no.nav.dagpenger.regel.api.db

internal interface BruktSubsumsjonStore {
    fun insertSubsumsjonBrukt(subsumsjonBrukt: SubsumsjonBrukt): Int
    fun getSubsumsjonBrukt(subsumsjonsId: String): SubsumsjonBrukt?
    fun subsumsjonBruktFraVedtak(vedtakId: String): SubsumsjonBrukt?
}

data class SubsumsjonBrukt(
    val id: String,
    val eksternId: String,
    val arenaTs: String,
    val ts: Long
)

internal class SubsumsjonBruktNotFoundException(override val message: String) : RuntimeException(message)
