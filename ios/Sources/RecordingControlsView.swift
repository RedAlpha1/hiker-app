import SwiftUI

/// SwiftUI counterpart to :android's RecordingScreen.kt -- "+ Start a hike" /
/// "+ Start a run" buttons, then live stats and an End button, driven by
/// [LocationRecorder]. Unverified in this sandbox (no Xcode host).
struct RecordingControlsView: View {
    @ObservedObject var recorder: LocationRecorder

    var body: some View {
        VStack(spacing: 12) {
            if recorder.isRecording {
                recordingCard
            } else {
                startButtons
            }
        }
        .padding()
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding()
    }

    private var startButtons: some View {
        HStack(spacing: 12) {
            Button("+ Start a hike") { recorder.start(activityKind: .hike) }
            Button("+ Start a run") { recorder.start(activityKind: .run) }
        }
        .buttonStyle(.borderedProminent)
    }

    private var recordingCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let update = recorder.update, update.hasGpsFix {
                let km = update.distanceM / 1000.0
                let minutes = update.durationMs / 1000 / 60
                Text(String(format: "%.2f km · %d min · +%.0f m", km, minutes, update.elevationGainM))
            } else {
                Text("Waiting for GPS fix…")
            }
            Button("End") { recorder.end() }
                .buttonStyle(.bordered)
        }
    }
}
