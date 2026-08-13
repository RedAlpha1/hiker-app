import AVFoundation
import Engine // Kotlin/Native framework built from :engine -- see ios/README.md
import SwiftUI

/// Minimal proof that the two native islands from CLAUDE.md -- camera
/// preview and map -- both work, and that the shared :engine framework
/// links: CameraPreviewView full-bleed, RidgelineMapView centered on
/// Kausani (the same real Garhwal viewpoint :engine's own tests use, here
/// as an `Engine.GeoPoint` constructed straight from the Kotlin framework)
/// in a corner card. This is a build/run check, not the real AR viewfinder
/// screen -- see :ui's ArViewfinderState for that screen's actual design
/// (not wired up here; see DECISIONS.md, "Camera preview and map view
/// actuals" for why :ui and this app target aren't connected yet).
struct ContentView: View {
    @State private var cameraAuthorized = false
    @StateObject private var recorder = LocationRecorder()

    // Same real coordinate GarhwalCheck.kt/ConfidenceCheck.kt use in :engine,
    // and the Android demo (MainActivity.kt) centers its map on too.
    private let kausani = GeoPoint(latDeg: 29.8422, lonDeg: 79.6006)

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            if cameraAuthorized {
                CameraPreviewView()
                    .ignoresSafeArea()
            } else {
                Color.black
                    .ignoresSafeArea()
                    .overlay(
                        Text("Camera permission is required to preview the skyline.")
                            .foregroundColor(.white)
                            .padding()
                    )
            }

            RidgelineMapView(
                centerLatitude: kausani.latDeg,
                centerLongitude: kausani.lonDeg,
                zoomLevel: 11
            )
            .frame(width: 160, height: 200)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .padding()

            RecordingControlsView(recorder: recorder)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
        }
        .task {
            await requestCameraAccess()
        }
    }

    private func requestCameraAccess() async {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            cameraAuthorized = true
        case .notDetermined:
            cameraAuthorized = await AVCaptureDevice.requestAccess(for: .video)
        default:
            cameraAuthorized = false
        }
    }
}
