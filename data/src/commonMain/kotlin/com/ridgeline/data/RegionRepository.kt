package com.ridgeline.data

/**
 * Region catalog + download state. Fetching/downloading the actual tiles and
 * peak data is a separate concern (networking, platform file storage) that
 * doesn't belong in this module's scope yet -- this repository is only the
 * local source of truth for "what regions exist" and "is this one on
 * device," which [markDownloaded] and [PeakRepository.replaceRegionPeaks]
 * get told about once a download finishes.
 */
interface RegionRepository {
    fun list(): List<Region>
    fun get(id: String): Region?

    /**
     * Adds or refreshes a region's catalog metadata (name, bounds, available
     * peak data version) without disturbing its download state -- re-syncing
     * the catalog must not un-download a region the user already has.
     */
    fun upsertMetadata(id: String, name: String, bounds: RegionBounds, peakDataVersion: String)

    fun markDownloaded(id: String, sizeBytes: Long)
}
