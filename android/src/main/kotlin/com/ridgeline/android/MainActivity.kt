package com.ridgeline.android

import android.Manifest
import android.content.Context
import android.content.pm.ActivityInfo
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
import androidx.compose.runtime.DisposableEffect
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
import com.ridgeline.ui.screens.OnboardingState
import org.maplibre.android.MapLibre

private const val PREFS_NAME = "ridgeline_prefs"
private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Must run once before any MapView is created. Belongs in a real
        // Application subclass once this app has one; there isn't one yet.
        MapLibre.getInstance(this)

        setContent {
            RidgelineApp()
        }
    }
}

private sealed interface AppRoute {
    data object Splash : AppRoute
    data object Onboarding : AppRoute
    data object Home : AppRoute
}

/**
 * Splash -> Onboarding (first launch only) -> Home. "Onboarding complete" is
 * a bare SharedPreferences flag, not a :data repository -- it's app-launch
 * state, not a trail/peak/region record, so it doesn't belong in the
 * SQLDelight schema those repositories front.
 */
@Composable
private fun RidgelineApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var route by remember { mutableStateOf<AppRoute>(AppRoute.Splash) }
    var onboardingState by remember { mutableStateOf(OnboardingState()) }

    MaterialTheme {
        when (route) {
            AppRoute.Splash -> SplashScreen(
                onFinished = {
                    route = if (prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)) {
                        AppRoute.Home
                    } else {
                        AppRoute.Onboarding
                    }
                },
            )

            AppRoute.Onboarding -> OnboardingScreen(
                state = onboardingState,
                onNext = { onboardingState = onboardingState.next() },
                onSkip = { onboardingState = onboardingState.skip() },
                onGetStarted = {
                    prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply()
                    route = AppRoute.Home
                },
            )

            AppRoute.Home -> RidgelineDemoScreen()
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
 *
 * Forced landscape here, but only here: the AR viewfinder this screen stands
 * in for defaults to landscape (DECISIONS.md, "AR viewfinder defaults to
 * landscape"), while Splash/Onboarding/Library stay portrait. A single
 * Activity can't declare per-screen orientation in the manifest, so this
 * locks on entry and restores "unspecified" on exit rather than the whole
 * app being forced landscape from the manifest, as it was before this
 * flow existed. Relies on this activity's android:configChanges
 * (orientation|screenSize|...) so flipping requestedOrientation doesn't
 * recreate the Activity and reset RidgelineApp's route/onboarding state.
 */
@Composable
private fun RidgelineDemoScreen() {
    var hasCameraPermission by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

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
    }
}
