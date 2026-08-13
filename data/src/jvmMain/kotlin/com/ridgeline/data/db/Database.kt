package com.ridgeline.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

/**
 * JVM has no shipping target in this app (see CLAUDE.md architecture) -- this
 * actual exists so `:data` can be exercised by real JVM tests against an
 * in-memory SQLite database. It always creates the schema fresh, which is
 * correct for `jdbc:sqlite::memory:` in tests but not a real migration
 * strategy; that only matters once there's a persistent JVM/desktop target.
 */
actual class DatabaseDriverFactory(private val jdbcUrl: String = "jdbc:sqlite::memory:") {
    actual fun createDriver(): SqlDriver {
        val driver: SqlDriver = JdbcSqliteDriver(jdbcUrl)
        RidgelineDatabase.Schema.create(driver)
        return driver
    }
}
