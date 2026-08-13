package com.ridgeline.data

import com.ridgeline.data.db.PeakEntity
import com.ridgeline.data.db.RidgelineDatabase
import com.ridgeline.engine.GeoPoint
import com.ridgeline.engine.Peak

class SqlDelightPeakRepository(private val db: RidgelineDatabase) : PeakRepository {

    override fun peaksInRegion(regionId: String): List<Peak> =
        db.peakQueries.selectForRegion(regionId).executeAsList().map { it.toPeak() }

    override fun replaceRegionPeaks(regionId: String, peaks: List<Peak>) {
        db.transaction {
            db.peakQueries.deleteForRegion(regionId)
            peaks.forEach { peak ->
                // Peak names/aliases never contain commas -- see Peak.sq.
                db.peakQueries.insert(
                    id = peak.id,
                    regionId = regionId,
                    name = peak.name,
                    nameLocal = peak.nameLocal,
                    aliases = peak.aliases.joinToString(","),
                    latDeg = peak.location.latDeg,
                    lonDeg = peak.location.lonDeg,
                    elevationM = peak.elevationM,
                )
            }
        }
    }
}

private fun PeakEntity.toPeak() = Peak(
    id = id,
    name = name,
    nameLocal = nameLocal,
    aliases = if (aliases.isEmpty()) emptyList() else aliases.split(","),
    location = GeoPoint(latDeg, lonDeg),
    elevationM = elevationM,
)
