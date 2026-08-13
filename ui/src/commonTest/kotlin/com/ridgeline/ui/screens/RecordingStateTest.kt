package com.ridgeline.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecordingStateTest {

    @Test
    fun togglePauseFlipsBetweenRecordingAndPaused() {
        val recording = RecordingState(activityType = ActivityType.HIKE, status = RecordingStatus.RECORDING)
        val paused = recording.togglePause()
        assertTrue(paused.isPaused)
        assertEquals(recording, paused.togglePause())
    }

    @Test
    fun togglePauseFromNoGpsFixGoesToPaused() {
        val state = RecordingState(activityType = ActivityType.RUN, status = RecordingStatus.NO_GPS_FIX)
        assertFalse(state.hasGpsFix)
        assertEquals(RecordingStatus.PAUSED, state.togglePause().status)
    }

    @Test
    fun requestEndAndCancelEndToggleTheConfirmationFlag() {
        val state = RecordingState(activityType = ActivityType.HIKE)
        val pending = state.requestEnd()
        assertTrue(pending.endConfirmationPending)
        assertFalse(pending.cancelEnd().endConfirmationPending)
    }
}
