package com.ridgeline.ui.theme

/**
 * Colors as ARGB `Long` (0xAARRGGBB) rather than a Compose `Color` -- :ui has
 * no Compose dependency yet (see CLAUDE.md architecture: this module is
 * still just screen state). Convert at the call site once composables land.
 * Values transcribed from Ridgeline_Standalone.html's "Tokens & shared
 * components" panel -- see DECISIONS.md, ":ui screen state from the design
 * file": visual/state shape only, never coordinates or bearings from that
 * file.
 */
object Colors {
    const val GROUND = 0xFFF0F0F5
    const val RAISED = 0xFFFFFFFF
    const val DIVIDER = 0xFFDCDCE3
    const val INK = 0xFF1C1A15
    const val MUTED = 0xFF6B6454

    const val PRIMARY_BLUE = 0xFF1D5FFF
    const val ROUTE_BLUE = 0xFF295F86
    const val SUCCESS_GREEN = 0xFF4F6B3F

    /**
     * Dot-only marker color for peaks below the naming threshold -- the
     * design's two-tier chip/dot system maps directly onto :engine's
     * `PlacedLabel.isDot` (itself driven by `Confidence.MARGINAL` or a
     * lane-exhaustion demotion).
     */
    const val MARGINAL_DOT = 0xFFD0692F

    /** Hypsometric terrain ramp, low to high. Normalized per-trail, always shown with a legend. */
    val HYPSOMETRIC_RAMP: List<Long> = listOf(0xFFD9CE8A, 0xFFC89A6A, 0xFFA97A52, 0xFF9E9086, 0xFFE9E9E6)

    /** Top/bottom stops for a vertical gradient. */
    data class GradientStops(val top: Long, val bottom: Long)

    /**
     * AR viewfinder background fallback for when there's no live camera feed
     * to overlay (e.g. a compass-only preview). The shipping AR screen
     * overlays live video, not one of these -- see the design brief's
     * "Palette" note.
     */
    object SkyFallback {
        val DUSK = GradientStops(top = 0xFF20211D, bottom = 0xFF4A2C18)
        val NIGHT = GradientStops(top = 0xFF050505, bottom = 0xFF20211D)
        val DAY = GradientStops(top = 0xFF4A4B42, bottom = 0xFF6B6C60)
    }
}
