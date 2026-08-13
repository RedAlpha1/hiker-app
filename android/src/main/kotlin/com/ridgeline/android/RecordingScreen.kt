package com.ridgeline.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ridgeline.data.SqlDelightTrackRepository
import com.ridgeline.data.Track
import com.ridgeline.data.db.DatabaseDriverFactory
import com.ridgeline.data.db.createDatabase
import kotlin.time.Duration.Companion.milliseconds

/**
 * Entry point for "Start a hike" / "Start a run" -- the implementation the
 * design's Library screen buttons didn't have yet. Plain Jetpack Compose
 * driven by [RecordingService], not `:ui`'s RecordingState/ActivityType:
 * `:ui` has no androidTarget() yet (see DECISIONS.md, "Camera preview and
 * map view actuals"), so [ActivityKind] mirrors that shape locally rather
 * than reaching for a cross-module dependency over two enum values.
 *
 * Deliberately Start/End only, no pause -- [com.ridgeline.engine.RecordingSession]
 * already supports pause/resume, but wiring a pause control through this
 * service (which has no other command channel yet) is a real follow-up, not
 * something to half-build into this pass.
 */
@Composable
fun RecordingHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val trackRepository = remember {
        SqlDelightTrackRepository(createDatabase(DatabaseDriverFactory(context.applicationContext)))
    }

    var activeTrack by remember { mutableStateOf<Track?>(null) }
    var pendingKind by remember { mutableStateOf<ActivityKind?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        val kind = pendingKind
        pendingKind = null
        if (kind != null && granted[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            activeTrack = beginRecording(context, trackRepository, kind)
        }
    }

    val update by RecordingService.updates.collectAsState()
    val track = activeTrack

    if (track == null) {
        StartButtons(
            modifier = modifier,
            onStart = { kind ->
                if (hasLocationPermission(context)) {
                    activeTrack = beginRecording(context, trackRepository, kind)
                } else {
                    pendingKind = kind
                    permissionLauncher.launch(requiredPermissions())
                }
            },
        )
    } else {
        RecordingCard(
            modifier = modifier,
            trackName = track.name,
            update = update,
            onEnd = {
                val totals = update
                RecordingService.stop(context)
                trackRepository.finish(
                    trackId = track.id,
                    distanceM = totals?.distanceM ?: 0.0,
                    elevationGainM = totals?.elevationGainM ?: 0.0,
                    elevationLossM = totals?.elevationLossM ?: 0.0,
                )
                activeTrack = null
            },
        )
    }
}

private fun beginRecording(context: Context, trackRepository: SqlDelightTrackRepository, kind: ActivityKind): Track {
    val name = if (kind == ActivityKind.HIKE) "Hike" else "Run"
    val track = trackRepository.start(name)
    RecordingService.start(context, track.id, track.name, kind)
    return track
}

private fun requiredPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

@Composable
private fun StartButtons(modifier: Modifier = Modifier, onStart: (ActivityKind) -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onClick = { onStart(ActivityKind.HIKE) }, modifier = Modifier.weight(1f)) {
            Text("+ Start a hike")
        }
        Button(onClick = { onStart(ActivityKind.RUN) }, modifier = Modifier.weight(1f)) {
            Text("+ Start a run")
        }
    }
}

@Composable
private fun RecordingCard(
    modifier: Modifier = Modifier,
    trackName: String,
    update: RecordingUpdate?,
    onEnd: () -> Unit,
) {
    Surface(modifier = modifier.fillMaxWidth().padding(16.dp), tonalElevation = 4.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = trackName, style = MaterialTheme.typography.titleMedium)
            if (update == null || !update.hasGpsFix) {
                Text("Waiting for GPS fix…")
            } else {
                val km = update.distanceM / 1000.0
                val minutes = update.durationMs.milliseconds.inWholeMinutes
                Text("%.2f km · %d min · +%.0f m".format(km, minutes, update.elevationGainM))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onEnd) { Text("End") }
        }
    }
}
