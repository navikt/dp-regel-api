package no.nav.dagpenger.regel.api.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.dagpenger.regel.api.Configuration
import no.nav.dagpenger.regel.api.Profile
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.flywaydb.core.Flyway
import org.flywaydb.core.internal.configuration.ConfigUtils

internal fun migrate(config: Configuration): Int {
    return when (config.application.profile) {
        Profile.LOCAL -> HikariDataSource(hikariConfigFrom(config)).use { migrate(it) }
        else -> hikariDataSourceWithVaultIntegration(config, Role.ADMIN).use {
            migrate(it, "SET ROLE \"${config.database.name}-${Role.ADMIN}\"")
        }
    }
}

private fun hikariDataSourceWithVaultIntegration(config: Configuration, role: Role = Role.USER) =
    HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
        hikariConfigFrom(config),
        config.vault.mountPath,
        "${config.database.name}-$role"
    )

internal fun dataSourceFrom(config: Configuration): HikariDataSource = when (config.application.profile) {
    Profile.LOCAL -> HikariDataSource(hikariConfigFrom(config))
    else -> hikariDataSourceWithVaultIntegration(config)
}

internal fun hikariConfigFrom(config: Configuration) =
    HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://${config.database.host}:${config.database.port}/${config.database.name}"
        maximumPoolSize = 5
        minimumIdle = 1
        idleTimeout = 10001
        connectionTimeout = 1000
        maxLifetime = 30001
        config.database.user?.let { username = it }
        config.database.password?.let { password = it }
    }

internal fun migrate(dataSource: HikariDataSource, initSql: String = ""): Int =
    Flyway.configure().dataSource(dataSource).initSql(initSql).load().migrate().migrations.size

internal fun clean(dataSource: HikariDataSource) = Flyway.configure()
    .cleanDisabled(System.getProperty(ConfigUtils.CLEAN_DISABLED)?.toBooleanStrict() ?: true)
    .dataSource(dataSource).load().clean()

private enum class Role {
    ADMIN, USER;

    override fun toString() = name.lowercase()
}
