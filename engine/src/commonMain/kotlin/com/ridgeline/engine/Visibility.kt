package com.ridgeline.engine

/**
 * Confidence that a resolved peak is actually visible/identifiable to the
 * user, based on distance-driven atmospheric haze. Thresholds are estimates,
 * not measurements — see DECISIONS.md, "Distant peaks get dots, not names".
 */
enum class Confidence { CLEAR, HAZY, MARGINAL }

/**
 * Tuning for [resolveVisiblePeaks]. Defaults are the Indian-Himalaya values
 * from DECISIONS.md, "Region scope: Uttarakhand and Himachal only, Indian
 * users" — do not lower [maxRangeM] to an Alpine-scale 60km without reading
 * that entry; it discards most of the visible skyline from Indian viewpoints.
 */
data class VisibilityConfig(
    val maxRangeM: Double = 150_000.0,
    /** DEM vertical error budget (Copernicus GLO-30 RMSE), metres. A ray isn't
     *  called occluded unless terrain clears the line of sight by more than this. */
    val terrainToleranceM: Double = 4.0,
    /** Raycast step. ~3,750 steps per peak at max range — see CLAUDE.md, "Engine performance contract". */
    val rayStepM: Double = 40.0,
    val hazyThresholdM: Double = 80_000.0,
    val marginalThresholdM: Double = 120_000.0,
) {
    init {
        require(hazyThresholdM < marginalThresholdM) { "hazyThresholdM must be less than marginalThresholdM" }
        require(rayStepM > 0) { "rayStepM must be positive" }
    }

    fun confidenceFor(distanceM: Double): Confidence = when {
        distanceM <= hazyThresholdM -> Confidence.CLEAR
        distanceM <= marginalThresholdM -> Confidence.HAZY
        else -> Confidence.MARGINAL
    }
}

/** A peak resolved as visible from a [Viewpoint], with the geometry [project] needs. */
data class VisiblePeak(
    val peak: Peak,
    val distanceM: Double,
    val bearingDeg: Double,
    /** Angle above the horizontal, degrees. Positive = above eye level. */
    val elevationAngleDeg: Double,
    val confidence: Confidence,
)

/**
 * Resolves which of [peaks] are visible from [viewpoint]: within range, and
 * with no intervening terrain (per [dem]) breaking the line of sight once
 * earth curvature and refraction are accounted for.
 *
 * Expensive — a 150km raycast at 40m steps is ~3,750 DEM lookups per peak.
 * Call on position change (~every 50m), never per frame. See CLAUDE.md,
 * "Engine performance contract".
 */
fun resolveVisiblePeaks(
    viewpoint: Viewpoint,
    peaks: List<Peak>,
    dem: ElevationSource,
    config: VisibilityConfig = VisibilityConfig(),
): List<VisiblePeak> {
    val eyeElevation = viewpoint.eyeElevationM

    return peaks.mapNotNull { peak ->
        val distance = distanceM(viewpoint.location, peak.location)
        if (distance <= 0.0 || distance > config.maxRangeM) return@mapNotNull null

        val bearing = bearingDeg(viewpoint.location, peak.location)
        val targetDrop = earthCurvatureDropM(distance)
        // Apparent target elevation once curvature+refraction have pulled it down.
        val apparentTargetElevation = peak.elevationM - targetDrop
        val straightLineAngle = kotlin.math.atan2(
            apparentTargetElevation - eyeElevation,
            distance,
        )

        if (isOccluded(viewpoint, peak, distance, bearing, dem, config)) return@mapNotNull null

        VisiblePeak(
            peak = peak,
            distanceM = distance,
            bearingDeg = bearing,
            elevationAngleDeg = straightLineAngle.toDegrees(),
            confidence = config.confidenceFor(distance),
        )
    }
}

/**
 * Raycasts from the viewpoint's eye toward [peak] at [VisibilityConfig.rayStepM]
 * intervals, comparing the straight line-of-sight elevation (curvature- and
 * refraction-corrected) against DEM terrain at each step. True if any
 * intermediate point's terrain clears the sightline by more than
 * [VisibilityConfig.terrainToleranceM].
 */
private fun isOccluded(
    viewpoint: Viewpoint,
    peak: Peak,
    distance: Double,
    bearing: Double,
    dem: ElevationSource,
    config: VisibilityConfig,
): Boolean {
    val eyeElevation = viewpoint.eyeElevationM
    val targetElevation = peak.elevationM
    var stepDistance = config.rayStepM
    while (stepDistance < distance) {
        val fraction = stepDistance / distance
        val samplePoint = destinationPoint(viewpoint.location, stepDistance, bearing)

        // Straight line-of-sight elevation at this distance, ignoring curvature.
        val lineOfSightElevation = eyeElevation + fraction * (targetElevation - eyeElevation)
        // Curvature+refraction pulls distant terrain down relative to a flat-earth line of sight.
        val dropAtStep = earthCurvatureDropM(stepDistance)
        val sightlineElevation = lineOfSightElevation - dropAtStep

        val terrainElevation = dem.elevationAt(samplePoint)
        if (terrainElevation > sightlineElevation + config.terrainToleranceM) {
            return true
        }
        stepDistance += config.rayStepM
    }
    return false
}
