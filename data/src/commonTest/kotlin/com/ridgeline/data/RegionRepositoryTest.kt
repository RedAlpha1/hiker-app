package com.ridgeline.data

import com.ridgeline.data.db.DatabaseDriverFactory
import com.ridgeline.data.db.createDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegionRepositoryTest {

    private fun newRepository(clock: FakeClock = FakeClock()): RegionRepository {
        val db = createDatabase(DatabaseDriverFactory())
        return SqlDelightRegionRepository(db, clock)
    }

    private val garhwalBounds = RegionBounds(southDeg = 29.0, westDeg = 78.5, northDeg = 31.5, eastDeg = 80.5)

    @Test
    fun upsertMetadataCreatesAnUndownloadedRegion() {
        val repo = newRepository()
        repo.upsertMetadata("garhwal", "Garhwal", garhwalBounds, peakDataVersion = "1")
        val region = repo.get("garhwal")!!
        assertFalse(region.isDownloaded)
        assertEquals("Garhwal", region.name)
    }

    @Test
    fun reUpsertingMetadataPreservesDownloadState() {
        val repo = newRepository()
        repo.upsertMetadata("garhwal", "Garhwal", garhwalBounds, peakDataVersion = "1")
        repo.markDownloaded("garhwal", sizeBytes = 42_000_000L)

        // Catalog refresh with a newer peak data version must not undo the download.
        repo.upsertMetadata("garhwal", "Garhwal", garhwalBounds, peakDataVersion = "2")

        val region = repo.get("garhwal")!!
        assertTrue(region.isDownloaded)
        assertEquals(42_000_000L, region.sizeBytes)
        assertEquals("2", region.peakDataVersion)
    }

    @Test
    fun listReturnsAllCatalogedRegions() {
        val repo = newRepository()
        repo.upsertMetadata("garhwal", "Garhwal", garhwalBounds, peakDataVersion = "1")
        repo.upsertMetadata("himachal", "Himachal", garhwalBounds, peakDataVersion = "1")
        assertEquals(setOf("garhwal", "himachal"), repo.list().map { it.id }.toSet())
    }
}
