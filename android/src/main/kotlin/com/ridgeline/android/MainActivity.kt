package com.ridgeline.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ridgeline.engine.GeoPoint
import org.maplibre.android.MapLibre

private val KAUSANI = GeoPoint(latDeg = 29.8422, lonDeg = 79.6006) // Uttarakhand, same viewpoint :engine's tests use

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Must run once before any MapView is created. Belongs in a real
        // Application subclass once this app has one; there isn't one yet.
        MapLibre.getInstance(this)

        setContent {
            RidgelineDemoScreen()
        }
    }
}

/**
 * Two states: not recording shows the original build/run check (CameraX
 * full-bleed + a small corner map, both proving the native islands from
 * CLAUDE.md work -- see DECISIONS.md, "Camera preview and map view
 * actuals"), with the "+ Start a hike"/"+ Start a run" buttons docked at
 * the bottom. Recording replaces that with the real feature: a full-bleed
 * live [RidgelineMapView] drawing the actual recorded route, with
 * [RecordingScreen]'s stat sheet on top -- see DECISIONS.md, "Start Hike /
 * Start Running: live map + real UI".
 */
@Composable
private fun RidgelineDemoScreen() {
    var hasCameraPermission by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val recordingController = rememberRecordingController()
    val liveData by RecordingService.liveData.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            val recording = liveData
            if (recording != null) {
                RidgelineMapView(
                    center = recording.routePoints.firstOrNull() ?: KAUSANI,
                    zoom = 15.0,
                    routePoints = recording.routePoints,
                    currentPosition = recording.routePoints.lastOrNull(),
                    followCurrentPosition = true,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (hasCameraPermission) {
                CameraPreview(modifier = Modifier.fillMaxSize())
            } else {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Camera permission is required to preview the skyline.",
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }

            if (liveData == null) {
                RidgelineMapView(
                    center = KAUSANI,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(width = 160.dp, height = 200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }

            RecordingScreen(
                controller = recordingController,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
