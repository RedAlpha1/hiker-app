package com.ridgeline.data

import com.ridgeline.data.db.DatabaseDriverFactory
import com.ridgeline.data.db.createDatabase
import com.ridgeline.engine.GeoPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TrackRepositoryTest {

    private fun newRepository(clock: FakeClock = FakeClock()): TrackRepository {
        val db = createDatabase(DatabaseDriverFactory())
        return SqlDelightTrackRepository(db, clock)
    }

    @Test
    fun startCreatesAnUnfinishedTrack() {
        val repo = newRepository()
        val track = repo.start("Kausani ridge walk")
        assertEquals("Kausani ridge walk", track.name)
        assertTrue(!track.isFinished)
        assertEquals(track, repo.get(track.id))
    }

    @Test
    fun appendPointAssignsSequentialSeq() {
        val repo = newRepository()
        val track = repo.start("Test")
        val p0 = repo.appendPoint(track.id, GeoPoint(29.84, 79.60), 1890.0, 1_000L)
        val p1 = repo.appendPoint(track.id, GeoPoint(29.85, 79.61), 1900.0, 2_000L)
        assertEquals(0, p0.seq)
        assertEquals(1, p1.seq)
        assertEquals(listOf(p0, p1), repo.points(track.id))
    }

    @Test
    fun finishRecordsSummaryAndMarksFinished() {
        val repo = newRepository()
        val track = repo.start("Test")
        val finished = repo.finish(track.id, distanceM = 5_200.0, elevationGainM = 340.0, elevationLossM = 120.0)
        assertTrue(finished.isFinished)
        assertEquals(5_200.0, finished.distanceM)
        assertEquals(340.0, finished.elevationGainM)
        assertEquals(120.0, finished.elevationLossM)
    }

    @Test
    fun renameUpdatesNameOnlyAndLeavesOtherFieldsAlone() {
        val repo = newRepository()
        val track = repo.start("Old name")
        val renamed = repo.rename(track.id, "New name")
        assertEquals("New name", renamed.name)
        assertEquals(track.startedAtEpochMs, renamed.startedAtEpochMs)
        assertEquals(track.id, renamed.id)
    }

    @Test
    fun deleteIsSoftAndHidesFromListButNotFromGet() {
        val repo = newRepository()
        val track = repo.start("Gone soon")
        repo.delete(track.id)

        assertTrue(repo.list().none { it.id == track.id }, "soft-deleted track should not appear in list()")
        val stillFetchable = repo.get(track.id)
        assertNotNull(stillFetchable, "soft-deleted track should still be fetchable by id")
        assertNotNull(stillFetchable.deletedAtEpochMs)
    }
}
