package com.ridgeline.engine

/** A [VisiblePeak] mapped onto screen space for the current camera heading/FOV. */
data class ProjectedPeak(
    val visiblePeak: VisiblePeak,
    val xPx: Double,
    val yPx: Double,
    /** False if outside the current horizontal FOV — caller should skip drawing it. */
    val inFrame: Boolean,
)

/**
 * Camera framing for [project]. `headingDeg` is the compass bearing the
 * camera currently points at (centre of frame).
 */
data class CameraFrame(
    val headingDeg: Double,
    val horizontalFovDeg: Double,
    val verticalFovDeg: Double,
    val widthPx: Int,
    val heightPx: Int,
)

/**
 * Cheap trigonometry mapping already-resolved [sightings] onto screen space
 * for the current [frame]. Runs per frame — see CLAUDE.md, "Engine
 * performance contract". Do not recompute [resolveVisiblePeaks] here, and do
 * not cache this across position changes (heading changes every frame; the
 * underlying sightings don't).
 */
fun project(sightings: List<VisiblePeak>, frame: CameraFrame): List<ProjectedPeak> {
    val halfHFov = frame.horizontalFovDeg / 2.0
    val halfVFov = frame.verticalFovDeg / 2.0

    return sightings.map { sighting ->
        val relativeBearing = angleDiffDeg(sighting.bearingDeg, frame.headingDeg)
        val x = frame.widthPx / 2.0 + (relativeBearing / halfHFov) * (frame.widthPx / 2.0)
        // Screen y grows downward; positive elevation angle should move up (toward y=0).
        val y = frame.heightPx / 2.0 - (sighting.elevationAngleDeg / halfVFov) * (frame.heightPx / 2.0)

        ProjectedPeak(
            visiblePeak = sighting,
            xPx = x,
            yPx = y,
            inFrame = relativeBearing >= -halfHFov && relativeBearing <= halfHFov,
        )
    }
}
