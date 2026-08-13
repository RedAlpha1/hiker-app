package com.ridgeline.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `engine/test/Harness.kt` per CLAUDE.md (physically under `src/commonTest` — that's
 * the source set that makes this runnable as a JVM test). Anchored to real Gornergrat /
 * Pennine Alps geometry, not the fabricated bearings in `Ridgeline_Standalone.html`
 * (DECISIONS.md, "Design file peak data is decorative, not ground truth"). Every
 * geometry change must keep this green.
 */
class Harness {

    private val gornergrat = Viewpoint(
        location = GeoPoint(45.9838, 7.7847),
        elevationM = 3089.0,
    )

    private val matterhorn = Peak("matterhorn", "Matterhorn", location = GeoPoint(45.9764, 7.6589), elevationM = 4478.0)
    private val dufourspitze = Peak("dufourspitze", "Dufourspitze", aliases = listOf("Monte Rosa"), location = GeoPoint(45.9369, 7.8666), elevationM = 4634.0)
    private val liskamm = Peak("liskamm", "Liskamm", location = GeoPoint(45.9306, 7.8348), elevationM = 4527.0)
    private val weisshorn = Peak("weisshorn", "Weisshorn", location = GeoPoint(46.1108, 7.7169), elevationM = 4506.0)
    private val dom = Peak("dom", "Dom", location = GeoPoint(46.0906, 7.8536), elevationM = 4545.0)
    private val breithorn = Peak("breithorn", "Breithorn", location = GeoPoint(45.9375, 7.6994), elevationM = 4164.0)
    private val zinalrothorn = Peak("zinalrothorn", "Zinalrothorn", location = GeoPoint(46.0672, 7.6867), elevationM = 4221.0)
    private val oberGabelhorn = Peak("ober-gabelhorn", "Ober Gabelhorn", location = GeoPoint(46.0453, 7.6489), elevationM = 4063.0)
    private val dentBlanche = Peak("dent-blanche", "Dent Blanche", location = GeoPoint(46.0439, 7.6033), elevationM = 4357.0)

    private val peaks = listOf(
        matterhorn, dufourspitze, liskamm, weisshorn, dom,
        breithorn, zinalrothorn, oberGabelhorn, dentBlanche,
    )

    // Alps-scale viewpoint: every summit above is 8-25km out -- nothing like the
    // Himalayan range this app ships for, so a tight maxRangeM is correct here, unlike
    // the 150km default (see CLAUDE.md, "VisibilityConfig.maxRangeM is 150km").
    private val config = VisibilityConfig(maxRangeM = 40_000.0)

    @Test
    fun allNamedPeaksAreVisibleAndClear() {
        val visible = resolveVisiblePeaks(gornergrat, peaks, FlatElevationSource(), config)
        assertEquals(peaks.size, visible.size, "expected every listed summit to resolve as visible")
        visible.forEach { v ->
            assertTrue(v.distanceM < 30_000.0, "${v.peak.name} unexpectedly far: ${v.distanceM}m")
            assertEquals(Confidence.CLEAR, v.confidence, "${v.peak.name} should be CLEAR at Alps range")
            assertTrue(v.elevationAngleDeg > 0.0, "${v.peak.name} should sit above eye level from Gornergrat")
        }
    }

    @Test
    fun peaksSurroundTheViewpointAcrossMostOfTheCompass() {
        // DECISIONS.md: "the real peaks around Gornergrat span 314° of bearing" -- not a
        // narrow frontal cluster like Ridgeline_Standalone.html's monotonic bearings
        // (214°, 198°, 187°, 179°, 165°) imply.
        val visible = resolveVisiblePeaks(gornergrat, peaks, FlatElevationSource(), config)
        val bearings = visible.map { it.bearingDeg }.sorted()
        val largestGap = bearings.indices.maxOf { i ->
            val next = if (i == bearings.lastIndex) bearings.first() + 360.0 else bearings[i + 1]
            next - bearings[i]
        }
        val spread = 360.0 - largestGap
        assertTrue(spread > 180.0, "expected summits to surround more than half the compass, spread was $spread")
    }

    @Test
    fun labelLayoutKeepsChipsFromCollidingWithinALandscapeFrame() {
        // DECISIONS.md, "Label lanes are absolute, not per-peak offsets": per-peak offsets
        // let chips in different lanes land within a chip-height of each other -- caught
        // by the harness as an overlapping pair. This test is that regression guard.
        val visible = resolveVisiblePeaks(gornergrat, peaks, FlatElevationSource(), config)
        val frame = CameraFrame(headingDeg = 250.0, horizontalFovDeg = 65.0, verticalFovDeg = 40.0, widthPx = 1920, heightPx = 1080)
        val projected = project(visible, frame)
        val layoutConfig = LabelLayoutConfig(laneCount = 4, topMarginPx = 100.0, laneSpacingPx = 56.0, chipWidthPx = 220.0)
        val placed = layoutLabels(projected, layoutConfig)

        val chips = placed.filter { !it.isDot }
        for (lane in 0 until layoutConfig.laneCount) {
            val inLane = chips.filter { it.laneIndex == lane }.sortedBy { it.chipXPx }
            for (i in 0 until inLane.size - 1) {
                val gap = inLane[i + 1].chipXPx!! - inLane[i].chipXPx!!
                assertTrue(
                    gap >= layoutConfig.chipWidthPx,
                    "chips overlap in lane $lane: ${inLane[i].visiblePeak.peak.name} and ${inLane[i + 1].visiblePeak.peak.name}",
                )
            }
        }
    }
}
