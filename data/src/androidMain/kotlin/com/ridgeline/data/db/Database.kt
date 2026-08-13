package com.ridgeline.data.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * Standard SQLDelight Android-driver setup, mirroring jvmMain/iosMain --
 * this was the missing piece: `libs.sqldelight.android.driver` was already
 * wired into build.gradle.kts's conditional androidMain source set, but no
 * actual implemented this expect class, so `:android` had no way to open
 * the on-device database at all. [context] should be an application
 * context (see RecordingService, which constructs this).
 *
 * Unverified in this sandbox (no Android SDK) -- standard driver
 * boilerplate straight from SQLDelight's own docs, not app-specific logic,
 * same caveat as the existing iosMain actual.
 */
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(RidgelineDatabase.Schema, context, "ridgeline.db")
}
