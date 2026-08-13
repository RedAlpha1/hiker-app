# :android

A plain Jetpack Compose Android application module: `MainActivity` shows a
full-screen CameraX preview with a MapLibre map in a corner card, centered
on Kausani (the same real Garhwal viewpoint `:engine`'s own tests use).
This proves the two native islands from CLAUDE.md -- camera preview and
map -- both work, and that `:engine`/`:data` are consumable as Android
libraries. It's a build/run check, not the real AR viewfinder screen.

**Unverified.** Written without an Android SDK available in the
environment that produced it -- first real compile happens on your machine.
See `DECISIONS.md`, "Camera preview and map view actuals" for the full
reasoning behind this module's shape.

`RecordingService.kt` and `RecordingScreen.kt` are real functionality, not a
build/run check like the rest of this module: a foreground service
(`android.location.LocationManager`, not Play Services) that drives
"+ Start a hike" / "+ Start a run" end to end -- GPS fixes into `:engine`'s
`RecordingSession` for the distance/elevation/duration math and `:data`'s
`TrackRepository` for persistence, with a persistent notification while
recording. See `DECISIONS.md`, "Start Hike / Start Running: no backend,
shared accumulation math".

## Activates automatically in Android Studio

This module -- and `androidTarget()` on `:engine`/`:data` -- only get
included when an Android SDK is findable (`ANDROID_HOME`/`ANDROID_SDK_ROOT`
env var, or a `local.properties` at the repo root with `sdk.dir` set).
Opening this repo in Android Studio writes that file automatically, so
nothing needs to be uncommented or edited by hand -- just open the project
and let Gradle sync. If it doesn't activate, check that one of those three
things is actually present (see `settings.gradle.kts`).

## Why plain Jetpack Compose, not Compose Multiplatform

CLAUDE.md's architecture puts Compose Multiplatform in `:ui`, shared across
both platforms. That's not what happened here: Compose Multiplatform's
common runtime has a hard transitive dependency on `androidx.arch.core` /
`androidx.lifecycle`, which only exist on Google's Maven repo -- and that
host was blocked by network policy in the environment this was built in,
for *every* target, not just Android. Rather than risk breaking `:ui`'s
already-tested plain-Kotlin state classes by adding a dependency that
couldn't be verified at all, camera/map living directly in `:android` (and
in plain SwiftUI on iOS -- see `../ios/README.md`) was the safer call.
`:ui`'s screen state (`ArViewfinderState` etc.) isn't wired into this demo
yet as a result -- see `MainActivity.kt`'s doc comment.

This is very possibly *not* how the app should look once it has real
network access to Google's Maven repo (which your machine does) -- if you
want true Compose Multiplatform screens shared across Android and iOS,
that's a reasonable thing to revisit. Nothing here forecloses it.

## Running it

Open the repo root in Android Studio. Grant camera permission when
prompted. You should see a live camera feed with a small map in the
bottom-right corner, centered on Kausani, Uttarakhand.
