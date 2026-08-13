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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration.Companion.milliseconds

enum class ActivityKind { HIKE, RUN }

/** Live totals a UI can collect while [RecordingService] is running. */
data class RecordingUpdate(
    val trackId: String,
    val activityKind: ActivityKind,
    val distanceM: Double,
    val elevationGainM: Double,
    val elevationLossM: Double,
    val durationMs: Long,
    val hasGpsFix: Boolean,
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
 * Unverified in this sandbox (no Android SDK) -- see DECISIONS.md, "Camera
 * preview and map view actuals" for the standing caveat on all `:android`
 * platform code.
 */
class RecordingService : Service() {

    companion object {
        const val EXTRA_TRACK_ID = "trackId"
        const val EXTRA_TRACK_NAME = "trackName"
        const val EXTRA_ACTIVITY_KIND = "activityKind"

        private const val NOTIFICATION_CHANNEL_ID = "recording"
        private const val NOTIFICATION_ID = 1
        private const val MIN_UPDATE_INTERVAL_MS = 3_000L
        private const val MIN_UPDATE_DISTANCE_M = 5f

        private val _updates = MutableStateFlow<RecordingUpdate?>(null)

        /** Latest totals for the in-progress recording, or null when nothing is recording. */
        val updates: StateFlow<RecordingUpdate?> = _updates.asStateFlow()

        fun start(context: Context, trackId: String, trackName: String, activityKind: ActivityKind) {
            val intent = Intent(context, RecordingService::class.java)
                .putExtra(EXTRA_TRACK_ID, trackId)
                .putExtra(EXTRA_TRACK_NAME, trackName)
                .putExtra(EXTRA_ACTIVITY_KIND, activityKind.name)
            ContextCompat.startForegroundService(context, intent)
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
    private var activityKind: ActivityKind = ActivityKind.HIKE

    private val locationListener = LocationListener { location -> onLocation(location) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getStringExtra(EXTRA_TRACK_ID)
        if (id == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Already recording this track -- onStartCommand can re-fire (e.g. process restart); don't re-init.
        if (trackId == id) return START_STICKY

        trackId = id
        trackName = intent.getStringExtra(EXTRA_TRACK_NAME).orEmpty()
        activityKind = ActivityKind.valueOf(intent.getStringExtra(EXTRA_ACTIVITY_KIND) ?: ActivityKind.HIKE.name)
        session = RecordingSession()

        val notification = buildNotification(hasGpsFix = false)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )
        publishUpdate(hasGpsFix = false)

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
        _updates.value = null
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
        val id = trackId ?: return
        val recordedAt = location.time

        session.addSample(
            RecordingSample(
                location = GeoPoint(location.latitude, location.longitude),
                elevationM = location.altitude,
                timestampMs = recordedAt,
            ),
        )
        trackRepository.appendPoint(
            trackId = id,
            location = GeoPoint(location.latitude, location.longitude),
            elevationM = location.altitude,
            recordedAtEpochMs = recordedAt,
        )

        publishUpdate(hasGpsFix = true)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(hasGpsFix = true))
    }

    private fun publishUpdate(hasGpsFix: Boolean) {
        val id = trackId ?: return
        val totals = session.totals()
        _updates.value = RecordingUpdate(
            trackId = id,
            activityKind = activityKind,
            distanceM = totals.distanceM,
            elevationGainM = totals.elevationGainM,
            elevationLossM = totals.elevationLossM,
            durationMs = totals.durationMs,
            hasGpsFix = hasGpsFix,
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

    private fun buildNotification(hasGpsFix: Boolean): Notification {
        val activityLabel = if (activityKind == ActivityKind.HIKE) "hike" else "run"
        val totals = session.totals()
        val distanceKm = totals.distanceM / 1000.0
        val minutes = totals.durationMs.milliseconds.inWholeMinutes

        val contentText = if (hasGpsFix) {
            "%.1f km · %d min".format(distanceKm, minutes)
        } else {
            "Waiting for GPS fix…"
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
