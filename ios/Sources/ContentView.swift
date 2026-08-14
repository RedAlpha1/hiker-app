import AVFoundation
import CoreLocation
import Engine // Kotlin/Native framework built from :engine -- see ios/README.md
import SwiftUI

/// Two states: not recording shows the original build/run check (camera
/// preview full-bleed + a small corner map, both proving the native islands
/// from CLAUDE.md work -- see DECISIONS.md, "Camera preview and map view
/// actuals"), with "+ Start a hike"/"+ Start a run" docked bottom-leading.
/// Recording replaces that with the real feature: a full-bleed live
/// `RidgelineMapView` drawing the actual recorded route, with
/// `RecordingControlsView`'s stat sheet on top -- see DECISIONS.md, "Start
/// Hike / Start Running: live map + real UI".
struct ContentView: View {
    @State private var cameraAuthorized = false
    @StateObject private var recorder = LocationRecorder()

    // Same real coordinate GarhwalCheck.kt/ConfidenceCheck.kt use in :engine,
    // and the Android demo (MainActivity.kt) centers its map on too.
    private let kausani = GeoPoint(latDeg: 29.8422, lonDeg: 79.6006)

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            if let data = recorder.liveData {
                let routeCoordinates = data.routePoints.map {
                    CLLocationCoordinate2D(latitude: $0.latDeg, longitude: $0.lonDeg)
                }
                RidgelineMapView(
                    centerLatitude: routeCoordinates.first?.latitude ?? kausani.latDeg,
                    centerLongitude: routeCoordinates.first?.longitude ?? kausani.lonDeg,
                    zoomLevel: 15,
                    routeCoordinates: routeCoordinates,
                    currentCoordinate: routeCoordinates.last,
                    followCurrentPosition: true
                )
                .ignoresSafeArea()
            } else if cameraAuthorized {
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

            if recorder.liveData == nil {
                RidgelineMapView(
                    centerLatitude: kausani.latDeg,
                    centerLongitude: kausani.lonDeg,
                    zoomLevel: 11
                )
                .frame(width: 160, height: 200)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .padding()
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomTrailing)
            }

            RecordingControlsView(recorder: recorder)
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
