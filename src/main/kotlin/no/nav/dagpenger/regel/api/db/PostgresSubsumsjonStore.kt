package no.nav.dagpenger.regel.api.db

import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.regel.api.Regel
import no.nav.dagpenger.regel.api.Status
import no.nav.dagpenger.regel.api.SubsumsjonsBehov
import no.nav.dagpenger.regel.api.models.Subsumsjon
import no.nav.dagpenger.regel.api.models.SubsumsjonSerDerException
import no.nav.dagpenger.regel.api.models.fromJson
import no.nav.dagpenger.regel.api.models.toJson
import no.nav.dagpenger.regel.api.moshiInstance
import org.postgresql.util.PSQLException

class PostgresSubsumsjonStore(private val dataSource: HikariDataSource) : SubsumsjonStore {

    override fun insertBehov(subsumsjonsBehov: SubsumsjonsBehov, regel: Regel): Int {
        val adapter = moshiInstance.adapter<SubsumsjonsBehov>(SubsumsjonsBehov::class.java)
        return try {
            using(sessionOf(dataSource)) { session ->
                session.run(queryOf(""" INSERT INTO behov VALUES (?,?, (to_json(?::json))) """,
                    subsumsjonsBehov.behovId, regel.name, adapter.toJson(subsumsjonsBehov)).asUpdate)
            }
        } catch (p: PSQLException) {
            throw StoreException(p.message!!)
        }
    }

    override fun behovStatus(behovId: String, regel: Regel): Status =
        getSubsumsjonIdBy(behovId, regel)?.let { Status.Done(it) } ?: Status.Pending

    override fun insertSubsumsjon(subsumsjon: Subsumsjon): Int {
        val json = toJson(subsumsjon)
            ?: throw SubsumsjonSerDerException("Unable to serialize $subsumsjon")
        return try {
            using(sessionOf(dataSource)) { session ->
                session.run(queryOf(""" INSERT INTO subsumsjon VALUES (? , ?, ?, (to_json(?::json))) ON CONFLICT ON CONSTRAINT subsumsjon_pkey DO NOTHING """,
                    subsumsjon.subsumsjonsId, subsumsjon.regel.name, subsumsjon.behovId, json).asUpdate)
            }
        } catch (p: PSQLException) {
            throw StoreException(p.message!!)
        }
    }

    override fun getSubsumsjon(subsumsjonId: String, regel: Regel): Subsumsjon {
        val json = using(sessionOf(dataSource)) { session ->
            session.run(queryOf(""" SELECT data FROM subsumsjon WHERE id = ? AND regel = ? """, subsumsjonId, regel.name)
                .map { row -> row.string("data") }
                .asSingle)
        } ?: throw SubsumsjonNotFoundException("Could not find subsumsjon with id $subsumsjonId")

        return fromJson(json, regel)
            ?: throw SubsumsjonSerDerException("Unable to deserialization json string $json")
    }

    private fun getSubsumsjonIdBy(behovId: String, regel: Regel): String? {
        try {
            return using(sessionOf(dataSource)) { session ->
                session.run(queryOf(""" SELECT id FROM subsumsjon WHERE behovId = ? AND regel = ?""", behovId, regel.name).map { row -> row.stringOrNull("id") }.asSingle)
            }
        } catch (p: PSQLException) {
            throw StoreException(p.message!!)
        }
    }
}
