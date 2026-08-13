package com.ridgeline.engine

import kotlin.math.abs
import kotlin.test.assertTrue

/**
 * No terrain, ever — line of sight is limited only by earth curvature/refraction, not
 * local relief. Fine for tests that check distance/bearing/confidence math but not
 * occlusion by real terrain (see OcclusionCheck.kt for that).
 */
internal class FlatElevationSource(private val elevationM: Double = 0.0) : ElevationSource {
    override fun elevationAt(point: GeoPoint): Double = elevationM
}

internal fun assertApprox(expected: Double, actual: Double, tolerance: Double, message: String = "") {
    assertTrue(
        abs(expected - actual) <= tolerance,
        "$message expected=$expected actual=$actual tolerance=$tolerance",
    )
}
