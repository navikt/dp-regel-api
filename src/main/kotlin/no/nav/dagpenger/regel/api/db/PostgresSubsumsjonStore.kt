package no.nav.dagpenger.regel.api.db

import kotliquery.queryOf
import kotliquery.sessionOf
import javax.sql.DataSource


class PostgresSubsumsjonStore(private val dataSource: DataSource) : SubsumsjonStore {
    override fun insert(subsumsjonsId: String, json: String) {
        sessionOf(dataSource).run(queryOf(
                """
            INSERT INTO subsumsjon VALUES (?, (to_json(?::json)))
            ON CONFLICT(ulid) DO UPDATE SET data = (to_json(?::json))
            """
                        .trimIndent(), subsumsjonsId, json, json).asUpdate)
    }

    override fun get(subsumsjonsId: String): String {
        return sessionOf(dataSource).run(queryOf(""" SELECT data FROM subsumsjon WHERE ulid = ?  """, subsumsjonsId)
                .map { row -> row.stringOrNull("data") }.asSingle)
                .takeIf { !it.isNullOrEmpty() }
                ?: throw SubsumsjonNotFoundException("Could not find subsumsjon with id $subsumsjonsId")
    }
}
