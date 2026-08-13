package com.ridgeline.ui.screens

import com.ridgeline.data.Track
import com.ridgeline.engine.Peak

/**
 * A peak identified at some point during a track. UI-facing only for now --
 * there's no `:data` persistence for this yet (see DECISIONS.md, ":ui
 * screen state from the design file"). Reuses `:engine`'s `Peak` rather than
 * a third peak shape.
 */
data class PeakSighting(val peak: Peak, val seenAtEpochMs: Long, val distanceAtSightingM: Double)

enum class WeatherLayer { TEMPERATURE, RAIN, SNOW, WIND, CLOUDS }

data class SummaryState(
    val track: Track,
    val locationLabel: String,
    val sightings: List<PeakSighting> = emptyList(),
    val activeWeatherLayer: WeatherLayer? = null,
) {
    fun toggleLayer(layer: WeatherLayer): SummaryState =
        copy(activeWeatherLayer = if (activeWeatherLayer == layer) null else layer)
}
