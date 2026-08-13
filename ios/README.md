# :ios

iOS actuals (location, device attitude, camera preview, background execution,
MapLibre `UIKitView` host) and Swift interop per the expect/actual boundaries
in `CLAUDE.md`.

**Gradle side is done:** `:engine`, `:data`, and `:ui` each declare
`iosX64()`, `iosArm64()`, `iosSimulatorArm64()` targets with a static
framework (`Engine`, `Data`, `UI` respectively). These compile only on a
macOS host with Xcode — on any other host Kotlin/Native disables them (with
a warning) rather than failing the build, which is why `./gradlew build`
still succeeds in environments without Xcode.

**Still to do, on a Mac:**

1. Confirm the frameworks actually build:
   `./gradlew :engine:linkDebugFrameworkIosSimulatorArm64` (and the `:data`,
   `:ui` equivalents).
2. Create the Xcode project/workspace under `ios/` for the Swift host app —
   this has to be done from Xcode itself (`File > New > Project`), not
   hand-authored.
3. Embed each framework via Xcode's "Embed & Sign" build phase, or add a
   "Run Script" phase invoking the Gradle
   `embedAndSignAppleFrameworkForXcode` task per module — the standard KMP
   pattern for wiring a Gradle-built framework into an Xcode build.
4. Uncomment `include(":ios")` in `settings.gradle.kts` only if a
   Gradle-side module ends up needed (e.g. a shared Kotlin/Native binary
   entry point); if the Xcode project just links the three frameworks
   directly, it isn't required.

See `DECISIONS.md`, "KMP module structure and engine harness".
