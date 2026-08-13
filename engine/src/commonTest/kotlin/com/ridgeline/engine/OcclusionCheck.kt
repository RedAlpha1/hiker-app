package com.ridgeline.engine

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * `engine/test/OcclusionCheck.kt` — isolated test of the raycast occlusion algorithm in
 * `resolveVisiblePeaks`. Uses synthetic terrain by design: this checks the algorithm's
 * behaviour against terrain profiles authored to exercise it, not a real viewpoint's
 * skyline. That's a different concern from Harness.kt/GarhwalCheck.kt using real
 * coordinates for geodesy — see DECISIONS.md, "Design file peak data is decorative, not
 * ground truth", which is about fabricated bearings/distances standing in for real ones,
 * not about hand-authored terrain for a unit test of the occlusion math itself.
 */
class OcclusionCheck {

    private val viewpoint = Viewpoint(location = GeoPoint(0.0, 0.0), elevationM = 100.0)
    private val target = Peak(
        id = "far-peak",
        name = "Far Peak",
        location = destinationPoint(GeoPoint(0.0, 0.0), 10_000.0, 90.0),
        elevationM = 500.0,
    )
    private val config = VisibilityConfig(maxRangeM = 20_000.0, rayStepM = 50.0, terrainToleranceM = 4.0)
    private val totalDistance = distanceM(viewpoint.location, target.location)

    @Test
    fun clearLineOfSightIsVisible() {
        val visible = resolveVisiblePeaks(viewpoint, listOf(target), FlatElevationSource(), config)
        assertTrue(visible.isNotEmpty(), "flat terrain well below the sightline should not occlude")
    }

    @Test
    fun aRidgeAboveTheSightlineOccludes() {
        // A wall halfway to the target, taller than any point on the straight line
        // between eye and summit.
        val wallAt = destinationPoint(GeoPoint(0.0, 0.0), 5_000.0, 90.0)
        val dem = ElevationSource { point -> if (distanceM(point, wallAt) < 200.0) 1_000.0 else 0.0 }
        val visible = resolveVisiblePeaks(viewpoint, listOf(target), dem, config)
        assertTrue(visible.isEmpty(), "a 1000m ridge halfway to a 500m summit should occlude it")
    }

    @Test
    fun terrainWithinToleranceDoesNotOcclude() {
        // Terrain that tracks a few metres above the geometric sightline, within the DEM
        // error budget (terrainToleranceM), should not flip a peak to occluded.
        val eyeElevation = viewpoint.eyeElevationM
        val dem = ElevationSource { point ->
            val d = distanceM(viewpoint.location, point)
            val fraction = (d / totalDistance).coerceIn(0.0, 1.0)
            val lineOfSightElevation = eyeElevation + fraction * (target.elevationM - eyeElevation)
            val sightlineElevation = lineOfSightElevation - earthCurvatureDropM(d)
            sightlineElevation + config.terrainToleranceM * 0.5
        }
        val visible = resolveVisiblePeaks(viewpoint, listOf(target), dem, config)
        assertTrue(visible.isNotEmpty(), "terrain within terrainToleranceM should not occlude")
    }
}
