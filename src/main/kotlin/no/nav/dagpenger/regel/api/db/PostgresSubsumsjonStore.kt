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

    override fun insertBehov(subsumsjonsBehov: SubsumsjonsBehov, regel: Regel) {
        val adapter = moshiInstance.adapter<SubsumsjonsBehov>(SubsumsjonsBehov::class.java)
        try {
            using(sessionOf(dataSource)) { session ->
                session.run(queryOf(""" INSERT INTO behov VALUES (?,?,?, (to_json(?::json))) """,
                    subsumsjonsBehov.behovId, regel.name, Status.Pending.toString(), adapter.toJson(subsumsjonsBehov)).asUpdate)
            }
        } catch (p: PSQLException) {
            throw StoreException(p.message!!)
        }
    }

    override fun behovStatus(behovId: String, regel: Regel): Status {
        val status = status(behovId, regel)
        return when (status) {
            "Pending" -> Status.Pending
            "Done" -> Status.Done(getSubsumsjonBy(behovId, regel))
            else -> throw StoreException("Illegal status for behovId $behovId and regel ${regel.name}. Status was $status")
        }
    }

    override fun insertSubsumsjon(subsumsjon: Subsumsjon) {
        val json = toJson(subsumsjon)
            ?: throw SubsumsjonSerDerException("Unable to serialize $subsumsjon")
        try {
            using(sessionOf(dataSource)) { session ->
                session.transaction { tx ->
                    tx.run(queryOf(""" INSERT INTO subsumsjon VALUES (? , ?, ?, (to_json(?::json))) ON CONFLICT ON CONSTRAINT subsumsjon_pkey DO NOTHING """,
                        subsumsjon.subsumsjonsId, subsumsjon.regel.name, subsumsjon.behovId, json).asUpdate)
                    tx.run(queryOf(""" UPDATE behov SET status = ? where id = ? AND regel = ?""",
                        Status.Done.toString(), subsumsjon.behovId, subsumsjon.regel.name).asUpdate)
                }
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

    private fun status(behovId: String, regel: Regel): String {
        try {
            return using(sessionOf(dataSource)) { session ->
                session.run(queryOf(""" SELECT status FROM behov WHERE id = ? AND regel = ?""", behovId, regel.name).map { row -> row.string("status") }
                    .asSingle)
                    ?: throw BehovNotFoundException("Could not find behov with PK $behovId:${regel.name}")
            }
        } catch (p: PSQLException) {
            throw StoreException(p.message!!)
        }
    }

    private fun getSubsumsjonBy(behovId: String, regel: Regel): String {
        return using(sessionOf(dataSource)) { session ->
            session.run(queryOf(""" SELECT id FROM subsumsjon WHERE behovId = ? AND regel = ?  """, behovId, regel.name)
                .map { row -> row.string("id") }.asSingle)
        } ?: throw SubsumsjonNotFoundException("Could not find subsumsjon for behovId $behovId and regel ${regel.name}")
    }
}
