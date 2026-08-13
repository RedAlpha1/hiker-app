import CoreLocation
import Data // Kotlin/Native framework built from :data
import Engine // Kotlin/Native framework built from :engine
import Foundation

enum ActivityKind {
    case hike
    case run
}

/// Live totals a SwiftUI view can observe while recording.
struct RecordingUpdate {
    let distanceM: Double
    let elevationGainM: Double
    let elevationLossM: Double
    let durationMs: Int64
    let hasGpsFix: Bool
}

/// iOS counterpart to :android's RecordingService: CLLocationManager fixes
/// feed the same shared `Engine.RecordingSession` math and the same
/// `Data.TrackRepository`, so "start a hike/run" behaves identically on
/// both platforms even though location capture itself isn't a KMP
/// expect/actual seam -- see DECISIONS.md, "Camera preview and map view
/// actuals" for why platform-specific capture code lives directly in
/// :android/:ios rather than as shared commonMain.
///
/// Unverified in this sandbox (no macOS/Xcode host) -- standard
/// CLLocationManager usage against well-documented APIs, same caveat as
/// every other :ios file to date. The Kotlin/Native interop calls
/// (`Data.createDatabase`, the `RecordingSession`/`RecordingSample`
/// initializers) follow Kotlin/Native's documented Objective-C export
/// conventions but are the one part of this file worth checking against
/// the actual generated header first if something doesn't compile.
final class LocationRecorder: NSObject, ObservableObject, CLLocationManagerDelegate {
    @Published private(set) var update: RecordingUpdate?
    @Published private(set) var isRecording = false

    private let locationManager = CLLocationManager()
    private let trackRepository = Data.SqlDelightTrackRepository(
        db: DatabaseKt.createDatabase(factory: Data.DatabaseDriverFactory())
    )
    private var session = Engine.RecordingSession()
    private var trackId: String?
    private var activityKind: ActivityKind = .hike

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

    /// Starts a new track and begins recording. Deliberately no pause here,
    /// same reasoning as :android's RecordingHost -- Start/End only for now.
    func start(activityKind kind: ActivityKind) {
        let name = kind == .hike ? "Hike" : "Run"
        let track = trackRepository.start(name: name)

        trackId = track.id
        activityKind = kind
        session = Engine.RecordingSession()
        update = RecordingUpdate(distanceM: 0, elevationGainM: 0, elevationLossM: 0, durationMs: 0, hasGpsFix: false)
        isRecording = true

        if locationManager.authorizationStatus == .notDetermined {
            locationManager.requestAlwaysAuthorization()
        }
        locationManager.startUpdatingLocation()
    }

    /// Stops recording and persists the final totals via `TrackRepository.finish`.
    func end() {
        locationManager.stopUpdatingLocation()
        if let id = trackId, let totals = update {
            _ = trackRepository.finish(
                trackId: id,
                distanceM: totals.distanceM,
                elevationGainM: totals.elevationGainM,
                elevationLossM: totals.elevationLossM
            )
        }
        trackId = nil
        isRecording = false
        update = nil
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

        let totals = session.totals()
        update = RecordingUpdate(
            distanceM: totals.distanceM,
            elevationGainM: totals.elevationGainM,
            elevationLossM: totals.elevationLossM,
            durationMs: totals.durationMs,
            hasGpsFix: true
        )
    }
}
