package no.nav.dagpenger.regel.api.db

import com.zaxxer.hikari.HikariDataSource
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class PostgresTest {

    @Test
    fun `Migration scripts are applied successfully`() {
        withCleanDb {
            val migrations = migrate(DataSource.instance)
            assertEquals(16, migrations, "Wrong number of migrations")
        }
    }

    @Test
    fun `Migration scripts are idempotent`() {
        withCleanDb {
            migrate(DataSource.instance)

            val migrations = migrate(DataSource.instance)
            assertEquals(0, migrations, "Wrong number of migrations")
        }
    }

    @Test
    fun `JDBC url is set correctly from  config values `() {
        with(hikariConfigFrom(Configuration())) {
            assertEquals("jdbc:postgresql://localhost:5432/dp-regel-api", jdbcUrl)
        }
    }

    @Test
    fun `Store health check UP`() {
        withMigratedDb {
            with(PostgresSubsumsjonStore(DataSource.instance)) {
                status() shouldBe HealthStatus.UP
            }
        }
    }

    @Test
    fun `Store health check DOWN`() {
        with(
            PostgresSubsumsjonStore(
                HikariDataSource().apply {
                    username = PostgresContainer.instance.username
                    password = "BAD PASSWORD"
                    jdbcUrl = PostgresContainer.instance.jdbcUrl
                    connectionTimeout = 1000L
                }
            )
        ) {
            status() shouldBe HealthStatus.DOWN
        }
    }

    @Test
    fun `hits indices for fetching results by subsumsjonsId`() {
        val kjenteResultatNøkler =
            setOf("satsResultat", "minsteinntektResultat", "periodeResultat", "grunnlagResultat")

        withMigratedDb {
            using(sessionOf(DataSource.instance)) { session ->
                assertSoftly {
                    kjenteResultatNøkler.forEach {
                        session.run(
                            queryOf(
                                """EXPLAIN SELECT data FROM v2_subsumsjon
                                            WHERE data->'$it' ->> 'subsumsjonsId' = 'id' """,
                                emptyMap()
                            ).map { r ->
                                withClue("Seq scan for resultatnøkkel  '$it'") { r.string(1).shouldNotContain("Seq Scan") }
                            }.asSingle
                        )
                    }
                }
            }
        }
    }
}
