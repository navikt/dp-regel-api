package no.nav.dagpenger.regel.api.db

import com.zaxxer.hikari.HikariDataSource
import io.prometheus.client.Counter
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import org.postgresql.util.PSQLException

private val LOGGER = KotlinLogging.logger {}

class PostgresBruktSubsumsjonStore(private val dataSource: HikariDataSource) : BruktSubsumsjonStore, HealthCheck {
    companion object {
        val insertCounter = Counter.build().name("subsumsjon_brukt_insert")
            .namespace("no_nav_dagpenger")
            .help("Hvor mange subsumsjoner fra vedtak lytter").register()
    }
    override fun status(): HealthStatus {
        return try {
            using(sessionOf(dataSource)) { session ->
                session.run(queryOf("SELECT 1").asExecute)
            }.let { HealthStatus.UP }
        } catch (p: PSQLException) {
            LOGGER.error("Failed health check", p)
            return HealthStatus.DOWN
        }
    }

    override fun insertSubsumsjonBrukt(subsumsjonBrukt: SubsumsjonBrukt): Int {
        return try {
            using(sessionOf(dataSource)) { session ->
                session.run(
                    queryOf(
                        """
                    INSERT INTO v1_subsumsjon_brukt(id, ekstern_id, kontekst, arena_ts) 
                        VALUES (?, ?, ?, ?)
                """.trimIndent(),
                        subsumsjonBrukt.id,
                        subsumsjonBrukt.eksternId,
                        "Vedtak",
                        subsumsjonBrukt.arenaTs
                    ).asUpdate
                ).also {
                    insertCounter.inc()
                }
            }
        } catch (p: PSQLException) {
            throw StoreException(p.message ?: "")
        }
    }

    override fun getSubsumsjonBrukt(subsumsjonsId: String): SubsumsjonBrukt? {
        try {
            return using(sessionOf(dataSource)) { session ->
                session.run(
                    queryOf(
                        """SELECT * FROM v1_subsumsjon_brukt
                        WHERE id = ?""".trimMargin(), subsumsjonsId
                    ).map { row ->
                        SubsumsjonBrukt(
                            id = row.string("id"),
                            eksternId = row.string("ekstern_id"),
                            arenaTs = row.zonedDateTime("arena_ts"),
                            ts = row.instant("created").toEpochMilli()
                        )
                    }.asSingle
                )
            }
        } catch (p: PSQLException) {
            throw StoreException(p.message ?: "")
        }
    }
    override fun subsumsjonBruktFraVedtak(vedtakId: String): SubsumsjonBrukt? {
        try {
            return using(sessionOf(dataSource)) { session ->
                session.run(
                    queryOf(
                        """SELECT * FROM v1_subsumsjon_brukt" +
                        WHERE ekstern_id = ?""".trimMargin(), vedtakId
                    ).map { row ->
                        SubsumsjonBrukt(
                            id = row.string("id"),
                            eksternId = row.string("ekstern_id"),
                            arenaTs = row.zonedDateTime("arena_ts"),
                            ts = row.long("created")
                        )
                    }.asSingle
                )
            }
        } catch (p: PSQLException) {
            throw StoreException(p.message ?: "")
        }
    }
}