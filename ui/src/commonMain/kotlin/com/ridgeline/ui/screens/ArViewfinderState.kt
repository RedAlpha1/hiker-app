package com.ridgeline.ui.screens

import com.ridgeline.engine.PlacedLabel
import com.ridgeline.engine.VisiblePeak

/**
 * `LIVE`: normal operation, camera feed + peak overlay. `COMPASS_ONLY`:
 * heading is known but nothing else is confidently rendered (e.g. attitude
 * not yet stable -- see CLAUDE.md's open question on compass accuracy).
 * `NO_ELEVATION_DATA`: outside a downloaded region, or DEM coverage is
 * missing -- no peak ID is possible, so [peakCount] reads zero regardless of
 * [labels].
 */
enum class ArMode { LIVE, COMPASS_ONLY, NO_ELEVATION_DATA }

/**
 * [labels] is `:engine`'s own `layoutLabels()` output -- this state holds
 * whatever the engine last resolved for the current position, re-projected
 * per frame. See CLAUDE.md, "Engine performance contract": [labels] changes
 * on position change; only [headingDeg] and [headingNudgeDeg] change per
 * frame.
 */
data class ArViewfinderState(
    val mode: ArMode = ArMode.LIVE,
    val headingDeg: Double = 0.0,
    /** Manual compass calibration offset the user can dial in, degrees. */
    val headingNudgeDeg: Int = 0,
    val labels: List<PlacedLabel> = emptyList(),
    val selectedPeak: VisiblePeak? = null,
) {
    val effectiveHeadingDeg: Double get() = headingDeg + headingNudgeDeg
    val peakCount: Int get() = if (mode == ArMode.NO_ELEVATION_DATA) 0 else labels.size
    val hasSelectedPeak: Boolean get() = selectedPeak != null

    fun select(peak: VisiblePeak): ArViewfinderState = copy(selectedPeak = peak)
    fun deselect(): ArViewfinderState = copy(selectedPeak = null)
    fun nudge(deltaDeg: Int): ArViewfinderState = copy(headingNudgeDeg = headingNudgeDeg + deltaDeg)
}
