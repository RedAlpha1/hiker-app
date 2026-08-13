package com.ridgeline.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `engine/test/ConfidenceCheck.kt` per CLAUDE.md — covers the haze-threshold confidence
 * tiers from DECISIONS.md, "Distant peaks get dots, not names": CLEAR/HAZY/MARGINAL by
 * distance, and HAZY ranking below CLEAR when lanes run out. Same Kausani viewpoint and
 * real summits as GarhwalCheck.kt.
 */
class ConfidenceCheck {

    private val kausani = Viewpoint(location = GeoPoint(29.8422, 79.6006), elevationM = 1890.0)
    private val config = VisibilityConfig() // defaults: 80km / 120km thresholds

    private val nandaDevi = Peak("nanda-devi", "Nanda Devi", location = GeoPoint(30.3753, 79.9707), elevationM = 7816.0) // ~69km -> CLEAR
    private val chaukhamba = Peak("chaukhamba", "Chaukhamba", location = GeoPoint(30.7300, 79.3600), elevationM = 7138.0) // ~101km -> HAZY
    private val kamet = Peak("kamet", "Kamet", location = GeoPoint(30.9235, 79.5920), elevationM = 7756.0) // ~120km -> MARGINAL
    private val shivling = Peak("shivling", "Shivling", location = GeoPoint(30.8817, 79.0672), elevationM = 6543.0) // ~126km -> MARGINAL

    private val allPeaks = listOf(nandaDevi, chaukhamba, kamet, shivling)

    @Test
    fun classifiesByDistanceThreshold() {
        val visible = resolveVisiblePeaks(kausani, allPeaks, FlatElevationSource(), config).associateBy { it.peak.id }

        assertEquals(Confidence.CLEAR, visible.getValue("nanda-devi").confidence)
        assertEquals(Confidence.HAZY, visible.getValue("chaukhamba").confidence)
        assertEquals(Confidence.MARGINAL, visible.getValue("kamet").confidence)
        assertEquals(Confidence.MARGINAL, visible.getValue("shivling").confidence)
    }

    @Test
    fun marginalPeaksRenderAsDotsNotChips() {
        val visible = resolveVisiblePeaks(kausani, allPeaks, FlatElevationSource(), config)
        val frame = CameraFrame(
            headingDeg = bearingDeg(kausani.location, chaukhamba.location),
            horizontalFovDeg = 90.0,
            verticalFovDeg = 40.0,
            widthPx = 1920,
            heightPx = 1080,
        )
        val projected = project(visible, frame)
        val layoutConfig = LabelLayoutConfig(laneCount = 4, topMarginPx = 100.0, laneSpacingPx = 56.0, chipWidthPx = 220.0)
        val placed = layoutLabels(projected, layoutConfig)

        val marginal = placed.filter { it.visiblePeak.confidence == Confidence.MARGINAL }
        assertTrue(marginal.isNotEmpty(), "expected at least one MARGINAL peak in frame")
        marginal.forEach { assertTrue(it.isDot, "${it.visiblePeak.peak.name} is MARGINAL but was placed as a chip") }
    }

    @Test
    fun hazyRanksBelowClearWhenLanesRunOut() {
        // One lane only, both peaks projected close together in x -- forces a fight over
        // the single slot. CLEAR must win per DECISIONS.md, "Distant peaks get dots, not
        // names": "HAZY peaks keep names but rank below CLEAR ones when lanes run out."
        val clearSighting = VisiblePeak(nandaDevi, distanceM = 69_000.0, bearingDeg = 0.0, elevationAngleDeg = 5.0, confidence = Confidence.CLEAR)
        val hazySighting = VisiblePeak(chaukhamba, distanceM = 101_000.0, bearingDeg = 0.5, elevationAngleDeg = 5.0, confidence = Confidence.HAZY)

        val frame = CameraFrame(headingDeg = 0.0, horizontalFovDeg = 90.0, verticalFovDeg = 40.0, widthPx = 1920, heightPx = 1080)
        val projected = project(listOf(clearSighting, hazySighting), frame)
        val layoutConfig = LabelLayoutConfig(laneCount = 1, topMarginPx = 100.0, laneSpacingPx = 56.0, chipWidthPx = 220.0)
        val placed = layoutLabels(projected, layoutConfig)

        val clearPlacement = placed.first { it.visiblePeak.peak.id == "nanda-devi" }
        val hazyPlacement = placed.first { it.visiblePeak.peak.id == "chaukhamba" }
        assertTrue(!clearPlacement.isDot, "CLEAR peak should win the only lane")
        assertTrue(hazyPlacement.isDot, "HAZY peak should be demoted to a dot when it loses the lane fight to CLEAR")
    }
}
