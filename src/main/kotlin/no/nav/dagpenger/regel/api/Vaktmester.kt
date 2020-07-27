package no.nav.dagpenger.regel.api

import io.prometheus.client.Counter
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.db.BruktSubsumsjonStore
import no.nav.dagpenger.regel.api.db.InternSubsumsjonBrukt
import no.nav.dagpenger.regel.api.db.PostgresBruktSubsumsjonStore
import no.nav.dagpenger.regel.api.db.PostgresSubsumsjonStore
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.Subsumsjon

private val deletedCounter = Counter.build()
    .name("subsumsjoner_slettet")
    .help("Antall subsumsjoner slettet fra databasen")
    .register()

class Vaktmester(
    val dataSource: DataSource,
    val subsumsjonStore: SubsumsjonStore = PostgresSubsumsjonStore(dataSource = dataSource),
    val bruktSubsumsjonStore: BruktSubsumsjonStore = PostgresBruktSubsumsjonStore(dataSource = dataSource),
    private val lifeSpanInDays: Int = 180
) {
    companion object {
        val LOGGER = KotlinLogging.logger { }
    }

    fun rydd() {
        using(sessionOf(dataSource)) { session ->
            val subsumsjonerSomSkalSlettes = session.run(queryOf(
                """SELECT data FROM v2_subsumsjon WHERE brukt = false AND created < (now() - (make_interval(days := :days)))""",
                mapOf("days" to lifeSpanInDays)
            ).map { Subsumsjon.fromJson(it.string("data")) }.asList)
            subsumsjonerSomSkalSlettes.forEach {
                subsumsjonStore.delete(it)
                deletedCounter.inc()
            }
        }
    }

    suspend fun markerBrukteSubsumsjoner() {
        var run = 0
        bruktSubsumsjonStore.listSubsumsjonBrukt().forEach {
            run++
            markerSomBrukt(it)
            if (run % 100 == 0) {
                LOGGER.info("Processed $run rows")
            }
        }
    }

    fun markerSomBrukt(internSubsumsjonBrukt: InternSubsumsjonBrukt) {
        subsumsjonStore.markerSomBrukt(internSubsumsjonBrukt)
    }
}
