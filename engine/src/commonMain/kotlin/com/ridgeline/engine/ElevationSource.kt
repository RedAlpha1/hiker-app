package com.ridgeline.engine

/**
 * Abstraction over the DEM. Production implementations read Copernicus
 * GLO-30 tiles (~2-4m RMSE in mountainous terrain — see [VisibilityConfig.terrainToleranceM]);
 * this module never touches tile storage or platform file APIs directly, so
 * it stays pure Kotlin and JVM-testable.
 */
fun interface ElevationSource {
    /** Ground elevation in metres at [point]. */
    fun elevationAt(point: GeoPoint): Double
}
