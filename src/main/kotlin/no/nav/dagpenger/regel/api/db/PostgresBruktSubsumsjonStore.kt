package no.nav.dagpenger.regel.api.db

import io.prometheus.client.Counter
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.models.EksternId
import no.nav.dagpenger.regel.api.models.Kontekst
import no.nav.dagpenger.regel.api.models.SubsumsjonId
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import org.postgresql.util.PSQLException
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import javax.sql.DataSource

private val LOGGER = KotlinLogging.logger {}

class PostgresBruktSubsumsjonStore(
    private val dataSource: DataSource,
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

    override fun listSubsumsjonBrukt(): List<InternSubsumsjonBrukt> {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    """SELECT * FROM v2_subsumsjon_brukt""", emptyMap()
                ).map { r ->
                    extractInternSubsumsjonBrukt(r)
                }.asList
            )
        }
    }

    override fun eksternTilInternSubsumsjon(v1: EksternSubsumsjonBrukt): InternSubsumsjonBrukt {
        val behandlingsId = subsumsjonStore.hentKoblingTilEkstern(EksternId(v1.eksternId.toString(), Kontekst.VEDTAK))
        return InternSubsumsjonBrukt(
            id = v1.id,
            behandlingsId = behandlingsId.id,
            arenaTs = v1.arenaTs,
            created = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(v1.ts), ZoneOffset.UTC
            )
        )
    }

    override fun insertSubsumsjonBrukt(internSubsumsjonBrukt: InternSubsumsjonBrukt): Int {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                when (internSubsumsjonBrukt.created) {
                    null -> queryOf(
                        """INSERT INTO v2_subsumsjon_brukt(id, behandlings_id, arena_ts) VALUES (:id, :behandling, :arena) ON CONFLICT DO NOTHING""",
                        mapOf(
                            "id" to internSubsumsjonBrukt.id,
                            "behandling" to internSubsumsjonBrukt.behandlingsId,
                            "arena" to internSubsumsjonBrukt.arenaTs
                        )
                    ).asUpdate
                    else -> queryOf(
                        """INSERT INTO v2_subsumsjon_brukt(id, behandlings_id, arena_ts, created) VALUES (:id, :behandling, :arena, :created) ON CONFLICT DO NOTHING""",
                        mapOf(
                            "id" to internSubsumsjonBrukt.id,
                            "behandling" to internSubsumsjonBrukt.behandlingsId,
                            "arena" to internSubsumsjonBrukt.arenaTs,
                            "created" to internSubsumsjonBrukt.created
                        )
                    ).asUpdate
                }
            )
        }
    }

    override fun getSubsumsjonBrukt(subsumsjonId: SubsumsjonId): InternSubsumsjonBrukt? {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    """SELECT * FROM v2_subsumsjon_brukt WHERE id = :id""",
                    mapOf("id" to subsumsjonId.id)
                ).map { r -> extractInternSubsumsjonBrukt(r) }.asSingle
            )
        }
    }

    private fun extractInternSubsumsjonBrukt(r: Row): InternSubsumsjonBrukt {
        return InternSubsumsjonBrukt(
            id = r.string("id"),
            behandlingsId = r.string("behandlings_id"),
            arenaTs = r.zonedDateTime("arena_ts"),
            created = r.zonedDateTime("created")
        )
    }

    override fun subsumsjonBruktFraBehandlingsId(behandlingsId: String): List<InternSubsumsjonBrukt> {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    """SELECT * FROM v2_subsumsjon_brukt WHERE behandlings_id = :bid""",
                    mapOf("bid" to behandlingsId)
                ).map { r -> extractInternSubsumsjonBrukt(r) }.asList
            )
        }
    }
}