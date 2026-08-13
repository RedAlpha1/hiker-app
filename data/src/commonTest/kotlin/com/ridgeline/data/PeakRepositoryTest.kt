package com.ridgeline.data

import com.ridgeline.data.db.DatabaseDriverFactory
import com.ridgeline.data.db.createDatabase
import com.ridgeline.engine.GeoPoint
import com.ridgeline.engine.Peak
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PeakRepositoryTest {

    private fun newRepository(): PeakRepository {
        val db = createDatabase(DatabaseDriverFactory())
        return SqlDelightPeakRepository(db)
    }

    // Real coordinates, same as GarhwalCheck.kt in :engine -- see CLAUDE.md,
    // "Chaukhamba/Chaukhambha ... so Peak carries nameLocal and aliases."
    private val chaukhamba = Peak(
        id = "chaukhamba",
        name = "Chaukhamba",
        nameLocal = "चौखम्बा",
        aliases = listOf("Chaukhambha"),
        location = GeoPoint(30.7300, 79.3600),
        elevationM = 7138.0,
    )
    private val nandaDevi = Peak(
        id = "nanda-devi",
        name = "Nanda Devi",
        location = GeoPoint(30.3753, 79.9707),
        elevationM = 7816.0,
    )

    @Test
    fun replaceAndFetchRoundTripsPeaksExactly() {
        val repo = newRepository()
        repo.replaceRegionPeaks("garhwal", listOf(chaukhamba, nandaDevi))
        val fetched = repo.peaksInRegion("garhwal").associateBy { it.id }

        assertEquals(chaukhamba, fetched.getValue("chaukhamba"))
        assertEquals(nandaDevi, fetched.getValue("nanda-devi"))
        assertTrue(fetched.getValue("nanda-devi").aliases.isEmpty(), "a peak with no aliases should round-trip to an empty list, not [\"\"]")
    }

    @Test
    fun replaceRegionPeaksIsWholesaleNotAdditive() {
        val repo = newRepository()
        repo.replaceRegionPeaks("garhwal", listOf(chaukhamba))
        repo.replaceRegionPeaks("garhwal", listOf(nandaDevi))
        assertEquals(listOf(nandaDevi), repo.peaksInRegion("garhwal"))
    }

    @Test
    fun differentRegionsAreIndependent() {
        val repo = newRepository()
        repo.replaceRegionPeaks("garhwal", listOf(nandaDevi))
        repo.replaceRegionPeaks("himachal", listOf(chaukhamba))
        assertEquals(listOf(nandaDevi), repo.peaksInRegion("garhwal"))
        assertEquals(listOf(chaukhamba), repo.peaksInRegion("himachal"))
    }
}
