package com.ridgeline.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Anchored to the real Kausani viewpoint (same coordinate GarhwalCheck.kt
 * uses) per this project's real-coordinates testing convention -- samples
 * below are built with [destinationPoint] off that real anchor, the same
 * technique [Visibility.kt]'s own occlusion raycast uses internally, rather
 * than inventing unanchored lat/lon pairs.
 */
class RecordingSessionTest {

    private val kausani = GeoPoint(29.8422, 79.6006)

    @Test
    fun accumulatesDistanceAndDurationAcrossSamples() {
        val session = RecordingSession()
        val hundredMetersNorth = destinationPoint(kausani, 100.0, bearingDeg = 0.0)
        val twoHundredMetersNorth = destinationPoint(kausani, 200.0, bearingDeg = 0.0)

        session.addSample(RecordingSample(kausani, elevationM = 1890.0, timestampMs = 0))
        session.addSample(RecordingSample(hundredMetersNorth, elevationM = 1890.0, timestampMs = 60_000))
        session.addSample(RecordingSample(twoHundredMetersNorth, elevationM = 1890.0, timestampMs = 120_000))

        assertApprox(200.0, session.totals().distanceM, tolerance = 1.0)
        assertEquals(120_000L, session.totals().durationMs)
    }

    @Test
    fun elevationJitterBelowThresholdIsIgnored() {
        val session = RecordingSession()
        session.addSample(RecordingSample(kausani, elevationM = 1890.0, timestampMs = 0))
        session.addSample(RecordingSample(kausani, elevationM = 1891.5, timestampMs = 1_000)) // +1.5m, below default 3m
        session.addSample(RecordingSample(kausani, elevationM = 1889.0, timestampMs = 2_000)) // -1.0m off original baseline

        val totals = session.totals()
        assertEquals(0.0, totals.elevationGainM)
        assertEquals(0.0, totals.elevationLossM)
    }

    @Test
    fun genuineClimbAndDescentCrossTheThreshold() {
        val session = RecordingSession()
        session.addSample(RecordingSample(kausani, elevationM = 1890.0, timestampMs = 0))
        session.addSample(RecordingSample(kausani, elevationM = 1900.0, timestampMs = 1_000)) // +10m climb
        session.addSample(RecordingSample(kausani, elevationM = 1893.0, timestampMs = 2_000)) // -7m descent

        val totals = session.totals()
        assertApprox(10.0, totals.elevationGainM, tolerance = 0.01)
        assertApprox(7.0, totals.elevationLossM, tolerance = 0.01)
    }

    @Test
    fun pauseExcludesTheGapFromDistanceAndDuration() {
        val session = RecordingSession()
        val hundredMetersNorth = destinationPoint(kausani, 100.0, bearingDeg = 0.0)
        val twoHundredMetersNorth = destinationPoint(kausani, 200.0, bearingDeg = 0.0)
        val threeHundredMetersNorth = destinationPoint(kausani, 300.0, bearingDeg = 0.0)
        val fourHundredMetersNorth = destinationPoint(kausani, 400.0, bearingDeg = 0.0)

        session.addSample(RecordingSample(kausani, elevationM = 1890.0, timestampMs = 0))
        session.addSample(RecordingSample(hundredMetersNorth, elevationM = 1890.0, timestampMs = 60_000))
        session.pause()
        assertTrue(session.isPaused)

        // A long break at trailhead lunch -- ignored entirely while paused.
        session.addSample(RecordingSample(twoHundredMetersNorth, elevationM = 1890.0, timestampMs = 3_600_000))
        assertApprox(100.0, session.totals().distanceM, tolerance = 1.0)
        assertEquals(60_000L, session.totals().durationMs)

        session.resume()
        // First sample after resume only re-anchors position/time, no jump counted.
        session.addSample(RecordingSample(threeHundredMetersNorth, elevationM = 1890.0, timestampMs = 3_601_000))
        assertApprox(100.0, session.totals().distanceM, tolerance = 1.0)
        assertEquals(60_000L, session.totals().durationMs)

        session.addSample(RecordingSample(fourHundredMetersNorth, elevationM = 1890.0, timestampMs = 3_602_000))
        assertApprox(200.0, session.totals().distanceM, tolerance = 1.0)
        assertEquals(61_000L, session.totals().durationMs)
    }

    @Test
    fun estimateRunCaloriesScalesWithDistance() {
        assertEquals(0, estimateRunCaloriesKcal(0.0))
        assertEquals(65, estimateRunCaloriesKcal(1_000.0))
        assertEquals(325, estimateRunCaloriesKcal(5_000.0))
    }
}
