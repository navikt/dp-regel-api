package no.nav.dagpenger.regel.api.db

import com.zaxxer.hikari.HikariDataSource
import io.prometheus.client.Counter
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.models.EksternId
import no.nav.dagpenger.regel.api.models.Kontekst
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import org.postgresql.util.PSQLException
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

private val LOGGER = KotlinLogging.logger {}

class PostgresBruktSubsumsjonStore(
    private val dataSource: HikariDataSource,
    val subsumsjonStore: SubsumsjonStore = PostgresSubsumsjonStore(dataSource)
) : BruktSubsumsjonStore, HealthCheck {
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

    override fun listSubsumsjonBruktV2(): List<SubsumsjonBruktV2> {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    """SELECT * FROM v2_subsumsjon_brukt""", emptyMap()
                ).map { r ->
                    extractSubsumsjonBruktV2(r)
                }.asList
            )
        }
    }

    fun insertSubsumsjonBrukt(subsumsjonBrukt: SubsumsjonBrukt): Int {
        return try {
            using(sessionOf(dataSource)) { session ->
                session.run(
                    queryOf(
                        """
                    INSERT INTO v1_subsumsjon_brukt(id, ekstern_id, kontekst, arena_ts) 
                        VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING
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

    fun getSubsumsjonBrukt(subsumsjonsId: String): SubsumsjonBrukt? {
        try {
            return using(sessionOf(dataSource)) { session ->
                session.run(
                    queryOf(
                        """SELECT * FROM v1_subsumsjon_brukt
                        WHERE id = ?""".trimMargin(), subsumsjonsId
                    ).map { row ->
                        extractSubsumsjonBrukt(row)
                    }.asSingle
                )
            }
        } catch (p: PSQLException) {
            throw StoreException(p.message ?: "")
        }
    }

    override fun v1TilV2(v1: SubsumsjonBrukt): SubsumsjonBruktV2 {
        val behandlingsId = subsumsjonStore.hentKoblingTilEkstern(EksternId(v1.eksternId.toString(), Kontekst.VEDTAK))
        return SubsumsjonBruktV2(
            id = v1.id,
            behandlingsId = behandlingsId.id,
            arenaTs = v1.arenaTs,
            created = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(v1.ts), ZoneOffset.UTC
            )
        )
    }

    override fun insertSubsumsjonBruktV2(subsumsjonBruktV2: SubsumsjonBruktV2): Int {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                when (subsumsjonBruktV2.created) {
                    null -> queryOf(
                        """INSERT INTO v2_subsumsjon_brukt(id, behandlings_id, arena_ts) VALUES (:id, :behandling, :arena) ON CONFLICT DO NOTHING""",
                        mapOf(
                            "id" to subsumsjonBruktV2.id,
                            "behandling" to subsumsjonBruktV2.behandlingsId,
                            "arena" to subsumsjonBruktV2.arenaTs
                        )
                    ).asUpdate
                    else -> queryOf(
                        """INSERT INTO v2_subsumsjon_brukt(id, behandlings_id, arena_ts, created) VALUES (:id, :behandling, :arena, :created) ON CONFLICT DO NOTHING""",
                        mapOf(
                            "id" to subsumsjonBruktV2.id,
                            "behandling" to subsumsjonBruktV2.behandlingsId,
                            "arena" to subsumsjonBruktV2.arenaTs,
                            "created" to subsumsjonBruktV2.created
                        )
                    ).asUpdate
                }
            )
        }
    }

    override fun getSubsumsjonBruktV2(subsumsjonsId: String): SubsumsjonBruktV2? {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    """SELECT * FROM v2_subsumsjon_brukt WHERE id = :id""",
                    mapOf("id" to subsumsjonsId)
                ).map { r -> extractSubsumsjonBruktV2(r) }.asSingle
            )
        }
    }

    private fun extractSubsumsjonBruktV2(r: Row): SubsumsjonBruktV2 {
        return SubsumsjonBruktV2(
            id = r.string("id"),
            behandlingsId = r.string("behandlings_id"),
            arenaTs = r.zonedDateTime("arena_ts"),
            created = r.zonedDateTime("created")
        )
    }

    private fun extractSubsumsjonBrukt(row: Row): SubsumsjonBrukt {
        return SubsumsjonBrukt(
            id = row.string("id"),
            eksternId = row.string("ekstern_id").toLong(),
            arenaTs = row.zonedDateTime("arena_ts"),
            ts = row.instant("created").toEpochMilli()
        )
    }

    override fun migrerV1TilV2() {
        using(sessionOf(dataSource)) { session ->
            session.forEach(
                queryOf(
                    "SELECT * FROM v1_subsumsjon_brukt WHERE id NOT IN (SELECT id FROM v2_subsumsjon_brukt AS v2 WHERE v2.id = id)",
                    emptyMap()
                )
            ) { r ->
                val v2SubsumsjonBrukt = v1TilV2(
                    SubsumsjonBrukt(
                        id = r.string("id"),
                        eksternId = r.string("ekstern_id").toDouble().toLong(),
                        arenaTs = r.zonedDateTime("arena_ts"),
                        ts = r.zonedDateTime("created").toInstant().toEpochMilli()
                    )
                )
                insertSubsumsjonBruktV2(v2SubsumsjonBrukt)
            }
        }
    }

    override fun subsumsjonBruktFraBehandlingsId(behandlingsId: String): List<SubsumsjonBruktV2> {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    """SELECT * FROM v2_subsumsjon_brukt WHERE behandlings_id = :bid""",
                    mapOf("bid" to behandlingsId)
                ).map { r -> extractSubsumsjonBruktV2(r) }.asList
            )
        }
    }
}