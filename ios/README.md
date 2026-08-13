# :ios

iOS actuals (location, device attitude, camera preview, background execution,
MapLibre `UIKitView` host) and Swift interop per the expect/actual boundaries
in `CLAUDE.md`.

Not wired into `settings.gradle.kts` yet. Kotlin/Native's `iosArm64()` /
`iosSimulatorArm64()` / `iosX64()` targets only compile on a macOS host with
Xcode, which isn't available in every environment this project has been
developed in so far. Once building on a macOS machine with Xcode:

1. Add `iosArm64()`, `iosSimulatorArm64()`, `iosX64()` to `:engine`, `:data`,
   `:ui`, with an XCFramework or CocoaPods export as needed.
2. Create the Xcode project/workspace under `ios/` for the Swift host app.
3. Uncomment `include(":ios")` in `settings.gradle.kts` if a Gradle-side
   module is needed (e.g. for a Kotlin/Native binary target), otherwise the
   Xcode project consumes the framework directly.

See `DECISIONS.md`, "KMP module structure and engine harness".
