package com.ridgeline.data.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform boundary for opening the SQLite connection -- one of the
 * expect/actual seams CLAUDE.md calls out (alongside location, device
 * attitude, camera preview, background execution, map view).
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(factory: DatabaseDriverFactory): RidgelineDatabase =
    RidgelineDatabase(factory.createDriver())
