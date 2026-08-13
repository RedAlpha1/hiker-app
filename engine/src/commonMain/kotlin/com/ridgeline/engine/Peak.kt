package com.ridgeline.engine

/**
 * A named summit. Hand-curated, not OSM-derived — coverage of the Indian
 * Himalaya is thin and transliteration is not standardised (e.g.
 * Chaukhamba/Chaukhambha, Trisul/Trishul), so callers should match on
 * [name], [nameLocal], or any [aliases] entry rather than assuming [name]
 * is canonical.
 */
data class Peak(
    val id: String,
    val name: String,
    val nameLocal: String? = null,
    val aliases: List<String> = emptyList(),
    val location: GeoPoint,
    val elevationM: Double,
) {
    /** True if [query] matches this peak's name, local name, or any alias, case-insensitively. */
    fun matches(query: String): Boolean {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return false
        return name.lowercase() == needle ||
            nameLocal?.lowercase() == needle ||
            aliases.any { it.lowercase() == needle }
    }
}

/** Where the user is standing: position, ground elevation, and eye height above it. */
data class Viewpoint(
    val location: GeoPoint,
    val elevationM: Double,
    val eyeHeightM: Double = 1.6,
) {
    val eyeElevationM: Double get() = elevationM + eyeHeightM
}
