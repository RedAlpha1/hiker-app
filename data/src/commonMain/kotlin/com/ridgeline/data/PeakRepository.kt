package com.ridgeline.data

import com.ridgeline.engine.Peak

/**
 * On-device cache of a region's hand-curated peaks. Returns [Peak] --
 * :engine's own domain type -- directly, so callers can feed the result
 * straight into `resolveVisiblePeaks` without a translation step.
 */
interface PeakRepository {
    fun peaksInRegion(regionId: String): List<Peak>

    /** Replaces the cached peak set for [regionId] wholesale, e.g. after a region download. */
    fun replaceRegionPeaks(regionId: String, peaks: List<Peak>)
}
