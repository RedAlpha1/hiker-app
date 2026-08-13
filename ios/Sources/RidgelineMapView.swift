import CoreLocation
import MapLibre
import SwiftUI

/// The map -- the other native island from CLAUDE.md, same reasoning as
/// CameraPreviewView. MapLibre iOS is a third-party framework (added via
/// Swift Package Manager -- see ios/README.md), so unlike the camera this
/// genuinely is Swift-native code, not something Kotlin/Native binds to.
///
/// `styleURL` defaults to MapLibre's public demo style so this compiles and
/// shows *something* -- CLAUDE.md's DEM/terrain tile pipeline (region
/// download) is still an open question (DECISIONS.md), so there's no real
/// Ridgeline style to point at yet.
struct RidgelineMapView: UIViewRepresentable {
    let centerLatitude: Double
    let centerLongitude: Double
    let zoomLevel: Double
    var styleURL: URL? = URL(string: "https://demotiles.maplibre.org/style.json")

    func makeUIView(context: Context) -> MLNMapView {
        let mapView = MLNMapView(frame: .zero, styleURL: styleURL)
        mapView.setCenter(
            CLLocationCoordinate2D(latitude: centerLatitude, longitude: centerLongitude),
            zoomLevel: zoomLevel,
            animated: false
        )
        return mapView
    }

    func updateUIView(_ uiView: MLNMapView, context: Context) {
        uiView.setCenter(
            CLLocationCoordinate2D(latitude: centerLatitude, longitude: centerLongitude),
            zoomLevel: zoomLevel,
            animated: false
        )
    }
}
