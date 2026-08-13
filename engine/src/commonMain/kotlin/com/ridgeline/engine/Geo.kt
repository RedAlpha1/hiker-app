package com.ridgeline.engine

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Mean Earth radius, metres. Good enough at the ranges this engine works at; see [Curvature]. */
const val EARTH_RADIUS_M: Double = 6_371_000.0

data class GeoPoint(val latDeg: Double, val lonDeg: Double)

internal fun Double.toRadians(): Double = this * PI / 180.0
internal fun Double.toDegrees(): Double = this * 180.0 / PI

/** Great-circle distance between two points, metres. Haversine — accurate enough at 150km range. */
fun distanceM(a: GeoPoint, b: GeoPoint): Double {
    val lat1 = a.latDeg.toRadians()
    val lat2 = b.latDeg.toRadians()
    val dLat = (b.latDeg - a.latDeg).toRadians()
    val dLon = (b.lonDeg - a.lonDeg).toRadians()

    val h = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(h), sqrt(1 - h))
    return EARTH_RADIUS_M * c
}

/** Initial bearing from [a] to [b], degrees clockwise from true north, in [0, 360). */
fun bearingDeg(a: GeoPoint, b: GeoPoint): Double {
    val lat1 = a.latDeg.toRadians()
    val lat2 = b.latDeg.toRadians()
    val dLon = (b.lonDeg - a.lonDeg).toRadians()

    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    val theta = atan2(y, x)
    return (theta.toDegrees() + 360.0) % 360.0
}

/** Point at [distanceM] and [bearingDeg] from [origin], along the great circle. */
fun destinationPoint(origin: GeoPoint, distanceM: Double, bearingDeg: Double): GeoPoint {
    val lat1 = origin.latDeg.toRadians()
    val lon1 = origin.lonDeg.toRadians()
    val brng = bearingDeg.toRadians()
    val angularDistance = distanceM / EARTH_RADIUS_M

    val lat2 = kotlin.math.asin(
        sin(lat1) * cos(angularDistance) + cos(lat1) * sin(angularDistance) * cos(brng)
    )
    val lon2 = lon1 + atan2(
        sin(brng) * sin(angularDistance) * cos(lat1),
        cos(angularDistance) - sin(lat1) * sin(lat2)
    )
    return GeoPoint(lat2.toDegrees(), lon2.toDegrees())
}

/** Smallest signed angular difference `a - b`, in degrees, in [-180, 180]. */
fun angleDiffDeg(a: Double, b: Double): Double {
    val diff = (a - b) % 360.0
    return when {
        diff > 180.0 -> diff - 360.0
        diff < -180.0 -> diff + 360.0
        else -> diff
    }
}
