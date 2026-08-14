package com.ridgeline.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ridgeline.engine.GeoPoint
import com.ridgeline.ui.theme.Colors
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private const val ROUTE_SOURCE_ID = "ridgeline-route-source"
private const val ROUTE_LAYER_ID = "ridgeline-route-layer"
private const val MARKERS_SOURCE_ID = "ridgeline-markers-source"
private const val MARKERS_LAYER_ID = "ridgeline-markers-layer"
private const val MARKER_COLOR_PROPERTY = "markerColor"

/**
 * The map -- the other native island from CLAUDE.md, hosted via AndroidView
 * for the same reason as [CameraPreview]. `MapLibre.getInstance(context)`
 * must run once before any MapView is created -- see MainActivity's
 * Application-level init (or add a real Application subclass and call it
 * from onCreate there).
 *
 * `styleUrl` defaults to MapLibre's public demo style so this compiles and
 * shows *something* -- CLAUDE.md's DEM/terrain tile pipeline (region
 * download) is still an open question (DECISIONS.md), so there's no real
 * Ridgeline style to point at yet.
 *
 * [routePoints]/[currentPosition] draw the actual recorded track -- real
 * data from [RecordingService]/[RecordingLiveData], not the design mockup's
 * illustrated route (see DECISIONS.md, "Start Hike / Start Running: live
 * map + real UI" for why that background is deliberately not reproduced).
 * Route line and start/current markers are added once the style finishes
 * loading, then updated in place via [GeoJsonSource.setGeoJson] as new
 * points arrive -- the standard MapLibre pattern for a live-updating line,
 * cheaper than tearing down and re-adding the layer on every fix.
 */
@Composable
fun RidgelineMapView(
    modifier: Modifier = Modifier,
    center: GeoPoint,
    zoom: Double = 11.0,
    styleUrl: String = "https://demotiles.maplibre.org/style.json",
    routePoints: List<GeoPoint> = emptyList(),
    currentPosition: GeoPoint? = null,
    followCurrentPosition: Boolean = false,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var map by remember { mutableStateOf<MapLibreMap?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val mapView = MapView(ctx)
            mapView.getMapAsync { loadedMap ->
                map = loadedMap
                loadedMap.setStyle(styleUrl) { style ->
                    style.addSource(GeoJsonSource(ROUTE_SOURCE_ID))
                    style.addLayer(
                        LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                            PropertyFactory.lineColor(Colors.PRIMARY_BLUE.toInt()),
                            PropertyFactory.lineWidth(4f),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                        ),
                    )
                    style.addSource(GeoJsonSource(MARKERS_SOURCE_ID))
                    style.addLayer(
                        CircleLayer(MARKERS_LAYER_ID, MARKERS_SOURCE_ID).withProperties(
                            PropertyFactory.circleColor(Expression.get(MARKER_COLOR_PROPERTY)),
                            PropertyFactory.circleRadius(6f),
                            PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE),
                            PropertyFactory.circleStrokeWidth(2f),
                        ),
                    )
                }
                loadedMap.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(center.latDeg, center.lonDeg))
                    .zoom(zoom)
                    .build()
            }

            // MapLibre's MapView (like the Mapbox GL view it's forked from)
            // needs its lifecycle callbacks forwarded manually when it's not
            // hosted by a Fragment/Activity that does this for you -- the
            // standard pattern for embedding it via AndroidView.
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            mapView.onCreate(null)
            mapView.onStart()
            mapView.onResume()

            mapView
        },
    )

    LaunchedEffect(routePoints, currentPosition) {
        val loadedMap = map ?: return@LaunchedEffect
        loadedMap.getStyle { style ->
            if (routePoints.size >= 2) {
                val line = LineString.fromLngLats(routePoints.map { Point.fromLngLat(it.lonDeg, it.latDeg) })
                style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)?.setGeoJson(line)
            }

            val markers = buildList {
                routePoints.firstOrNull()?.let { start ->
                    add(startMarkerFeature(start))
                }
                currentPosition?.let { add(currentPositionMarkerFeature(it)) }
            }
            style.getSourceAs<GeoJsonSource>(MARKERS_SOURCE_ID)?.setGeoJson(FeatureCollection.fromFeatures(markers))

            if (followCurrentPosition && currentPosition != null) {
                loadedMap.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(currentPosition.latDeg, currentPosition.lonDeg))
                    .build()
            }
        }
    }
}

// Hex literals here match ui/theme/Colors.kt's SUCCESS_GREEN/PRIMARY_BLUE
// (0xFF4F6B3F / 0xFF1D5FFF) -- kept as plain strings rather than derived
// from the ARGB Long via bit manipulation, since MapLibre's style
// expressions want a CSS-style hex string in the feature property, not an
// Android color Int.
private fun startMarkerFeature(point: GeoPoint): Feature =
    Feature.fromGeometry(Point.fromLngLat(point.lonDeg, point.latDeg)).apply {
        addStringProperty(MARKER_COLOR_PROPERTY, "#4f6b3f")
    }

private fun currentPositionMarkerFeature(point: GeoPoint): Feature =
    Feature.fromGeometry(Point.fromLngLat(point.lonDeg, point.latDeg)).apply {
        addStringProperty(MARKER_COLOR_PROPERTY, "#1d5fff")
    }
