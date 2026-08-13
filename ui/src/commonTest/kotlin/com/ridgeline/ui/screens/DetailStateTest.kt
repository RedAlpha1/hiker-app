package com.ridgeline.ui.screens

import com.ridgeline.data.Track
import com.ridgeline.data.TrackPoint
import com.ridgeline.engine.GeoPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DetailStateTest {

    private val track = Track(
        id = "track-1",
        name = "Kausani ridge walk",
        startedAtEpochMs = 0L,
        endedAtEpochMs = 3_600_000L,
        distanceM = 8_000.0,
        elevationGainM = 450.0,
        elevationLossM = 300.0,
        createdAtEpochMs = 0L,
        updatedAtEpochMs = 0L,
    )

    @Test
    fun maxAndMinElevationComeFromThePoints() {
        val points = listOf(
            TrackPoint("track-1", 0, GeoPoint(29.84, 79.60), elevationM = 1890.0, recordedAtEpochMs = 0L),
            TrackPoint("track-1", 1, GeoPoint(29.85, 79.61), elevationM = 2340.0, recordedAtEpochMs = 1_000L),
            TrackPoint("track-1", 2, GeoPoint(29.86, 79.62), elevationM = 2100.0, recordedAtEpochMs = 2_000L),
        )
        val state = DetailState(track, points)
        assertEquals(2340.0, state.maxElevationM)
        assertEquals(1890.0, state.minElevationM)
    }

    @Test
    fun elevationExtremesAreNullWithNoPoints() {
        val state = DetailState(track, points = emptyList())
        assertNull(state.maxElevationM)
        assertNull(state.minElevationM)
    }
}
