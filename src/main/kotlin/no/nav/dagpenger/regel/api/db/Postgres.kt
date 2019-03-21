package no.nav.dagpenger.regel.api.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.dagpenger.regel.api.Configuration
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.flywaydb.core.Flyway

internal fun migrate(config: Configuration): Int {
    return hikariDataSourceWithVaultIntegration(config, Role.ADMIN).use {
        migrate(it, "SET ROLE \"${config.database.name}-${Role.ADMIN}\"")
    }
}

internal fun hikariDataSourceWithVaultIntegration(config: Configuration, role: Role = Role.USER): HikariDataSource {
    return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
            hikariConfigFrom(config),
            config.vault.mountPath,
            "${config.database.name}-$role"
    )
}

internal fun hikariConfigFrom(config: Configuration) =
        HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://${config.database.host}:${config.database.port}/${config.database.name}"
            maximumPoolSize = 2
            minimumIdle = 0
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        }

internal fun migrate(dataSource: HikariDataSource, initSql: String = ""): Int =
        Flyway.configure().dataSource(dataSource).initSql(initSql).load().migrate()

internal fun clean(dataSource: HikariDataSource) = Flyway.configure().dataSource(dataSource).load().clean()

internal enum class Role {
    ADMIN, USER;

    override fun toString() = name.toLowerCase()
}
