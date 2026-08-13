package com.ridgeline.ui.screens

import com.ridgeline.data.Region
import com.ridgeline.data.Track

/**
 * The three library states from the design file: first-launch empty state,
 * populated (hikes + region catalog), and a region actively downloading.
 * Reuses `:data`'s own `Track`/`Region` -- no separate UI-facing copy of
 * either shape.
 */
sealed interface LibraryState {
    data object Empty : LibraryState

    data class Populated(
        val hikes: List<Track>,
        val regions: List<Region>,
    ) : LibraryState

    data class DownloadingRegion(
        val hikes: List<Track>,
        val regions: List<Region>,
        val progress: RegionDownloadProgress,
    ) : LibraryState
}

data class RegionDownloadProgress(
    val regionName: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
) {
    val fraction: Float get() = if (totalBytes <= 0L) 0f else (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
}
