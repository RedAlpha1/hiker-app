package com.ridgeline.ui.screens

enum class ActivityType { HIKE, RUN }

enum class RecordingStatus { RECORDING, PAUSED, NO_GPS_FIX }

/**
 * The active-recording sheet. Hike and run share this shape -- the design's
 * two variants (screens 5 and 5b) differ only in which three stats are shown
 * (gain/duration/pace for a hike; pace/duration/kcal for a run) and in
 * accent color, both rendering concerns keyed off [activityType], not
 * reasons to split the state shape.
 */
data class RecordingState(
    val activityType: ActivityType,
    val status: RecordingStatus = RecordingStatus.RECORDING,
    val distanceM: Double = 0.0,
    val elevationGainM: Double = 0.0,
    val durationMs: Long = 0L,
    val caloriesKcal: Int = 0,
    val endConfirmationPending: Boolean = false,
) {
    val isPaused: Boolean get() = status == RecordingStatus.PAUSED
    val hasGpsFix: Boolean get() = status != RecordingStatus.NO_GPS_FIX

    fun togglePause(): RecordingState = copy(
        status = if (status == RecordingStatus.PAUSED) RecordingStatus.RECORDING else RecordingStatus.PAUSED,
    )

    fun requestEnd(): RecordingState = copy(endConfirmationPending = true)
    fun cancelEnd(): RecordingState = copy(endConfirmationPending = false)
}
