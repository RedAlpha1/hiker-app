import CoreLocation
import MapLibre
import SwiftUI

private let routeSourceID = "ridgeline-route-source"
private let routeLayerID = "ridgeline-route-layer"
private let markersSourceID = "ridgeline-markers-source"
private let markersLayerID = "ridgeline-markers-layer"
private let markerColorProperty = "markerColor"

/// The map -- the other native island from CLAUDE.md, same reasoning as
/// CameraPreviewView. MapLibre iOS is a third-party framework (added via
/// Swift Package Manager -- see ios/README.md), so unlike the camera this
/// genuinely is Swift-native code, not something Kotlin/Native binds to.
///
/// `styleURL` defaults to MapLibre's public demo style so this compiles and
/// shows *something* -- CLAUDE.md's DEM/terrain tile pipeline (region
/// download) is still an open question (DECISIONS.md), so there's no real
/// Ridgeline style to point at yet.
///
/// `routeCoordinates`/`currentCoordinate` draw the actual recorded track --
/// real data from `LocationRecorder`, not the design mockup's illustrated
/// route (see DECISIONS.md, "Start Hike / Start Running: live map + real
/// UI"). Drawn as an `MLNShapeSource` + `MLNLineStyleLayer`/`MLNCircleStyleLayer`,
/// the same GeoJSON-layer approach :android's RidgelineMapView uses
/// (MapLibre Android's `GeoJsonSource`/`LineLayer`/`CircleLayer`) rather
/// than the older `MLNPolyline`/`MLNAnnotation` API, so both platforms
/// style the route identically and update it the same way.
struct RidgelineMapView: UIViewRepresentable {
    let centerLatitude: Double
    let centerLongitude: Double
    let zoomLevel: Double
    var styleURL: URL? = URL(string: "https://demotiles.maplibre.org/style.json")
    var routeCoordinates: [CLLocationCoordinate2D] = []
    var currentCoordinate: CLLocationCoordinate2D?
    var followCurrentPosition = false

    func makeUIView(context: Context) -> MLNMapView {
        let mapView = MLNMapView(frame: .zero, styleURL: styleURL)
        mapView.delegate = context.coordinator
        mapView.setCenter(
            CLLocationCoordinate2D(latitude: centerLatitude, longitude: centerLongitude),
            zoomLevel: zoomLevel,
            animated: false
        )
        return mapView
    }

    func updateUIView(_ uiView: MLNMapView, context: Context) {
        context.coordinator.pendingRoute = routeCoordinates
        context.coordinator.pendingCurrent = currentCoordinate
        if let style = uiView.style {
            context.coordinator.applyRoute(to: style)
        }

        if followCurrentPosition, let current = currentCoordinate {
            uiView.setCenter(current, zoomLevel: uiView.zoomLevel, animated: true)
        } else if !followCurrentPosition {
            uiView.setCenter(
                CLLocationCoordinate2D(latitude: centerLatitude, longitude: centerLongitude),
                zoomLevel: zoomLevel,
                animated: false
            )
        }
    }

    func makeCoordinator() -> Coordinator { Coordinator() }

    final class Coordinator: NSObject, MLNMapViewDelegate {
        var pendingRoute: [CLLocationCoordinate2D] = []
        var pendingCurrent: CLLocationCoordinate2D?

        func mapView(_ mapView: MLNMapView, didFinishLoading style: MLNStyle) {
            let routeSource = MLNShapeSource(identifier: routeSourceID, shape: nil, options: nil)
            style.addSource(routeSource)
            let routeLayer = MLNLineStyleLayer(identifier: routeLayerID, source: routeSource)
            routeLayer.lineColor = NSExpression(forConstantValue: UIColor(argb: 0xFF1D5FFF))
            routeLayer.lineWidth = NSExpression(forConstantValue: 4)
            routeLayer.lineCap = NSExpression(forConstantValue: "round")
            routeLayer.lineJoin = NSExpression(forConstantValue: "round")
            style.addLayer(routeLayer)

            let markersSource = MLNShapeSource(identifier: markersSourceID, shape: nil, options: nil)
            style.addSource(markersSource)
            let markersLayer = MLNCircleStyleLayer(identifier: markersLayerID, source: markersSource)
            markersLayer.circleColor = NSExpression(forKeyPath: markerColorProperty)
            markersLayer.circleRadius = NSExpression(forConstantValue: 6)
            markersLayer.circleStrokeColor = NSExpression(forConstantValue: UIColor.white)
            markersLayer.circleStrokeWidth = NSExpression(forConstantValue: 2)
            style.addLayer(markersLayer)

            applyRoute(to: style)
        }

        /// Updates the two sources in place -- cheaper than removing and
        /// re-adding layers on every GPS fix, same reasoning as :android's
        /// `GeoJsonSource.setGeoJson` calls.
        func applyRoute(to style: MLNStyle) {
            if pendingRoute.count >= 2, let routeSource = style.source(withIdentifier: routeSourceID) as? MLNShapeSource {
                routeSource.shape = MLNPolylineFeature(coordinates: pendingRoute, count: UInt(pendingRoute.count))
            }

            guard let markersSource = style.source(withIdentifier: markersSourceID) as? MLNShapeSource else { return }
            var markers: [MLNPointFeature] = []
            if let start = pendingRoute.first {
                let feature = MLNPointFeature()
                feature.coordinate = start
                feature.attributes = [markerColorProperty: "#4f6b3f"]
                markers.append(feature)
            }
            if let current = pendingCurrent {
                let feature = MLNPointFeature()
                feature.coordinate = current
                feature.attributes = [markerColorProperty: "#1d5fff"]
                markers.append(feature)
            }
            markersSource.shape = MLNShapeCollectionFeature(shapes: markers)
        }
    }
}

private extension UIColor {
    convenience init(argb: Int64) {
        let a = CGFloat((argb >> 24) & 0xFF) / 255.0
        let r = CGFloat((argb >> 16) & 0xFF) / 255.0
        let g = CGFloat((argb >> 8) & 0xFF) / 255.0
        let b = CGFloat(argb & 0xFF) / 255.0
        self.init(red: r, green: g, blue: b, alpha: a)
    }
}
