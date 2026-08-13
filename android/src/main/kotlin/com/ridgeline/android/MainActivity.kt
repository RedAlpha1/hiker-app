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
 * Minimal proof that the two native islands from CLAUDE.md -- camera preview
 * and map -- both work: CameraX full-bleed background, a MapLibre map
 * centered on Kausani (the same real Garhwal viewpoint :engine's own tests
 * use) in a corner card. This is a build/run check, not the real AR
 * viewfinder screen -- see :ui's ArViewfinderState for that screen's actual
 * design (not wired up here; see DECISIONS.md, "Camera preview and map view
 * actuals" for why :ui and :android aren't connected yet).
 */
@Composable
private fun RidgelineDemoScreen() {
    var hasCameraPermission by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
            if (hasCameraPermission) {
                CameraPreview(modifier = Modifier.fillMaxSize())
            } else {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Camera permission is required to preview the skyline.",
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }

            RidgelineMapView(
                center = GeoPoint(latDeg = 29.8422, lonDeg = 79.6006), // Kausani, Uttarakhand
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(width = 160.dp, height = 200.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )

            RecordingHost(modifier = Modifier.align(Alignment.BottomStart))
        }
    }
}
