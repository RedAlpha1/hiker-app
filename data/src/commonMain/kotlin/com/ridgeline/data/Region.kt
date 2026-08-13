package com.ridgeline.data

import com.ridgeline.engine.GeoPoint

/**
 * A downloadable region catalog entry (CLAUDE.md, "Curated regions, not
 * worldwide"). [downloadedAtEpochMs] is null until the DEM tiles and peak
 * data have actually landed on-device -- [PeakRepository] only has rows for
 * regions where that's happened.
 */
data class Region(
    val id: String,
    val name: String,
    val bounds: RegionBounds,
    val peakDataVersion: String,
    val downloadedAtEpochMs: Long?,
    val sizeBytes: Long,
) {
    val isDownloaded: Boolean get() = downloadedAtEpochMs != null
}

data class RegionBounds(
    val southDeg: Double,
    val westDeg: Double,
    val northDeg: Double,
    val eastDeg: Double,
) {
    operator fun contains(point: GeoPoint): Boolean =
        point.latDeg in southDeg..northDeg && point.lonDeg in westDeg..eastDeg
}
