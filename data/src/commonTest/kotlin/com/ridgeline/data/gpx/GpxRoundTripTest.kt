package com.ridgeline.data.gpx

import com.ridgeline.data.TrackPoint
import com.ridgeline.engine.GeoPoint
import kotlin.test.Test
import kotlin.test.assertEquals

class GpxRoundTripTest {

    @Test
    fun writeThenParseRecoversPointsAndName() {
        val points = listOf(
            TrackPoint("track-1", 0, GeoPoint(29.8422, 79.6006), 1890.0, 1_700_000_000_000L),
            TrackPoint("track-1", 1, GeoPoint(29.8430, 79.6010), 1895.5, 1_700_000_060_000L),
        )
        val xml = GpxWriter.write("Kausani ridge walk", points)
        val result = GpxParser.parse(xml)

        assertEquals("Kausani ridge walk", result.name)
        assertEquals(points.size, result.points.size)
        points.zip(result.points).forEach { (expected, actual) ->
            assertEquals(expected.location.latDeg, actual.location.latDeg, 1e-9)
            assertEquals(expected.location.lonDeg, actual.location.lonDeg, 1e-9)
            assertEquals(expected.elevationM, actual.elevationM)
            assertEquals(expected.recordedAtEpochMs, actual.timeEpochMs)
        }
    }

    @Test
    fun parseToleratesUnknownElementsAndNamespacedExtensions() {
        val xml = """
            <?xml version="1.0"?>
            <gpx xmlns:gpxtpx="http://example.com/ext" version="1.1">
              <trk>
                <name>Imported</name>
                <trkseg>
                  <trkpt lat="30.1" lon="79.2">
                    <ele>4200</ele>
                    <time>2026-01-01T00:00:00Z</time>
                    <extensions><gpxtpx:hr>140</gpxtpx:hr></extensions>
                  </trkpt>
                </trkseg>
              </trk>
            </gpx>
        """.trimIndent()

        val result = GpxParser.parse(xml)
        assertEquals("Imported", result.name)
        assertEquals(1, result.points.size)
        assertEquals(30.1, result.points[0].location.latDeg)
        assertEquals(79.2, result.points[0].location.lonDeg)
        assertEquals(4200.0, result.points[0].elevationM)
    }

    @Test
    fun parseHandlesMissingElevationAndTime() {
        val xml = """<gpx><trk><trkseg><trkpt lat="30.0" lon="79.0"/></trkseg></trk></gpx>"""
        val result = GpxParser.parse(xml)
        assertEquals(1, result.points.size)
        assertEquals(null, result.points[0].elevationM)
        assertEquals(null, result.points[0].timeEpochMs)
    }
}
