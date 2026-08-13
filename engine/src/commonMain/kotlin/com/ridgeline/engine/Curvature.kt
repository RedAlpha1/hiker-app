package com.ridgeline.engine

import kotlin.math.atan
import kotlin.math.pow

/**
 * Standard terrestrial refraction coefficient. Bends the line of sight enough
 * to partly compensate earth curvature; 0.13 is the conventional value for
 * mid-latitude daytime conditions and is what surveying/amateur-astronomy
 * "dip" tables use.
 */
private const val REFRACTION_COEFFICIENT: Double = 0.13

private val EFFECTIVE_EARTH_RADIUS_M: Double = EARTH_RADIUS_M / (1 - REFRACTION_COEFFICIENT)

/**
 * How far the target drops below the tangent line-of-sight due to earth
 * curvature, net of atmospheric refraction, at [distanceM]. Not optional at
 * Himalayan viewpoint ranges: this is ~1084m at 126km (Kausani -> Shivling).
 * See CLAUDE.md, "Before you change anything".
 */
fun earthCurvatureDropM(distanceM: Double): Double =
    distanceM.pow(2) / (2 * EFFECTIVE_EARTH_RADIUS_M)

/**
 * The elevation-angle error, in degrees, from ignoring [earthCurvatureDropM]
 * at [distanceM]. Small at short range, ~0.5° at 126km — enough to misplace
 * a label relative to its neighbours in a 4.9°-12.7° elevation-angle band.
 */
fun curvatureAngleErrorDeg(distanceM: Double): Double =
    atan(earthCurvatureDropM(distanceM) / distanceM).toDegrees()
