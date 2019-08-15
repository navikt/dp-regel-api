package no.nav.dagpenger.regel.api.db

import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import no.nav.dagpenger.regel.api.models.Behov
import no.nav.dagpenger.regel.api.models.Status
import no.nav.dagpenger.regel.api.models.Subsumsjon
import no.nav.dagpenger.regel.api.models.SubsumsjonSerDerException
import org.postgresql.util.PSQLException

private val LOGGER = KotlinLogging.logger {}

internal class PostgresSubsumsjonStore(private val dataSource: HikariDataSource) : SubsumsjonStore, HealthCheck {

    override fun insertBehov(behov: Behov): Int {
        return try {
            using(sessionOf(dataSource)) { session ->
                session.run(queryOf(""" INSERT INTO v1_behov VALUES (?, (to_json(?::json))) """,
                        behov.behovId, Behov.toJson(behov)).asUpdate)
            }
        } catch (p: PSQLException) {
            throw StoreException(p.message!!)
        }
    }

    override fun behovStatus(id: String): Status {
        return when (behovExists(id)) {
            true -> getSubsumsjonIdBy(id)?.let { Status.Done(it) } ?: Status.Pending
            false -> throw BehovNotFoundException("BehovId: $id")
        }
    }

    override fun insertSubsumsjon(subsumsjon: Subsumsjon): Int {
        return try {
            using(sessionOf(dataSource)) { session ->
                session.run(queryOf(""" INSERT INTO v1_subsumsjon VALUES (?,?, (to_json(?::json))) ON CONFLICT ON CONSTRAINT v1_subsumsjon_pkey DO NOTHING """,
                        subsumsjon.id, subsumsjon.behovId, subsumsjon.toJson()).asUpdate)
            }
        } catch (p: PSQLException) {
            throw StoreException(p.message!!)
        }
    }

    override fun getSubsumsjon(id: String): Subsumsjon {
        val json = using(sessionOf(dataSource)) { session ->
            session.run(queryOf(""" SELECT data FROM v1_subsumsjon WHERE id = ? """, id)
                    .map { row -> row.string("data") }
                    .asSingle)
        } ?: throw SubsumsjonNotFoundException("Could not find subsumsjon with id $id")

        return Subsumsjon.fromJson(json) ?: throw SubsumsjonSerDerException("Unable to deserialize: $json")
    }

    override fun status(): HealthStatus {
        return try {
            using(sessionOf(dataSource)) { session -> session.run(queryOf(""" SELECT 1""").asExecute) }.let { HealthStatus.UP }
        } catch (p: PSQLException) {
            LOGGER.error("Failed health check", p)
            return HealthStatus.DOWN
        }
    }

    override fun getSubsumsjonByResult(subsumsjonId: SubsumsjonId): Subsumsjon {
        val json = using(sessionOf(dataSource)) { session ->
            session.run(queryOf(""" select
                                                  data
                                            from v1_subsumsjon
                                            where data -> 'satsResultat' ->> 'subsumsjonsId'::text = :id
                                               OR data -> 'minsteinntektResultat' ->> 'subsumsjonsId'::text = :id
                                               OR data -> 'periodeResultat' ->> 'subsumsjonsId'::text = :id
                                               OR data -> 'grunnlagResultat' ->> 'subsumsjonsId'::text = :id """,
                mapOf("id" to subsumsjonId.id))
                .map { row -> row.string("data") }.asSingle)
        } ?: throw SubsumsjonNotFoundException("Could not find subsumsjon with subsumsjonId $subsumsjonId")

        return Subsumsjon.fromJson(json) ?: throw SubsumsjonSerDerException("Unable to deserialize: $json")
    }

    private fun behovExists(behovId: String): Boolean {
        try {
            return using(sessionOf(dataSource)) { session ->
                session.run(queryOf(""" SELECT EXISTS (SELECT 1 FROM v1_behov WHERE id = ? ) AS "exists" """, behovId).map { row -> row.boolean("exists") }.asSingle)!!
            }
        } catch (p: PSQLException) {
            throw StoreException(p.message!!)
        }
    }

    private fun getSubsumsjonIdBy(behovId: String): String? {
        try {
            return using(sessionOf(dataSource)) { session ->
                session.run(queryOf(""" SELECT id FROM v1_subsumsjon WHERE behov_id = ? """, behovId).map { row -> row.stringOrNull("id") }.asSingle)
            }
        } catch (p: PSQLException) {
            throw StoreException(p.message!!)
        }
    }
}
