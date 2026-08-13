package com.ridgeline.android

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ridgeline.engine.GeoPoint
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

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
 */
@Composable
fun RidgelineMapView(
    modifier: Modifier = Modifier,
    center: GeoPoint,
    zoom: Double = 11.0,
    styleUrl: String = "https://demotiles.maplibre.org/style.json",
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val mapView = MapView(ctx)
            mapView.getMapAsync { map ->
                map.setStyle(styleUrl)
                map.cameraPosition = CameraPosition.Builder()
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
}
