import SwiftUI
import UI // Kotlin/Native framework built from :ui

/// SwiftUI counterpart to :android's RecordingScreen.kt, rebuilt against
/// Screens 5/5b of `Ridgeline_Standalone.html` -- status pill, hero
/// distance, 3-stat grid, elevation sparkline, Pause/End -- rather than the
/// placeholder buttons this file started as. See DECISIONS.md, "Start Hike
/// / Start Running: live map + real UI" for why the mockup's illustrated
/// terrain background and peak markers are deliberately not reproduced.
/// Unverified in this sandbox (no Xcode host).
struct RecordingControlsView: View {
    @ObservedObject var recorder: LocationRecorder

    var body: some View {
        VStack {
            if let data = recorder.liveData {
                RecordingSheet(
                    liveData: data,
                    onTogglePause: { recorder.togglePause() },
                    onRequestEnd: { recorder.requestEnd() },
                    onCancelEnd: { recorder.cancelEnd() },
                    onConfirmEnd: { recorder.confirmEnd() }
                )
            } else {
                StartButtonsRow(onStart: { recorder.start(activityType: $0) })
            }
        }
    }
}

private struct StartButtonsRow: View {
    let onStart: (UI.ActivityType) -> Void

    var body: some View {
        HStack(spacing: 12) {
            Button("+ Start a hike") { onStart(.hike) }
                .buttonStyle(FilledButtonStyle(color: Color(argb: Colors.shared.PRIMARY_BLUE)))
            Button("+ Start a run") { onStart(.run) }
                .buttonStyle(FilledButtonStyle(color: Color(argb: Colors.shared.SUCCESS_GREEN)))
        }
        .padding()
    }
}

private struct RecordingSheet: View {
    let liveData: RecordingLiveData
    let onTogglePause: () -> Void
    let onRequestEnd: () -> Void
    let onCancelEnd: () -> Void
    let onConfirmEnd: () -> Void

    private var state: UI.RecordingState { liveData.state }
    private var accent: Color {
        state.activityType == .hike ? Color(argb: Colors.shared.PRIMARY_BLUE) : Color(argb: Colors.shared.SUCCESS_GREEN)
    }
    private var activityLabel: String { state.activityType == .hike ? "hike" : "run" }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Capsule()
                .fill(Color(argb: Colors.shared.DIVIDER))
                .frame(width: 36, height: 4)
                .frame(maxWidth: .infinity)
                .padding(.bottom, 14)

            HStack {
                HStack(spacing: 8) {
                    Circle()
                        .fill(statusDotColor)
                        .frame(width: 9, height: 9)
                    Text(statusLabel)
                        .font(.system(size: 13, weight: .heavy))
                        .tracking(0.8)
                        .foregroundColor(Color(argb: Colors.shared.INK))
                }
                Spacer()
                Text(gpsLabel)
                    .font(.system(size: 12, weight: .semibold, design: .monospaced))
                    .foregroundColor(gpsLabelColor)
            }
            .padding(.bottom, 12)

            if state.status == .noGpsFix {
                Text("No GPS fix — distance estimated from motion sensors.")
                    .font(.system(size: 12))
                    .foregroundColor(Color(red: 0.36, green: 0.25, blue: 0.16))
                    .padding(10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(red: 0.95, green: 0.89, blue: 0.85))
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(argb: Colors.shared.PRIMARY_BLUE), lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .padding(.bottom, 12)
            }

            Text(formatDistanceKm(state.distanceM))
                .font(.system(size: 52, weight: .heavy, design: .monospaced))
                .foregroundColor(Color(argb: Colors.shared.INK))
            Text("distance so far")
                .font(.system(size: 12, weight: .medium))
                .foregroundColor(Color(argb: Colors.shared.MUTED))
                .padding(.bottom, 16)

            HStack(spacing: 14) {
                ForEach(statCells, id: \.label) { cell in
                    StatCellView(cell: cell)
                }
            }
            .padding(.bottom, 16)

            ElevationSparkline(samples: liveData.elevationSamples, color: accent)
                .frame(height: 26)
                .padding(.bottom, 16)

            if state.endConfirmationPending {
                HStack(spacing: 8) {
                    Button("Cancel", action: onCancelEnd)
                        .buttonStyle(OutlineButtonStyle())
                    Button("End \(activityLabel)", action: onConfirmEnd)
                        .buttonStyle(FilledButtonStyle(color: accent))
                }
            } else {
                HStack(spacing: 8) {
                    // Peaks: stub -- peak sightings during a track are an existing
                    // tracked gap, tied to the AR viewfinder screen (DECISIONS.md).
                    Button("Peaks", action: {})
                        .buttonStyle(OutlineButtonStyle())
                    Button(state.isPaused ? "Resume" : "Pause", action: onTogglePause)
                        .buttonStyle(FilledButtonStyle(color: accent))
                }
                Button("End \(activityLabel)", action: onRequestEnd)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(Color(argb: Colors.shared.MUTED))
                    .padding(.top, 4)
            }
        }
        .padding(EdgeInsets(top: 10, leading: 20, bottom: 28, trailing: 20))
        .background(Color(argb: Colors.shared.RAISED))
        .clipShape(RoundedCorner(radius: 20, corners: [.topLeft, .topRight]))
        .shadow(radius: 8, y: -2)
    }

    private var statusDotColor: Color {
        switch state.status {
        case .recording: return accent
        case .paused: return Color(argb: Colors.shared.MUTED)
        case .noGpsFix: return Color(argb: Colors.shared.PRIMARY_BLUE)
        default: return accent
        }
    }

    private var statusLabel: String {
        let base = state.status == .paused ? "PAUSED" : "RECORDING"
        return state.activityType == .run ? "\(base) · RUN" : base
    }

    private var gpsLabel: String { state.status == .noGpsFix ? "GPS: searching" : "GPS: strong" }
    private var gpsLabelColor: Color {
        state.status == .noGpsFix ? Color(argb: Colors.shared.PRIMARY_BLUE) : Color(argb: Colors.shared.MUTED)
    }

    private var statCells: [StatCellData] {
        let duration = formatDuration(state.durationMs)
        let pace = formatPace(durationMs: state.durationMs, distanceM: state.distanceM)
        if state.activityType == .hike {
            return [
                StatCellData(barColor: Color(argb: Colors.shared.ROUTE_BLUE), value: formatElevation(state.elevationGainM), label: "Elev. gain"),
                StatCellData(barColor: Color(argb: Colors.shared.PRIMARY_BLUE), value: duration, label: "Duration"),
                StatCellData(barColor: Color(argb: Colors.shared.SUCCESS_GREEN), value: pace, label: "Pace"),
            ]
        } else {
            return [
                StatCellData(barColor: Color(argb: Colors.shared.SUCCESS_GREEN), value: pace, label: "Pace"),
                StatCellData(barColor: Color(argb: Colors.shared.PRIMARY_BLUE), value: duration, label: "Duration"),
                StatCellData(barColor: Color(argb: Colors.shared.ROUTE_BLUE), value: "\(state.caloriesKcal)", label: "Kcal"),
            ]
        }
    }
}

private struct StatCellData {
    let barColor: Color
    let value: String
    let label: String
}

private struct StatCellView: View {
    let cell: StatCellData

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            RoundedRectangle(cornerRadius: 2).fill(cell.barColor).frame(height: 3)
            Text(cell.value)
                .font(.system(size: 17, weight: .bold, design: .monospaced))
                .foregroundColor(Color(argb: Colors.shared.INK))
            Text(cell.label.uppercased())
                .font(.system(size: 9))
                .foregroundColor(Color(argb: Colors.shared.MUTED))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct ElevationSparkline: View {
    let samples: [Double]
    let color: Color

    var body: some View {
        GeometryReader { geometry in
            if samples.count >= 2, let min = samples.min(), let max = samples.max() {
                let range = (max - min) > 0 ? (max - min) : 1.0
                let stepX = geometry.size.width / CGFloat(samples.count - 1)

                Path { path in
                    for (index, elevation) in samples.enumerated() {
                        let x = CGFloat(index) * stepX
                        let normalized = CGFloat((elevation - min) / range)
                        let y = geometry.size.height - normalized * geometry.size.height
                        if index == 0 {
                            path.move(to: CGPoint(x: x, y: y))
                        } else {
                            path.addLine(to: CGPoint(x: x, y: y))
                        }
                    }
                }
                .stroke(color, style: StrokeStyle(lineWidth: 2, lineCap: .round, lineJoin: .round))
            }
        }
    }
}

private struct FilledButtonStyle: ButtonStyle {
    let color: Color

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 14, weight: .heavy))
            .foregroundColor(.white)
            .frame(maxWidth: .infinity, minHeight: 48)
            .background(color.opacity(configuration.isPressed ? 0.85 : 1))
            .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

private struct OutlineButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 13, weight: .bold))
            .foregroundColor(Color(argb: Colors.shared.INK))
            .frame(maxWidth: .infinity, minHeight: 48)
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(red: 0.78, green: 0.78, blue: 0.82), lineWidth: 1))
    }
}

/// Rounds only the given corners -- SwiftUI has no built-in for this.
private struct RoundedCorner: Shape {
    var radius: CGFloat = 0
    var corners: UIRectCorner = .allCorners

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(roundedRect: rect, byRoundingCorners: corners, cornerRadii: CGSize(width: radius, height: radius))
        return Path(path.cgPath)
    }
}

private func formatDistanceKm(_ distanceM: Double) -> String {
    String(format: "%.1f km", distanceM / 1000.0)
}

private func formatElevation(_ elevationM: Double) -> String {
    String(format: "%.0f m", elevationM)
}

private func formatDuration(_ durationMs: Int64) -> String {
    let totalSeconds = durationMs / 1000
    let hours = totalSeconds / 3600
    let minutes = (totalSeconds % 3600) / 60
    let seconds = totalSeconds % 60
    return hours > 0
        ? String(format: "%d:%02d:%02d", hours, minutes, seconds)
        : String(format: "%d:%02d", minutes, seconds)
}

private func formatPace(durationMs: Int64, distanceM: Double) -> String {
    guard distanceM > 0 else { return "—" }
    let distanceKm = distanceM / 1000.0
    let secondsPerKm = (Double(durationMs) / 1000.0) / distanceKm
    let minutes = Int(secondsPerKm / 60)
    let seconds = Int(secondsPerKm.truncatingRemainder(dividingBy: 60))
    return String(format: "%d:%02d /km", minutes, seconds)
}
