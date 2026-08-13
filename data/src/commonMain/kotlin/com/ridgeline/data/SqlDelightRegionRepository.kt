package com.ridgeline.data

import com.ridgeline.data.db.RegionEntity
import com.ridgeline.data.db.RidgelineDatabase
import kotlinx.datetime.Clock

class SqlDelightRegionRepository(
    private val db: RidgelineDatabase,
    private val clock: Clock = Clock.System,
) : RegionRepository {

    override fun list(): List<Region> = db.regionQueries.selectAll().executeAsList().map { it.toRegion() }

    override fun get(id: String): Region? = db.regionQueries.selectById(id).executeAsOneOrNull()?.toRegion()

    override fun upsertMetadata(id: String, name: String, bounds: RegionBounds, peakDataVersion: String) {
        // Preserve existing download state -- re-syncing the catalog must not
        // un-download a region the user already has. See RegionRepository docs.
        val existing = db.regionQueries.selectById(id).executeAsOneOrNull()
        db.regionQueries.upsertMetadata(
            id = id,
            name = name,
            southDeg = bounds.southDeg,
            westDeg = bounds.westDeg,
            northDeg = bounds.northDeg,
            eastDeg = bounds.eastDeg,
            peakDataVersion = peakDataVersion,
            downloadedAtEpochMs = existing?.downloadedAtEpochMs,
            sizeBytes = existing?.sizeBytes ?: 0L,
        )
    }

    override fun markDownloaded(id: String, sizeBytes: Long) {
        db.regionQueries.markDownloaded(
            downloadedAtEpochMs = clock.now().toEpochMilliseconds(),
            sizeBytes = sizeBytes,
            id = id,
        )
    }
}

private fun RegionEntity.toRegion() = Region(
    id = id,
    name = name,
    bounds = RegionBounds(southDeg, westDeg, northDeg, eastDeg),
    peakDataVersion = peakDataVersion,
    downloadedAtEpochMs = downloadedAtEpochMs,
    sizeBytes = sizeBytes,
)
