import CoreLocation
import Data // Kotlin/Native framework built from :data
import Engine // Kotlin/Native framework built from :engine
import UI // Kotlin/Native framework built from :ui
import Foundation

/// Everything the live map/sparkline need that isn't part of `UI.RecordingState`
/// -- that type models the design's stat sheet exactly (no elevation loss,
/// no route geometry), so this stays a separate wrapper, mirroring
/// :android's `RecordingLiveData` (see DECISIONS.md, "Start Hike / Start
/// Running: live map + real UI").
struct RecordingLiveData {
    let trackName: String
    let state: UI.RecordingState
    let elevationLossM: Double
    let routePoints: [Engine.GeoPoint]
    let elevationSamples: [Double]
}

/// iOS counterpart to :android's RecordingService: CLLocationManager fixes
/// feed the same shared `Engine.RecordingSession` math and the same
/// `Data.TrackRepository`, publishing the same `UI.RecordingState` shape
/// :android renders from -- so "start a hike/run" behaves identically on
/// both platforms even though location capture itself isn't a KMP
/// expect/actual seam. See DECISIONS.md, "Camera preview and map view
/// actuals" for why platform-specific capture code lives directly in
/// :android/:ios rather than as shared commonMain, and "Start Hike / Start
/// Running: live map + real UI" for why this reuses `:ui`'s RecordingState
/// instead of a parallel Swift-only shape.
///
/// Unverified in this sandbox (no macOS/Xcode host) -- standard
/// CLLocationManager usage against well-documented APIs, same caveat as
/// every other :ios file to date. The Kotlin/Native interop calls (`UI`'s
/// enum case names, `RecordingState`'s full-arity initializer, the
/// `RecordingSessionKt` top-level-function facade) follow Kotlin/Native's
/// documented Objective-C export conventions but are the one part of this
/// file worth checking against the actual generated headers first if
/// something doesn't compile.
final class LocationRecorder: NSObject, ObservableObject, CLLocationManagerDelegate {
    @Published private(set) var liveData: RecordingLiveData?

    private let locationManager = CLLocationManager()
    private let trackRepository = Data.SqlDelightTrackRepository(
        db: DatabaseKt.createDatabase(factory: Data.DatabaseDriverFactory())
    )
    private var session = Engine.RecordingSession()
    private var trackId: String?
    private var trackName = ""
    private var activityType: UI.ActivityType = .hike
    private var hasGpsFix = false
    private var routePoints: [Engine.GeoPoint] = []
    private var elevationSamples: [Double] = []

    override init() {
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        // A hike/run keeps recording with the screen off -- requires both of
        // these plus UIBackgroundModes:location in Info.plist (see
        // ios/README.md).
        locationManager.allowsBackgroundLocationUpdates = true
        locationManager.pausesLocationUpdatesAutomatically = false
    }

    /// Starts a new track and begins recording.
    func start(activityType kind: UI.ActivityType) {
        let name = kind == .hike ? "Hike" : "Run"
        let track = trackRepository.start(name: name)

        trackId = track.id
        trackName = track.name
        activityType = kind
        session = Engine.RecordingSession()
        hasGpsFix = false
        routePoints = []
        elevationSamples = []
        publish()

        if locationManager.authorizationStatus == .notDetermined {
            locationManager.requestAlwaysAuthorization()
        }
        locationManager.startUpdatingLocation()
    }

    /// Pauses/resumes both the shared accumulation math and the GPS radio
    /// itself -- CLAUDE.md's "all-day battery" goal is exactly what a
    /// paused radio buys back, same reasoning as :android's RecordingService.
    func togglePause() {
        guard let current = liveData?.state else { return }
        if current.isPaused {
            session.resume()
            locationManager.startUpdatingLocation()
        } else {
            session.pause()
            locationManager.stopUpdatingLocation()
        }
        publish()
    }

    func requestEnd() {
        guard let data = liveData else { return }
        liveData = withState(data, data.state.requestEnd())
    }

    func cancelEnd() {
        guard let data = liveData else { return }
        liveData = withState(data, data.state.cancelEnd())
    }

    /// Stops recording and persists the final totals via `TrackRepository.finish`.
    func confirmEnd() {
        guard let id = trackId, let data = liveData else { return }
        locationManager.stopUpdatingLocation()
        _ = trackRepository.finish(
            trackId: id,
            distanceM: data.state.distanceM,
            elevationGainM: data.state.elevationGainM,
            elevationLossM: data.elevationLossM
        )
        trackId = nil
        liveData = nil
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let id = trackId, let location = locations.last else { return }

        let point = Engine.GeoPoint(latDeg: location.coordinate.latitude, lonDeg: location.coordinate.longitude)
        let timestampMs = Int64(location.timestamp.timeIntervalSince1970 * 1000)

        session.addSample(sample: Engine.RecordingSample(
            location: point,
            elevationM: location.altitude,
            timestampMs: timestampMs
        ))
        _ = trackRepository.appendPoint(
            trackId: id,
            location: point,
            elevationM: location.altitude,
            recordedAtEpochMs: timestampMs
        )
        routePoints.append(point)
        elevationSamples.append(location.altitude)
        hasGpsFix = true
        publish()
    }

    private func publish() {
        guard trackId != nil else { return }
        let totals = session.totals()
        let status: UI.RecordingStatus = session.isPaused ? .paused : (hasGpsFix ? .recording : .noGpsFix)
        let calories = activityType == .run
            ? Engine.RecordingSessionKt.estimateRunCaloriesKcal(distanceM: totals.distanceM)
            : 0

        liveData = RecordingLiveData(
            trackName: trackName,
            state: UI.RecordingState(
                activityType: activityType,
                status: status,
                distanceM: totals.distanceM,
                elevationGainM: totals.elevationGainM,
                durationMs: totals.durationMs,
                caloriesKcal: calories,
                endConfirmationPending: liveData?.state.endConfirmationPending ?? false
            ),
            elevationLossM: totals.elevationLossM,
            routePoints: routePoints,
            elevationSamples: elevationSamples
        )
    }

    private func withState(_ data: RecordingLiveData, _ state: UI.RecordingState) -> RecordingLiveData {
        RecordingLiveData(
            trackName: data.trackName,
            state: state,
            elevationLossM: data.elevationLossM,
            routePoints: data.routePoints,
            elevationSamples: data.elevationSamples
        )
    }
}
