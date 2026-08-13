package com.ridgeline.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

internal class FakeClock(private var epochMs: Long = 0L) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(epochMs)
    fun advance(byMs: Long) {
        epochMs += byMs
    }
}
