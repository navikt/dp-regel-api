package no.nav.dagpenger.regel.api.db

import kotliquery.queryOf
import kotliquery.sessionOf
import org.postgresql.util.PSQLException
import javax.sql.DataSource

class PostgresSubsumsjonStore(private val dataSource: DataSource) : SubsumsjonStore {

    override fun isHealthy() = sessionOf(dataSource).run(queryOf("SELECT 1").asExecute)

    override fun insert(subsumsjonsId: String, json: String) {
        try {
            sessionOf(dataSource).run(queryOf(""" INSERT INTO subsumsjon VALUES (?, (to_json(?::json))) """
                    .trimIndent(), subsumsjonsId, json).asUpdate)
        } catch (p: PSQLException) {
            throw StoreException(p.message!!)
        }
    }

    override fun get(subsumsjonsId: String): String {
        return sessionOf(dataSource).run(queryOf(""" SELECT data FROM subsumsjon WHERE ulid = ?  """, subsumsjonsId)
                .map { row -> row.stringOrNull("data") }.asSingle)
                .takeIf { !it.isNullOrEmpty() }
                ?: throw SubsumsjonNotFoundException("Could not find subsumsjon with id $subsumsjonsId")
    }
}
