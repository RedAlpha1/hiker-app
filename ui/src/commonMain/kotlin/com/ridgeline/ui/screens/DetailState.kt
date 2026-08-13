package com.ridgeline.ui.screens

import com.ridgeline.data.Track
import com.ridgeline.data.TrackPoint

/**
 * A timestamped note taken during a track (screen 7's "Notes List"). UI-only
 * for now, same caveat as [com.ridgeline.ui.screens.PeakSighting] -- no
 * `:data` table backs this yet.
 */
data class TrackNote(val recordedAtEpochMs: Long, val elevationM: Double, val text: String)

data class DetailState(
    val track: Track,
    val points: List<TrackPoint>,
    val notes: List<TrackNote> = emptyList(),
) {
    val maxElevationM: Double? get() = points.maxOfOrNull { it.elevationM }
    val minElevationM: Double? get() = points.minOfOrNull { it.elevationM }
}
