package com.ridgeline.ui.theme

/** Maps to CSS font-weight values, matching the design file's Inter weights. */
enum class TextWeight(val cssValue: Int) {
    MEDIUM(500),
    SEMIBOLD(600),
    BOLD(700),
    EXTRABOLD(800),
}

data class TextStyleSpec(
    val sizeSp: Int,
    val weight: TextWeight,
    /** Tabular-nums treatment used throughout for stat readouts (distances, elevations, times). */
    val monospacedNumerals: Boolean = false,
    val uppercase: Boolean = false,
    val letterSpacingEm: Double = 0.0,
)

/** Type scale transcribed from the design file. Inter throughout. */
object Typography {
    /** The big distance-so-far readout on the recording sheet. */
    val statHero = TextStyleSpec(sizeSp = 52, weight = TextWeight.EXTRABOLD, monospacedNumerals = true)

    /** Summary screen stat grid ("11.4 km", "1,340 m"). */
    val statLarge = TextStyleSpec(sizeSp = 22, weight = TextWeight.BOLD, monospacedNumerals = true)

    /** Recording sheet's gain/duration/pace trio. */
    val statMedium = TextStyleSpec(sizeSp = 17, weight = TextWeight.BOLD, monospacedNumerals = true)

    /** Library "Your hikes", summary distance/elevation headline numbers. */
    val pageTitle = TextStyleSpec(sizeSp = 26, weight = TextWeight.EXTRABOLD)

    val onboardingTitle = TextStyleSpec(sizeSp = 24, weight = TextWeight.EXTRABOLD)
    val sectionTitle = TextStyleSpec(sizeSp = 18, weight = TextWeight.EXTRABOLD)

    /** Peak name in an AR chip, hike title on a library card. */
    val peakName = TextStyleSpec(sizeSp = 14, weight = TextWeight.EXTRABOLD)

    val body = TextStyleSpec(sizeSp = 14, weight = TextWeight.MEDIUM)

    /** Meta text: dates, GPS status, coordinates -- monospace, small. */
    val caption = TextStyleSpec(sizeSp = 11, weight = TextWeight.SEMIBOLD, monospacedNumerals = true)

    /** Uppercase section eyebrows: "DISTANCE", "OFFLINE REGIONS". */
    val eyebrow = TextStyleSpec(sizeSp = 11, weight = TextWeight.BOLD, uppercase = true, letterSpacingEm = 0.1)

    val button = TextStyleSpec(sizeSp = 14, weight = TextWeight.EXTRABOLD)
}
