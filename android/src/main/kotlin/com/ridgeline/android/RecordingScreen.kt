package com.ridgeline.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ridgeline.data.SqlDelightTrackRepository
import com.ridgeline.data.db.DatabaseDriverFactory
import com.ridgeline.data.db.createDatabase
import com.ridgeline.ui.screens.ActivityType
import com.ridgeline.ui.screens.RecordingState
import com.ridgeline.ui.screens.RecordingStatus
import com.ridgeline.ui.theme.Colors
import com.ridgeline.ui.theme.Typography
import kotlin.time.Duration.Companion.milliseconds

/**
 * "Start a hike"/"Start a run" and the active-recording sheet, rebuilt
 * against Screens 5/5b of `Ridgeline_Standalone.html` (status pill, hero
 * distance, 3-stat grid, elevation sparkline, Pause/End) rather than the
 * placeholder buttons Phase 1 shipped. The mockup's illustrated terrain
 * background, numbered peak markers, and "Weisshorn" chip are decorative/
 * fabricated (same category CLAUDE.md already warns about for the rest of
 * that file) and are deliberately not reproduced -- see DECISIONS.md,
 * "Start Hike / Start Running: live map + real UI". The real map behind
 * this sheet is [RidgelineMapView], wired in by the caller (`MainActivity`),
 * not this file -- this file only owns the stat sheet and the start/pause/
 * end state machine.
 */
@Stable
class RecordingController internal constructor(
    private val context: Context,
    private val trackRepository: SqlDelightTrackRepository,
) {
    var pendingActivityType by mutableStateOf<ActivityType?>(null)
        private set
    var activeTrackId by mutableStateOf<String?>(null)
        private set
    var endConfirmationPending by mutableStateOf(false)
        private set

    fun requestStart(activityType: ActivityType, hasPermission: Boolean, requestPermission: () -> Unit) {
        if (!hasPermission) {
            pendingActivityType = activityType
            requestPermission()
            return
        }
        beginRecording(activityType)
    }

    fun onPermissionResult(granted: Boolean) {
        val activityType = pendingActivityType
        pendingActivityType = null
        if (granted && activityType != null) beginRecording(activityType)
    }

    private fun beginRecording(activityType: ActivityType) {
        val name = if (activityType == ActivityType.HIKE) "Hike" else "Run"
        val track = trackRepository.start(name)
        activeTrackId = track.id
        endConfirmationPending = false
        RecordingService.start(context, track.id, track.name, activityType)
    }

    fun togglePause(isPaused: Boolean) {
        if (isPaused) RecordingService.resume(context) else RecordingService.pause(context)
    }

    fun requestEnd() {
        endConfirmationPending = true
    }

    fun cancelEnd() {
        endConfirmationPending = false
    }

    fun confirmEnd(liveData: RecordingLiveData) {
        val id = activeTrackId ?: return
        RecordingService.stop(context)
        trackRepository.finish(
            trackId = id,
            distanceM = liveData.state.distanceM,
            elevationGainM = liveData.state.elevationGainM,
            elevationLossM = liveData.elevationLossM,
        )
        activeTrackId = null
        endConfirmationPending = false
    }
}

@Composable
fun rememberRecordingController(): RecordingController {
    val context = LocalContext.current
    val trackRepository = remember {
        SqlDelightTrackRepository(createDatabase(DatabaseDriverFactory(context.applicationContext)))
    }
    return remember { RecordingController(context, trackRepository) }
}

@Composable
fun RecordingScreen(controller: RecordingController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val liveData by RecordingService.liveData.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted -> controller.onPermissionResult(granted[Manifest.permission.ACCESS_FINE_LOCATION] == true) }

    val data = liveData
    if (controller.activeTrackId == null || data == null) {
        StartButtonsRow(
            modifier = modifier,
            onStart = { activityType ->
                controller.requestStart(
                    activityType = activityType,
                    hasPermission = hasLocationPermission(context),
                    requestPermission = { permissionLauncher.launch(requiredPermissions()) },
                )
            },
        )
    } else {
        RecordingSheet(
            liveData = data,
            endConfirmationPending = controller.endConfirmationPending,
            onTogglePause = { controller.togglePause(data.state.isPaused) },
            onRequestEnd = controller::requestEnd,
            onCancelEnd = controller::cancelEnd,
            onConfirmEnd = { controller.confirmEnd(data) },
            modifier = modifier,
        )
    }
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
private fun StartButtonsRow(modifier: Modifier = Modifier, onStart: (ActivityType) -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = { onStart(ActivityType.HIKE) },
            colors = ButtonDefaults.buttonColors(containerColor = Colors.PRIMARY_BLUE.toComposeColor()),
            modifier = Modifier.weight(1f),
        ) {
            Text("+ Start a hike", style = Typography.button.toTextStyle(Color.White))
        }
        Button(
            onClick = { onStart(ActivityType.RUN) },
            colors = ButtonDefaults.buttonColors(containerColor = Colors.SUCCESS_GREEN.toComposeColor()),
            modifier = Modifier.weight(1f),
        ) {
            Text("+ Start a run", style = Typography.button.toTextStyle(Color.White))
        }
    }
}

@Composable
private fun RecordingSheet(
    liveData: RecordingLiveData,
    endConfirmationPending: Boolean,
    onTogglePause: () -> Unit,
    onRequestEnd: () -> Unit,
    onCancelEnd: () -> Unit,
    onConfirmEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = liveData.state
    val accent = accentColorFor(state.activityType)
    val activityLabel = if (state.activityType == ActivityType.HIKE) "hike" else "run"

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Colors.RAISED.toComposeColor(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 10.dp, bottom = 28.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(36.dp)
                    .height(4.dp)
                    .background(Colors.DIVIDER.toComposeColor(), RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(statusDotColor(state.status, accent), CircleShape),
                    )
                    Text(statusLabel(state), style = Typography.button.toTextStyle(Colors.INK.toComposeColor()))
                }
                Text(gpsLabel(state.status), style = Typography.caption.toTextStyle(gpsLabelColor(state.status)))
            }
            Spacer(Modifier.height(12.dp))

            if (state.status == RecordingStatus.NO_GPS_FIX) {
                Surface(
                    color = Color(0xFFF3E2D8),
                    border = BorderStroke(1.dp, Colors.PRIMARY_BLUE.toComposeColor()),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                ) {
                    Text(
                        "No GPS fix — distance estimated from motion sensors.",
                        color = Color(0xFF5C4128),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }

            Text(formatDistanceKm(state.distanceM), style = Typography.statHero.toTextStyle(Colors.INK.toComposeColor()))
            Text(
                "distance so far",
                style = Typography.body.toTextStyle(Colors.MUTED.toComposeColor()),
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                statCellsFor(state).forEach { cell ->
                    StatCell(cell, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(16.dp))

            ElevationSparkline(
                samples = liveData.elevationSamples,
                color = accent,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            if (endConfirmationPending) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCancelEnd, modifier = Modifier.weight(1f)) {
                        Text("Cancel", style = Typography.button.toTextStyle(Colors.INK.toComposeColor()))
                    }
                    Button(
                        onClick = onConfirmEnd,
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("End $activityLabel", style = Typography.button.toTextStyle(Color.White))
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Peaks: stub -- peak sightings during a track are an existing
                    // tracked gap, tied to the AR viewfinder screen (DECISIONS.md).
                    OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                        Text("Peaks", style = Typography.button.toTextStyle(Colors.INK.toComposeColor()))
                    }
                    Button(
                        onClick = onTogglePause,
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        modifier = Modifier.weight(2f),
                    ) {
                        Text(if (state.isPaused) "Resume" else "Pause", style = Typography.button.toTextStyle(Color.White))
                    }
                }
                TextButton(onClick = onRequestEnd, modifier = Modifier.padding(top = 4.dp)) {
                    Text("End $activityLabel", color = Colors.MUTED.toComposeColor(), fontSize = 12.sp)
                }
            }
        }
    }
}

private data class StatCellData(val barColor: Color, val value: String, val label: String)

private fun statCellsFor(state: RecordingState): List<StatCellData> {
    val duration = formatDuration(state.durationMs)
    val pace = formatPace(state.durationMs, state.distanceM)
    return if (state.activityType == ActivityType.HIKE) {
        listOf(
            StatCellData(Colors.ROUTE_BLUE.toComposeColor(), formatElevation(state.elevationGainM), "Elev. gain"),
            StatCellData(Colors.PRIMARY_BLUE.toComposeColor(), duration, "Duration"),
            StatCellData(Colors.SUCCESS_GREEN.toComposeColor(), pace, "Pace"),
        )
    } else {
        listOf(
            StatCellData(Colors.SUCCESS_GREEN.toComposeColor(), pace, "Pace"),
            StatCellData(Colors.PRIMARY_BLUE.toComposeColor(), duration, "Duration"),
            StatCellData(Colors.ROUTE_BLUE.toComposeColor(), state.caloriesKcal.toString(), "Kcal"),
        )
    }
}

@Composable
private fun StatCell(cell: StatCellData, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(cell.barColor, RoundedCornerShape(2.dp)))
        Spacer(Modifier.height(6.dp))
        Text(cell.value, style = Typography.statMedium.toTextStyle(Colors.INK.toComposeColor()))
        Text(cell.label.uppercase(), color = Colors.MUTED.toComposeColor(), fontSize = 9.sp)
    }
}

@Composable
private fun ElevationSparkline(samples: List<Double>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(26.dp)) {
        if (samples.size < 2) return@Canvas
        val min = samples.min()
        val max = samples.max()
        val range = (max - min).takeIf { it > 0.0 } ?: 1.0
        val stepX = size.width / (samples.size - 1)

        val path = Path()
        samples.forEachIndexed { index, elevation ->
            val x = index * stepX
            val normalized = ((elevation - min) / range).toFloat()
            val y = size.height - normalized * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

private fun accentColorFor(activityType: ActivityType): Color =
    if (activityType == ActivityType.HIKE) Colors.PRIMARY_BLUE.toComposeColor() else Colors.SUCCESS_GREEN.toComposeColor()

private fun statusDotColor(status: RecordingStatus, accent: Color): Color = when (status) {
    RecordingStatus.RECORDING -> accent
    RecordingStatus.PAUSED -> Colors.MUTED.toComposeColor()
    RecordingStatus.NO_GPS_FIX -> Colors.PRIMARY_BLUE.toComposeColor()
}

private fun statusLabel(state: RecordingState): String {
    val base = if (state.status == RecordingStatus.PAUSED) "PAUSED" else "RECORDING"
    return if (state.activityType == ActivityType.RUN) "$base · RUN" else base
}

private fun gpsLabel(status: RecordingStatus): String =
    if (status == RecordingStatus.NO_GPS_FIX) "GPS: searching" else "GPS: strong"

private fun gpsLabelColor(status: RecordingStatus): Color =
    if (status == RecordingStatus.NO_GPS_FIX) Colors.PRIMARY_BLUE.toComposeColor() else Colors.MUTED.toComposeColor()

private fun formatDistanceKm(distanceM: Double): String = "%.1f km".format(distanceM / 1000.0)

private fun formatElevation(elevationM: Double): String = "%.0f m".format(elevationM)

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs.milliseconds.inWholeSeconds
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

private fun formatPace(durationMs: Long, distanceM: Double): String {
    if (distanceM <= 0.0) return "—"
    val distanceKm = distanceM / 1000.0
    val secondsPerKm = (durationMs / 1000.0) / distanceKm
    val minutes = (secondsPerKm / 60).toInt()
    val seconds = (secondsPerKm % 60).toInt()
    return "%d:%02d /km".format(minutes, seconds)
}
