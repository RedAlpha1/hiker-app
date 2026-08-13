# :android

Android actuals (location, device attitude, camera preview, background
foreground service, MapLibre `AndroidView` host) per the expect/actual
boundaries in `CLAUDE.md`.

Not wired into `settings.gradle.kts` yet. Applying `com.android.application`
requires the Android SDK (`ANDROID_HOME`), which isn't installed in every
environment this project has been developed in so far. Once building on a
machine with the SDK:

1. Add `androidTarget()` to `:engine`, `:data`, `:ui`.
2. Create `android/build.gradle.kts` applying `com.android.application` (or
   `com.android.library` if this becomes a thin actuals module consumed by a
   separate app module — undecided, see `DECISIONS.md` before picking one).
3. Uncomment `include(":android")` in `settings.gradle.kts`.

See `DECISIONS.md`, "KMP module structure and engine harness".
