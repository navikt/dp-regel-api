package no.nav.dagpenger.regel.api.db

import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.internal.configuration.ConfigUtils
import org.testcontainers.postgresql.PostgreSQLContainer
import javax.sql.DataSource

internal object PostgresTestSetup {
    val instance by lazy {
        PostgreSQLContainer("postgres:15.4").apply {
            start()
        }
    }

    fun withMigratedDb(block: () -> Unit) {
        withCleanDb {
            PostgresDataSourceBuilder.runMigration()
            block()
        }
    }

    fun withMigratedDb(block: (ds: DataSource) -> Unit) {
        withCleanDb {
            PostgresDataSourceBuilder.runMigration()
            block(PostgresDataSourceBuilder.dataSource)
        }
    }

    fun withMigratedDb(): HikariDataSource {
        setup()
        PostgresDataSourceBuilder.clean()
        PostgresDataSourceBuilder.runMigration()
        return PostgresDataSourceBuilder.dataSource
    }

    private fun setup() {
        System.setProperty(ConfigUtils.CLEAN_DISABLED, "false")
        System.setProperty(PostgresDataSourceBuilder.DB_URL_KEY, instance.jdbcUrl)
        System.setProperty(PostgresDataSourceBuilder.DB_PASSWORD_KEY, instance.password)
        System.setProperty(PostgresDataSourceBuilder.DB_USERNAME_KEY, instance.username)
    }

    private fun tearDown() {
        System.clearProperty(PostgresDataSourceBuilder.DB_PASSWORD_KEY)
        System.clearProperty(PostgresDataSourceBuilder.DB_USERNAME_KEY)
        System.clearProperty(PostgresDataSourceBuilder.DB_URL_KEY)
        System.clearProperty(ConfigUtils.CLEAN_DISABLED)
    }

    fun withCleanDb(block: () -> Unit) {
        setup()
        PostgresDataSourceBuilder.clean().run {
            block()
        }.also {
            tearDown()
        }
    }
}
