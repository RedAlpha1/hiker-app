package com.ridgeline.ui.screens

import com.ridgeline.engine.CameraFrame
import com.ridgeline.engine.Confidence
import com.ridgeline.engine.ElevationSource
import com.ridgeline.engine.GeoPoint
import com.ridgeline.engine.LabelLayoutConfig
import com.ridgeline.engine.Peak
import com.ridgeline.engine.PlacedLabel
import com.ridgeline.engine.VisibilityConfig
import com.ridgeline.engine.VisiblePeak
import com.ridgeline.engine.Viewpoint
import com.ridgeline.engine.bearingDeg
import com.ridgeline.engine.layoutLabels
import com.ridgeline.engine.project
import com.ridgeline.engine.resolveVisiblePeaks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArViewfinderStateTest {

    /** No terrain -- these tests only check distance/bearing/confidence wiring, not occlusion. */
    private val flatTerrain = ElevationSource { 0.0 }

    private fun testVisiblePeak(confidence: Confidence = Confidence.CLEAR) = VisiblePeak(
        peak = Peak("p", "Test Peak", location = GeoPoint(0.0, 0.0), elevationM = 1000.0),
        distanceM = 1000.0,
        bearingDeg = 0.0,
        elevationAngleDeg = 5.0,
        confidence = confidence,
    )

    @Test
    fun noElevationDataModeZeroesPeakCountRegardlessOfLabels() {
        val placed = PlacedLabel(testVisiblePeak(), anchorXPx = 0.0, anchorYPx = 0.0, isDot = false)
        val state = ArViewfinderState(mode = ArMode.NO_ELEVATION_DATA, labels = listOf(placed))
        assertEquals(0, state.peakCount)
    }

    @Test
    fun liveModeReportsRealLabelCount() {
        val label = PlacedLabel(testVisiblePeak(), anchorXPx = 0.0, anchorYPx = 0.0, isDot = false)
        val state = ArViewfinderState(mode = ArMode.LIVE, labels = listOf(label, label))
        assertEquals(2, state.peakCount)
    }

    @Test
    fun nudgeAccumulatesIntoEffectiveHeading() {
        var state = ArViewfinderState(headingDeg = 100.0)
        state = state.nudge(3).nudge(-1)
        assertEquals(2, state.headingNudgeDeg)
        assertEquals(102.0, state.effectiveHeadingDeg)
    }

    @Test
    fun selectAndDeselectTrackTheSelectedPeak() {
        val peak = testVisiblePeak()
        var state = ArViewfinderState()
        assertFalse(state.hasSelectedPeak)
        state = state.select(peak)
        assertTrue(state.hasSelectedPeak)
        assertEquals(peak, state.selectedPeak)
        state = state.deselect()
        assertFalse(state.hasSelectedPeak)
    }

    /**
     * The point of putting :engine's own types directly into ArViewfinderState:
     * feed it a real resolveVisiblePeaks -> project -> layoutLabels pipeline,
     * using the same real Kausani/Garhwal coordinates as :engine's own
     * GarhwalCheck.kt/ConfidenceCheck.kt, and confirm the wiring holds together
     * end to end -- not just that each piece compiles in isolation.
     */
    @Test
    fun realEnginePipelineFeedsDirectlyIntoScreenState() {
        val kausani = Viewpoint(location = GeoPoint(29.8422, 79.6006), elevationM = 1890.0)
        val nandaDevi = Peak("nanda-devi", "Nanda Devi", location = GeoPoint(30.3753, 79.9707), elevationM = 7816.0) // ~69km -> CLEAR
        val chaukhamba = Peak("chaukhamba", "Chaukhamba", location = GeoPoint(30.7300, 79.3600), elevationM = 7138.0) // ~101km -> HAZY
        val shivling = Peak("shivling", "Shivling", location = GeoPoint(30.8817, 79.0672), elevationM = 6543.0) // ~126km -> MARGINAL

        val visible = resolveVisiblePeaks(kausani, listOf(nandaDevi, chaukhamba, shivling), flatTerrain, VisibilityConfig())
        val heading = bearingDeg(kausani.location, chaukhamba.location)
        val frame = CameraFrame(headingDeg = heading, horizontalFovDeg = 90.0, verticalFovDeg = 40.0, widthPx = 1920, heightPx = 1080)
        val projected = project(visible, frame)
        val placed = layoutLabels(projected, LabelLayoutConfig(laneCount = 4, topMarginPx = 100.0, laneSpacingPx = 56.0, chipWidthPx = 220.0))

        val state = ArViewfinderState(mode = ArMode.LIVE, headingDeg = heading, labels = placed)

        assertEquals(3, state.peakCount)
        assertTrue(state.labels.any { !it.isDot && it.visiblePeak.confidence == Confidence.CLEAR })
        assertTrue(state.labels.any { it.isDot && it.visiblePeak.confidence == Confidence.MARGINAL })
    }
}
