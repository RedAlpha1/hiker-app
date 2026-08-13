package com.ridgeline.data

import com.ridgeline.engine.GeoPoint

/** Repository interface over all track data access -- see DECISIONS.md, "No
 *  backend, no accounts in v1": this is what keeps sync deferrable later. */
interface TrackRepository {
    fun list(): List<Track>
    fun get(id: String): Track?
    fun points(trackId: String): List<TrackPoint>

    /** Starts a new, unfinished track (no [Track.endedAtEpochMs] yet). */
    fun start(name: String): Track

    /** Appends a GPS sample, assigning the next [TrackPoint.seq] for [trackId]. */
    fun appendPoint(trackId: String, location: GeoPoint, elevationM: Double, recordedAtEpochMs: Long): TrackPoint

    /** Marks a track finished and records its computed summary. */
    fun finish(trackId: String, distanceM: Double, elevationGainM: Double, elevationLossM: Double): Track

    fun rename(trackId: String, name: String): Track

    /** Soft delete -- see [Track.deletedAtEpochMs]. */
    fun delete(trackId: String)
}
