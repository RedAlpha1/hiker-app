package com.ridgeline.data

import com.ridgeline.data.db.RidgelineDatabase
import com.ridgeline.data.db.TrackEntity
import com.ridgeline.data.db.TrackPointEntity
import com.ridgeline.engine.GeoPoint
import kotlinx.datetime.Clock

class SqlDelightTrackRepository(
    private val db: RidgelineDatabase,
    private val clock: Clock = Clock.System,
) : TrackRepository {

    override fun list(): List<Track> = db.trackQueries.selectAll().executeAsList().map { it.toTrack() }

    override fun get(id: String): Track? = db.trackQueries.selectById(id).executeAsOneOrNull()?.toTrack()

    override fun points(trackId: String): List<TrackPoint> =
        db.trackPointQueries.selectForTrack(trackId).executeAsList().map { it.toTrackPoint() }

    override fun start(name: String): Track {
        val now = clock.now().toEpochMilliseconds()
        val track = Track(
            id = newId(),
            name = name,
            startedAtEpochMs = now,
            endedAtEpochMs = null,
            distanceM = 0.0,
            elevationGainM = 0.0,
            elevationLossM = 0.0,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
        )
        db.trackQueries.insert(
            id = track.id,
            name = track.name,
            startedAtEpochMs = track.startedAtEpochMs,
            endedAtEpochMs = track.endedAtEpochMs,
            distanceM = track.distanceM,
            elevationGainM = track.elevationGainM,
            elevationLossM = track.elevationLossM,
            createdAtEpochMs = track.createdAtEpochMs,
            updatedAtEpochMs = track.updatedAtEpochMs,
            deletedAtEpochMs = track.deletedAtEpochMs,
        )
        return track
    }

    override fun appendPoint(trackId: String, location: GeoPoint, elevationM: Double, recordedAtEpochMs: Long): TrackPoint {
        val nextSeq = db.trackPointQueries.countForTrack(trackId).executeAsOne().toInt()
        val point = TrackPoint(trackId, nextSeq, location, elevationM, recordedAtEpochMs)
        db.trackPointQueries.insert(
            trackId = point.trackId,
            seq = point.seq.toLong(),
            latDeg = point.location.latDeg,
            lonDeg = point.location.lonDeg,
            elevationM = point.elevationM,
            recordedAtEpochMs = point.recordedAtEpochMs,
        )
        return point
    }

    override fun finish(trackId: String, distanceM: Double, elevationGainM: Double, elevationLossM: Double): Track {
        val now = clock.now().toEpochMilliseconds()
        db.trackQueries.updateSummary(
            endedAtEpochMs = now,
            distanceM = distanceM,
            elevationGainM = elevationGainM,
            elevationLossM = elevationLossM,
            updatedAtEpochMs = now,
            id = trackId,
        )
        return requireNotNull(get(trackId)) { "finish() called on unknown track $trackId" }
    }

    override fun rename(trackId: String, name: String): Track {
        db.trackQueries.rename(name = name, updatedAtEpochMs = clock.now().toEpochMilliseconds(), id = trackId)
        return requireNotNull(get(trackId)) { "rename() called on unknown track $trackId" }
    }

    override fun delete(trackId: String) {
        val now = clock.now().toEpochMilliseconds()
        db.trackQueries.softDelete(deletedAtEpochMs = now, updatedAtEpochMs = now, id = trackId)
    }
}

private fun TrackEntity.toTrack() = Track(
    id = id,
    name = name,
    startedAtEpochMs = startedAtEpochMs,
    endedAtEpochMs = endedAtEpochMs,
    distanceM = distanceM,
    elevationGainM = elevationGainM,
    elevationLossM = elevationLossM,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
    deletedAtEpochMs = deletedAtEpochMs,
)

private fun TrackPointEntity.toTrackPoint() = TrackPoint(
    trackId = trackId,
    seq = seq.toInt(),
    location = GeoPoint(latDeg, lonDeg),
    elevationM = elevationM,
    recordedAtEpochMs = recordedAtEpochMs,
)
