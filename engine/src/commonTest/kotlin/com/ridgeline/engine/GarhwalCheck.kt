package com.ridgeline.engine

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * `engine/test/GarhwalCheck.kt` per CLAUDE.md — anchored to the real Kausani viewpoint
 * and the distances DECISIONS.md records for it ("Region scope: Uttarakhand and Himachal
 * only, Indian users"). These are the numbers that pushed `maxRangeM` from 60km to 150km
 * and made curvature correction load-bearing; keep this green on any geodesy change.
 */
class GarhwalCheck {

    private val kausani = Viewpoint(location = GeoPoint(29.8422, 79.6006), elevationM = 1890.0)

    private val nandaDevi = Peak("nanda-devi", "Nanda Devi", location = GeoPoint(30.3753, 79.9707), elevationM = 7816.0)
    private val kamet = Peak("kamet", "Kamet", location = GeoPoint(30.9235, 79.5920), elevationM = 7756.0)
    private val shivling = Peak("shivling", "Shivling", location = GeoPoint(30.8817, 79.0672), elevationM = 6543.0)
    private val chaukhamba = Peak(
        id = "chaukhamba",
        name = "Chaukhamba",
        nameLocal = "चौखम्बा",
        aliases = listOf("Chaukhambha"),
        location = GeoPoint(30.7300, 79.3600),
        elevationM = 7138.0,
    )

    @Test
    fun distancesMatchTheDecisionLog() {
        // DECISIONS.md: "Nanda Devi is 69km out, Kamet 120km, Shivling 126km".
        assertApprox(69_000.0, distanceM(kausani.location, nandaDevi.location), tolerance = 2_000.0)
        assertApprox(120_000.0, distanceM(kausani.location, kamet.location), tolerance = 2_000.0)
        assertApprox(126_000.0, distanceM(kausani.location, shivling.location), tolerance = 2_000.0)
    }

    @Test
    fun sixtyKilometreCutoffWouldDiscardMostOfTheSkyline() {
        // The claim that motivated raising maxRangeM from 60km to 150km (CLAUDE.md,
        // "Before you change anything").
        val alpineRange = VisibilityConfig(maxRangeM = 60_000.0)
        val himalayanRange = VisibilityConfig(maxRangeM = 150_000.0)
        val peaks = listOf(nandaDevi, kamet, shivling, chaukhamba)

        val visibleAt60 = resolveVisiblePeaks(kausani, peaks, FlatElevationSource(), alpineRange)
        val visibleAt150 = resolveVisiblePeaks(kausani, peaks, FlatElevationSource(), himalayanRange)

        assertTrue(
            visibleAt60.size <= 1,
            "60km cutoff should keep at most one of these four summits, kept ${visibleAt60.map { it.peak.name }}",
        )
        assertTrue(visibleAt150.size == peaks.size, "150km range should keep all four summits")
    }

    @Test
    fun curvatureDropAtShivlingRangeMatchesTheDecisionLog() {
        // DECISIONS.md: "At 126km the earth drops the target 1,084m ... misplaces the
        // label by ~0.5°."
        val distance = distanceM(kausani.location, shivling.location)
        assertApprox(1_084.0, earthCurvatureDropM(distance), tolerance = 30.0)
        assertApprox(0.5, curvatureAngleErrorDeg(distance), tolerance = 0.05)
    }

    @Test
    fun chaukhambaMatchesOnAnyTransliteration() {
        // CLAUDE.md: "Chaukhamba/Chaukhambha ... so Peak carries nameLocal and aliases.
        // Match on any variant."
        assertTrue(chaukhamba.matches("Chaukhamba"))
        assertTrue(chaukhamba.matches("chaukhambha"))
        assertTrue(chaukhamba.matches("चौखम्बा"))
        assertTrue(!chaukhamba.matches("Trisul"))
    }
}
