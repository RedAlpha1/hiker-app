package com.ridgeline.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryStateTest {

    @Test
    fun fractionIsZeroWhenTotalIsUnknown() {
        val progress = RegionDownloadProgress("Garhwal", downloadedBytes = 42, totalBytes = 0)
        assertEquals(0f, progress.fraction)
    }

    @Test
    fun fractionIsClampedToOneEvenIfDownloadedExceedsTotal() {
        val progress = RegionDownloadProgress("Garhwal", downloadedBytes = 120, totalBytes = 100)
        assertEquals(1f, progress.fraction)
    }

    @Test
    fun fractionIsTheSimpleRatioInTheNormalCase() {
        val progress = RegionDownloadProgress("Garhwal", downloadedBytes = 25, totalBytes = 100)
        assertEquals(0.25f, progress.fraction)
    }
}
