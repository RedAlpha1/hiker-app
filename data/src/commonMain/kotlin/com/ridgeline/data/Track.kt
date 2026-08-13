package com.ridgeline.data

import com.ridgeline.engine.GeoPoint

/**
 * A recorded run/trail. Client-generated UUID [id] (see [IdGenerator]),
 * `createdAt`/`updatedAt` on every row, soft delete via [deletedAtEpochMs] --
 * see DECISIONS.md, "No backend, no accounts in v1": this is what keeps sync
 * deferrable without a migration.
 */
data class Track(
    val id: String,
    val name: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long?,
    val distanceM: Double,
    val elevationGainM: Double,
    val elevationLossM: Double,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val deletedAtEpochMs: Long? = null,
) {
    val isFinished: Boolean get() = endedAtEpochMs != null
}

/** One GPS sample of a [Track], in recording order (see [seq]). */
data class TrackPoint(
    val trackId: String,
    val seq: Int,
    val location: GeoPoint,
    val elevationM: Double,
    val recordedAtEpochMs: Long,
)
