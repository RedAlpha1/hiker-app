# :ios

Camera preview and map demo, proving the two native islands from CLAUDE.md
work and that the shared `:engine` Kotlin/Native framework links from Swift.
Not the real app yet -- see `DECISIONS.md`, "Camera preview and map view
actuals" for the current scope and why this isn't Compose Multiplatform.

**Unverified.** This was written without Xcode or a macOS host available --
first real compile happens when you build it. The Swift code (SwiftUI,
AVFoundation, MapLibre) is written against well-documented, stable APIs, but
double-check against current docs if something doesn't compile as-is.

## What's here

- `Sources/RidgelineApp.swift` -- app entry point.
- `Sources/ContentView.swift` -- the demo screen; imports `Engine` (the
  Kotlin/Native framework built from `:engine`) and constructs a real
  `GeoPoint` for Kausani, the same viewpoint `:engine`'s own tests use.
- `Sources/CameraPreviewView.swift` -- `UIViewRepresentable` wrapping
  `AVCaptureSession` + `AVCaptureVideoPreviewLayer`. System framework, no
  Kotlin/Native interop involved.
- `Sources/RidgelineMapView.swift` -- `UIViewRepresentable` wrapping
  MapLibre iOS's `MLNMapView`. Third-party framework, added via Swift
  Package Manager (below). While recording, draws the real recorded route
  (`MLNShapeSource` + `MLNLineStyleLayer`/`MLNCircleStyleLayer`, updated live)
  -- see `DECISIONS.md`, "Start Hike / Start Running: live map + real UI".
- `Sources/LocationRecorder.swift` -- "Start a hike"/"Start a run": a
  `CLLocationManager` wrapper that feeds `Engine.RecordingSession` (distance/
  elevation/duration math, including real pause/resume) and
  `Data.TrackRepository` (persistence) on every fix, publishing `UI.RecordingState`
  -- the same type `:android` renders from, not a parallel Swift-only shape.
  Real functionality, not a build/run check like the rest of this demo --
  see `DECISIONS.md`, "Start Hike / Start Running: no backend, shared
  accumulation math" and "...: live map + real UI".
- `Sources/RecordingControlsView.swift` -- the SwiftUI stat sheet driven by
  `LocationRecorder`, rebuilt against Screens 5/5b of
  `Ridgeline_Standalone.html`, wired into `ContentView`.
- `Sources/ColorTheme.swift` -- converts `UI.Colors`' ARGB `Int64` tokens to
  SwiftUI `Color`, the Swift-side call site `Colors.kt`'s own doc comment
  points to (mirrors `:android`'s `toComposeColor()`).

## Setting up the Xcode project

There's no `.xcodeproj` checked in -- hand-authoring one risks a corrupted
project file, so this has to be created from Xcode itself.

1. **File > New > Project > iOS > App.** Interface: SwiftUI. Language: Swift.
   Name it `RidgelineApp` (or whatever you like). **Save it directly inside
   this `ios/` folder** -- i.e. `hiker-app/ios/RidgelineApp.xcodeproj` -- the
   build script below assumes `$SRCROOT/..` is the repo root.
2. Delete the template's generated `ContentView.swift` and `*App.swift`,
   then drag the four files under `ios/Sources/` into the project (check
   "Copy items if needed" off, since they're already inside `ios/`).
3. **Add MapLibre via Swift Package Manager**: File > Add Package
   Dependencies, search "MapLibre" (or use the maplibre-native repo URL from
   https://maplibre.org/maplibre-native/ios/api/ if search doesn't surface
   it), add the `MapLibre` library product to the app target.
4. **Info.plist / Info tab**: add
   - `Privacy - Camera Usage Description` (`NSCameraUsageDescription`) --
     required or camera access silently fails; something like "Ridgeline
     uses the camera to identify peaks on the skyline."
   - `Privacy - Location When In Use Usage Description`
     (`NSLocationWhenInUseUsageDescription`) and `Privacy - Location Always
     and When In Use Usage Description`
     (`NSLocationAlwaysAndWhenInUseUsageDescription`) -- both required by
     `LocationRecorder.swift`'s `requestAlwaysAuthorization()`, since a
     hike/run keeps recording with the app backgrounded; something like
     "Ridgeline records your route in the background while you hike or run."
   - `Privacy - Location Updates Background Mode` -- add `location` under
     **Signing & Capabilities > Background Modes** (or `UIBackgroundModes`
     → `location` directly in Info.plist). Without this,
     `allowsBackgroundLocationUpdates = true` in `LocationRecorder.swift`
     throws at runtime.

## Embedding the Kotlin frameworks

1. Select the app target > **Build Phases > + > New Run Script Phase**.
   Drag it to run **before** "Compile Sources" (Swift needs the framework's
   generated headers to resolve `import Engine`).
2. Script contents:
   ```bash
   cd "$SRCROOT/.."
   ./gradlew :engine:embedAndSignAppleFrameworkForXcode :data:embedAndSignAppleFrameworkForXcode :ui:embedAndSignAppleFrameworkForXcode
   ```
   All three frameworks are now actually used: `ContentView.swift` imports
   `Engine`, and `LocationRecorder.swift`/`RecordingControlsView.swift`/
   `ColorTheme.swift` import `Data` and `UI` directly -- `UI.RecordingState`/
   `ActivityType`/`Colors`/`Typography` are the same types `:android` renders
   from, not a parallel Swift-only copy (see DECISIONS.md, "Start Hike /
   Start Running: live map + real UI").
3. Build. Framework search paths are handled automatically by the
   `embedAndSignAppleFrameworkForXcode` task -- if Xcode still can't find the
   framework, check that the target's "Framework Search Paths" includes the
   path the task prints (something under
   `<module>/build/xcode-frameworks/`).

## Confirm the frameworks actually build first

Before touching Xcode, it's worth confirming the Kotlin/Native side compiles
on your Mac:
```bash
./gradlew :engine:linkDebugFrameworkIosSimulatorArm64
```
If that fails, fix it before debugging anything on the Xcode side -- the
Run Script phase above will hit the same failure, just with a much less
useful error message.

See `DECISIONS.md`, "KMP module structure and engine harness" and "Camera
preview and map view actuals".
