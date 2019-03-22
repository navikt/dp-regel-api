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

    override fun insertBehov(subsumsjonsBehov: SubsumsjonsBehov) {
        val adapter = moshiInstance.adapter<SubsumsjonsBehov>(SubsumsjonsBehov::class.java)
        try {
            using(sessionOf(dataSource)) { session ->
                session.transaction { tx ->
                    tx.run(queryOf(""" INSERT INTO behov VALUES (?,?, (to_json(?::json))) """, subsumsjonsBehov.behovId, Status.Pending.toString(), adapter.toJson(subsumsjonsBehov)).asUpdate)
                    tx.run(queryOf(""" INSERT INTO arena_vedtak_behov_mapping(behovId, vedtakId)  VALUES (?,?) """, subsumsjonsBehov.behovId, subsumsjonsBehov.vedtakId).asUpdate)
                }
            }
        } catch (p: PSQLException) {
            throw StoreException(p.message!!)
        }
    }

    override fun behovStatus(behovId: String): Status {
        val status = status(behovId)
        return when (status) {
            "Pending" -> Status.Pending
            "Done" -> Status.Done(getSubsumsjonIdBy(behovId))
            else -> throw StoreException("Illegal status for behovId $behovId. Status was $status")
        }
    }

    override fun insertSubsumsjon(subsumsjon: Subsumsjon) {
        val json = toJson(subsumsjon)
            ?: throw SubsumsjonSerDerException("Unable to serialize $subsumsjon")
        try {
            using(sessionOf(dataSource)) { session ->
                session.transaction { tx ->
                    tx.run(queryOf(""" INSERT INTO subsumsjon VALUES (? , ?, ?, (to_json(?::json))) """, subsumsjon.subsumsjonsId, subsumsjon.regel.name, subsumsjon.behovId, json).asUpdate)
                    tx.run(queryOf(""" UPDATE behov SET status = ? where id = ?""", Status.Done.toString(), subsumsjon.behovId).asUpdate)
                }
            }
        } catch (p: PSQLException) {
            throw StoreException(p.message!!)
        }
    }

    override fun getSubsumsjon(subsumsjonId: String): Subsumsjon {
        val (regel, json) = using(sessionOf(dataSource)) { session ->
            session.run(queryOf(""" SELECT data FROM subsumsjon WHERE id = ? """, subsumsjonId)
                .map { row -> Pair(Regel.valueOf(row.string("regel")), row.string("data")) }
                .asSingle)
        } ?: throw SubsumsjonNotFoundException("Could not find subsumsjon with id $subsumsjonId")

        return fromJson(json, regel)
            ?: throw SubsumsjonSerDerException("Unable to deserialization json string $json")
    }

    private fun status(behovId: String): String {
        try {
            return using(sessionOf(dataSource)) { session ->
                session.run(queryOf(""" SELECT status from behov where id = ?""", behovId).map { row -> row.string("status") }
                    .asSingle)
                    ?: throw BehovNotFoundException("Behov $behovId not found.")
            }
        } catch (p: PSQLException) {
            throw StoreException(p.message!!)
        }
    }

    private fun getSubsumsjonIdBy(behovId: String): String {
        return using(sessionOf(dataSource)) { session ->
            session.run(queryOf(""" SELECT id FROM subsumsjon WHERE behovId = ? """, behovId)
                .map { row -> row.string("id") }.asSingle)
        } ?: throw SubsumsjonNotFoundException("Could not find subsumsjon for behovId $behovId")
    }
}
