package no.nav.dagpenger.regel.api.db

import com.zaxxer.hikari.HikariDataSource
import org.testcontainers.containers.PostgreSQLContainer

internal object PostgresContainer {
    val instance by lazy {
        PostgreSQLContainer<Nothing>("postgres:11.2").apply {
            start()
        }
    }
}

internal object DataSource {
    val instance: HikariDataSource by lazy {
        HikariDataSource().apply {
            username = PostgresContainer.instance.username
            password = PostgresContainer.instance.password
            jdbcUrl = PostgresContainer.instance.jdbcUrl
            connectionTimeout = 1000L
        }
    }
}

internal fun withCleanDb(test: () -> Unit) = DataSource.instance.also { clean(it) }.run { test() }

internal fun withMigratedDb(test: () -> Unit) =
    DataSource.instance.also { clean(it) }.also { migrate(it) }.run { test() }
