package com.ridgeline.android

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.ridgeline.data.SqlDelightTrackRepository
import com.ridgeline.data.db.DatabaseDriverFactory
import com.ridgeline.data.db.createDatabase
import com.ridgeline.engine.GeoPoint
import com.ridgeline.engine.RecordingSample
import com.ridgeline.engine.RecordingSession
import com.ridgeline.engine.estimateRunCaloriesKcal
import com.ridgeline.ui.screens.ActivityType
import com.ridgeline.ui.screens.RecordingState
import com.ridgeline.ui.screens.RecordingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration.Companion.milliseconds

/**
 * Everything the live map, elevation sparkline, and `TrackRepository.finish`
 * need that isn't part of `:ui`'s [RecordingState] shape -- that type models
 * the design's stat sheet exactly (which never shows elevation loss or the
 * route itself), so this stays a separate, :android-local wrapper rather
 * than growing [RecordingState] beyond what the design actually specifies.
 */
data class RecordingLiveData(
    val trackName: String,
    val state: RecordingState,
    val elevationLossM: Double,
    val routePoints: List<GeoPoint>,
    val elevationSamples: List<Double>,
)

/**
 * Foreground service that owns one active recording end to end: raw GPS
 * fixes (plain `android.location`, not Play Services' fused provider -- no
 * new Play Services dependency, and GPS-only matches CLAUDE.md's
 * offline-first/zero-signal field constraint better than a provider that
 * can silently prefer a network fix) feed [RecordingSession] (`:engine`,
 * the shared distance/elevation/duration math) and [SqlDelightTrackRepository]
 * (`:data`, persistence) on every fix.
 *
 * A foreground service, not a plain background job: a hike can run for
 * hours with the screen off, and the persistent notification this requires
 * is the whole point, not optional chrome -- it's what keeps the OS from
 * killing GPS updates.
 *
 * Pause/resume actually stop and restart GPS updates (not just accumulation)
 * -- CLAUDE.md's "all-day battery" goal is exactly what a paused GPS radio
 * buys back.
 *
 * Unverified in this sandbox (no Android SDK) -- see DECISIONS.md, "Camera
 * preview and map view actuals" for the standing caveat on all `:android`
 * platform code.
 */
class RecordingService : Service() {

    companion object {
        const val EXTRA_TRACK_ID = "trackId"
        const val EXTRA_TRACK_NAME = "trackName"
        const val EXTRA_ACTIVITY_KIND = "activityKind"
        private const val ACTION_PAUSE = "com.ridgeline.android.action.PAUSE"
        private const val ACTION_RESUME = "com.ridgeline.android.action.RESUME"

        private const val NOTIFICATION_CHANNEL_ID = "recording"
        private const val NOTIFICATION_ID = 1
        private const val MIN_UPDATE_INTERVAL_MS = 3_000L
        private const val MIN_UPDATE_DISTANCE_M = 5f

        private val _liveData = MutableStateFlow<RecordingLiveData?>(null)

        /** Latest state for the in-progress recording, or null when nothing is recording. */
        val liveData: StateFlow<RecordingLiveData?> = _liveData.asStateFlow()

        fun start(context: Context, trackId: String, trackName: String, activityType: ActivityType) {
            val intent = Intent(context, RecordingService::class.java)
                .putExtra(EXTRA_TRACK_ID, trackId)
                .putExtra(EXTRA_TRACK_NAME, trackName)
                .putExtra(EXTRA_ACTIVITY_KIND, activityType.name)
            ContextCompat.startForegroundService(context, intent)
        }

        fun pause(context: Context) {
            context.startService(Intent(context, RecordingService::class.java).setAction(ACTION_PAUSE))
        }

        fun resume(context: Context) {
            context.startService(Intent(context, RecordingService::class.java).setAction(ACTION_RESUME))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RecordingService::class.java))
        }
    }

    private val trackRepository by lazy {
        SqlDelightTrackRepository(createDatabase(DatabaseDriverFactory(applicationContext)))
    }
    private val locationManager by lazy { getSystemService(LOCATION_SERVICE) as LocationManager }
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }

    private var session = RecordingSession()
    private var trackId: String? = null
    private var trackName: String = ""
    private var activityType: ActivityType = ActivityType.HIKE
    private var hasGpsFix = false
    private val routePoints = mutableListOf<GeoPoint>()
    private val elevationSamples = mutableListOf<Double>()

    private val locationListener = LocationListener { location -> onLocation(location) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> {
                session.pause()
                runCatching { locationManager.removeUpdates(locationListener) }
                publishUpdate()
                return START_STICKY
            }
            ACTION_RESUME -> {
                session.resume()
                beginLocationUpdates()
                publishUpdate()
                return START_STICKY
            }
        }

        val id = intent?.getStringExtra(EXTRA_TRACK_ID)
        if (id == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Already recording this track -- onStartCommand can re-fire (e.g. process restart); don't re-init.
        if (trackId == id) return START_STICKY

        trackId = id
        trackName = intent.getStringExtra(EXTRA_TRACK_NAME).orEmpty()
        activityType = ActivityType.valueOf(intent.getStringExtra(EXTRA_ACTIVITY_KIND) ?: ActivityType.HIKE.name)
        session = RecordingSession()
        hasGpsFix = false
        routePoints.clear()
        elevationSamples.clear()

        val notification = buildNotification()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )
        publishUpdate()

        if (!beginLocationUpdates()) {
            // Permission was revoked between the caller's check and this service starting --
            // nothing useful to do without location, so stop rather than run a silent no-op service.
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { locationManager.removeUpdates(locationListener) }
        _liveData.value = null
        super.onDestroy()
    }

    private fun beginLocationUpdates(): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return false

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            MIN_UPDATE_INTERVAL_MS,
            MIN_UPDATE_DISTANCE_M,
            locationListener,
        )
        return true
    }

    private fun onLocation(location: Location) {
        if (trackId == null) return
        val recordedAt = location.time
        val point = GeoPoint(location.latitude, location.longitude)

        session.addSample(RecordingSample(location = point, elevationM = location.altitude, timestampMs = recordedAt))
        trackRepository.appendPoint(
            trackId = requireNotNull(trackId),
            location = point,
            elevationM = location.altitude,
            recordedAtEpochMs = recordedAt,
        )
        routePoints += point
        elevationSamples += location.altitude
        hasGpsFix = true

        publishUpdate()
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun publishUpdate() {
        if (trackId == null) return
        val totals = session.totals()
        val status = when {
            session.isPaused -> RecordingStatus.PAUSED
            hasGpsFix -> RecordingStatus.RECORDING
            else -> RecordingStatus.NO_GPS_FIX
        }
        val calories = if (activityType == ActivityType.RUN) estimateRunCaloriesKcal(totals.distanceM) else 0

        _liveData.value = RecordingLiveData(
            trackName = trackName,
            state = RecordingState(
                activityType = activityType,
                status = status,
                distanceM = totals.distanceM,
                elevationGainM = totals.elevationGainM,
                durationMs = totals.durationMs,
                caloriesKcal = calories,
            ),
            elevationLossM = totals.elevationLossM,
            routePoints = routePoints.toList(),
            elevationSamples = elevationSamples.toList(),
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Recording",
            NotificationManager.IMPORTANCE_LOW,
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val activityLabel = if (activityType == ActivityType.HIKE) "hike" else "run"
        val totals = session.totals()
        val distanceKm = totals.distanceM / 1000.0
        val minutes = totals.durationMs.milliseconds.inWholeMinutes

        val contentText = when {
            session.isPaused -> "Paused · %.1f km".format(distanceKm)
            hasGpsFix -> "%.1f km · %d min".format(distanceKm, minutes)
            else -> "Waiting for GPS fix…"
        }

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Recording a $activityLabel")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }
}
