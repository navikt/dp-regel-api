package no.nav.dagpenger.regel.api.db

import com.zaxxer.hikari.HikariConfig
import no.nav.dagpenger.regel.api.Configuration
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.flywaydb.core.Flyway

internal fun migrate(config: Configuration) {
    HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
            HikariConfig().apply {
                this.jdbcUrl = jdbcUrlFrom(config)
                this.maximumPoolSize = 1
            },
            config.vault.mountPath,
            pgRoleFrom(config)
    ).use {
        Flyway.configure().initSql("SET ROLE \"${pgRoleFrom(config)}\"").load().migrate()
    }
}

private fun pgRoleFrom(config: Configuration) = "${config.database.name}-admin"

private fun jdbcUrlFrom(config: Configuration) =
        "jdbc:postgresql://${config.database.host}:${config.database.port}/${config.database.name}"
