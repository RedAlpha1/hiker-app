package com.ridgeline.ui.screens

import com.ridgeline.data.Track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SummaryStateTest {

    private val track = Track(
        id = "track-1",
        name = "Kausani ridge walk",
        startedAtEpochMs = 0L,
        endedAtEpochMs = 3_600_000L,
        distanceM = 8_000.0,
        elevationGainM = 450.0,
        elevationLossM = 300.0,
        createdAtEpochMs = 0L,
        updatedAtEpochMs = 0L,
    )

    @Test
    fun togglingTheSameLayerTwiceClearsIt() {
        var state = SummaryState(track, locationLabel = "Kausani, Uttarakhand")
        state = state.toggleLayer(WeatherLayer.SNOW)
        assertEquals(WeatherLayer.SNOW, state.activeWeatherLayer)
        state = state.toggleLayer(WeatherLayer.SNOW)
        assertNull(state.activeWeatherLayer)
    }

    @Test
    fun togglingADifferentLayerSwitchesTheActiveOne() {
        var state = SummaryState(track, locationLabel = "Kausani, Uttarakhand")
        state = state.toggleLayer(WeatherLayer.RAIN)
        state = state.toggleLayer(WeatherLayer.WIND)
        assertEquals(WeatherLayer.WIND, state.activeWeatherLayer)
    }
}
