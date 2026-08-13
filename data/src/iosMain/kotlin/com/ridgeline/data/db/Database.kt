package com.ridgeline.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * Standard SQLDelight native-driver setup. Not verified to compile in this
 * environment -- iOS targets are disabled on non-macOS hosts (see
 * DECISIONS.md, "iOS targets added to :engine, :data, :ui") -- but this is
 * boilerplate straight from SQLDelight's own docs, not app-specific logic.
 */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver = NativeSqliteDriver(RidgelineDatabase.Schema, "ridgeline.db")
}
