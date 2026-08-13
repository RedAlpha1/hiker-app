package com.ridgeline.engine

/** One GPS/altitude fix during an active recording, in the order captured. */
data class RecordingSample(
    val location: GeoPoint,
    val elevationM: Double,
    val timestampMs: Long,
)

/** Cumulative distance/elevation/duration for a recording in progress. */
data class RecordingTotals(
    val distanceM: Double,
    val elevationGainM: Double,
    val elevationLossM: Double,
    val durationMs: Long,
)

/**
 * Tuning for [RecordingSession]. Raw device altitude is noisy (GPS altitude
 * error is typically worse than horizontal fix error) -- comparable in
 * spirit to [VisibilityConfig.terrainToleranceM] but a different error
 * source (a live device fix, not a DEM lookup). Elevation only accumulates
 * into gain/loss once it has moved by more than [minElevationDeltaM] from
 * the last accepted reading, so fix jitter on flat ground doesn't inflate
 * gain.
 */
data class RecordingConfig(val minElevationDeltaM: Double = 3.0) {
    init {
        require(minElevationDeltaM >= 0.0) { "minElevationDeltaM must be non-negative" }
    }
}

/**
 * Accumulates distance, elevation gain/loss, and active duration from a
 * live stream of [RecordingSample]s. Cheap per-sample arithmetic -- safe to
 * call on every GPS fix, unlike [resolveVisiblePeaks] (see CLAUDE.md,
 * "Engine performance contract", which draws exactly this line between
 * per-position and per-frame work). Mutable and stateful by design: this
 * models one live recording, not a pure function over a finished track.
 */
class RecordingSession(private val config: RecordingConfig = RecordingConfig()) {
    private var lastSample: RecordingSample? = null
    private var referenceElevationM: Double? = null
    private var paused = false

    var distanceM: Double = 0.0
        private set
    var elevationGainM: Double = 0.0
        private set
    var elevationLossM: Double = 0.0
        private set
    var durationMs: Long = 0L
        private set

    val isPaused: Boolean get() = paused

    /**
     * Pauses accumulation; samples delivered while paused are ignored. Drops
     * the last-seen sample (not the elevation reference) so the gap until
     * [resume] contributes neither distance nor duration once recording
     * continues.
     */
    fun pause() {
        paused = true
        lastSample = null
    }

    /** Resumes accumulation. The next sample re-anchors distance/duration, not elevation. */
    fun resume() {
        paused = false
    }

    /** Feeds one new fix. No-op while [isPaused]. */
    fun addSample(sample: RecordingSample) {
        if (paused) return

        val previous = lastSample
        if (previous != null) {
            distanceM += distanceM(previous.location, sample.location)
            durationMs += (sample.timestampMs - previous.timestampMs).coerceAtLeast(0L)
        }
        lastSample = sample

        val reference = referenceElevationM
        if (reference == null) {
            referenceElevationM = sample.elevationM
            return
        }
        val delta = sample.elevationM - reference
        when {
            delta >= config.minElevationDeltaM -> {
                elevationGainM += delta
                referenceElevationM = sample.elevationM
            }
            -delta >= config.minElevationDeltaM -> {
                elevationLossM += -delta
                referenceElevationM = sample.elevationM
            }
        }
    }

    fun totals(): RecordingTotals = RecordingTotals(distanceM, elevationGainM, elevationLossM, durationMs)
}
