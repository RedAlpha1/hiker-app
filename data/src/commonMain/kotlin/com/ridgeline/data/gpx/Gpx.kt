package com.ridgeline.data.gpx

import com.ridgeline.data.TrackPoint
import com.ridgeline.engine.GeoPoint
import kotlinx.datetime.Instant

/** One trackpoint as parsed from a GPX file, before it's assigned to a [com.ridgeline.data.Track]. */
data class GpxTrackPoint(
    val location: GeoPoint,
    val elevationM: Double?,
    val timeEpochMs: Long?,
)

data class GpxImportResult(
    val name: String?,
    val points: List<GpxTrackPoint>,
)

/**
 * Writes a minimal, valid GPX 1.1 `<trk>` -- just what Ridgeline itself
 * records (lat/lon/ele/time per point). Hand-rolled rather than pulling in a
 * multiplatform XML library: the output shape is small and fixed, so a
 * string template is simpler and has no new dependency to keep compiling on
 * every target (see [GpxParser] for the matching trade-off on the read side).
 */
object GpxWriter {
    fun write(name: String, points: List<TrackPoint>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<gpx version=\"1.1\" creator=\"Ridgeline\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        append("  <trk>\n")
        append("    <name>").append(escape(name)).append("</name>\n")
        append("    <trkseg>\n")
        points.forEach { point ->
            append("      <trkpt lat=\"").append(point.location.latDeg)
                .append("\" lon=\"").append(point.location.lonDeg).append("\">\n")
            append("        <ele>").append(point.elevationM).append("</ele>\n")
            append("        <time>").append(Instant.fromEpochMilliseconds(point.recordedAtEpochMs)).append("</time>\n")
            append("      </trkpt>\n")
        }
        append("    </trkseg>\n")
        append("  </trk>\n")
        append("</gpx>\n")
    }

    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}

/**
 * Parses `<trkpt>` elements out of a GPX file with targeted regexes rather
 * than a full XML parser -- real-world exports (Strava, Garmin, OsmAnd, our
 * own [GpxWriter]) all use straightforward double-quoted attributes and
 * unnested elements, and this avoids a multiplatform XML dependency for what
 * v1 needs. Known limitation: no CDATA support, and a `<trkpt>` embedded
 * inside a comment or CDATA block would be misread. Tolerant of unknown
 * child elements/namespaces -- it only looks for what it needs.
 */
object GpxParser {
    // [\s\S] instead of "." + RegexOption.DOT_MATCHES_ALL: that option is JVM-only
    // in kotlin.text (not part of the common API), so it won't compile for iOS.
    // [\s\S] matches any character including newlines on every Kotlin target.
    private val nameRegex = Regex("<name>([\\s\\S]*?)</name>")
    private val trkptRegex = Regex("<trkpt\\b([^>]*?)(?:/>|>([\\s\\S]*?)</trkpt>)")
    private val latRegex = Regex("""lat\s*=\s*"([^"]+)"""")
    private val lonRegex = Regex("""lon\s*=\s*"([^"]+)"""")
    private val eleRegex = Regex("<ele>([\\s\\S]*?)</ele>")
    private val timeRegex = Regex("<time>([\\s\\S]*?)</time>")

    fun parse(xml: String): GpxImportResult {
        val name = nameRegex.find(xml)?.groupValues?.get(1)?.let { unescape(it.trim()) }?.takeIf { it.isNotEmpty() }

        val points = trkptRegex.findAll(xml).mapNotNull { match ->
            val attributes = match.groupValues[1]
            val body = match.groupValues.getOrElse(2) { "" }
            val lat = latRegex.find(attributes)?.groupValues?.get(1)?.toDoubleOrNull()
            val lon = lonRegex.find(attributes)?.groupValues?.get(1)?.toDoubleOrNull()
            if (lat == null || lon == null) return@mapNotNull null

            val elevation = eleRegex.find(body)?.groupValues?.get(1)?.trim()?.toDoubleOrNull()
            val timeEpochMs = timeRegex.find(body)?.groupValues?.get(1)?.trim()
                ?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() }

            GpxTrackPoint(GeoPoint(lat, lon), elevation, timeEpochMs)
        }.toList()

        return GpxImportResult(name, points)
    }

    private fun unescape(text: String): String = text
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&")
}
