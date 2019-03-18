package no.nav.dagpenger.regel.api.db

import org.flywaydb.core.Flyway
import javax.sql.DataSource

fun migrate(ds: DataSource) = Flyway.configure().dataSource(ds).load().migrate()

fun clean(ds: DataSource) = Flyway.configure().dataSource(ds).load().clean()
