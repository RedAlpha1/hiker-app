# Ridgeline — decision log

Append-only. Each entry records what was decided and *why*, so the reasoning
survives a gap of months. Newest at the bottom.

---

## 2026-08-13 — Product shape

**Decided:** Trail/run recording plus live AR peak identification. Android and iOS
from one Compose Multiplatform UI. Offline-first. AR ships in v1.

**Why:** The AR viewfinder is the differentiator — a recording app without it is
one of fifty. Shipping recording first and AR later risks launching into a
crowded category with nothing to say.

**Cost accepted:** Roughly 5–7 months solo. Explicitly agreed as acceptable;
this is a personal project without a deadline.

---

## 2026-08-13 — No backend, no accounts in v1

**Decided:** All data local. No user accounts, no app server.

**Why:** Nothing in the v1 screens moves data between users, and nothing needs
server-side trust (no payments, leaderboards, or quotas). A backend would be
pure cost.

**Still required:** Hosted *content* — DEM tiles and peak data for region
downloads (static files on a CDN), plus a weather API for the summary card.
That is a data pipeline, not a server.

**Kept reversible by:** Repository interfaces over all data access; UUID primary
keys generated client-side; `createdAt`/`updatedAt` on every row; soft deletes.
Adding sync later is then a new implementation, not a migration.

---

## 2026-08-13 — Curated regions, not worldwide

**Decided:** Ship a small set of hand-curated regions.

**Why:** Peak data quality can be hand-verified per region, and the DEM pipeline
does not need to handle every projection, voidfill and naming edge case on
earth before v1 works anywhere.

---

## 2026-08-13 — CMP shared UI with two native islands

**Decided:** Compose Multiplatform for shared UI. Map view and camera preview
are platform views hosted via `AndroidView` / `UIKitView`, with CMP overlays
drawn on top.

**Why:** CMP has no map story, and camera preview must be native on both sides.
Everything else — library, summary, detail, onboarding — is list and form work
that CMP handles well, and shared UI means design changes land in one place.

**Trade-off accepted:** CMP renders its own canvas on iOS, so scroll physics and
text selection are not natively free. Judged acceptable for these screens.

---

## 2026-08-13 — Design file peak data is decorative, not ground truth

**Decided:** Do not use any coordinate, bearing or distance from
`Ridgeline_Standalone.html` as a test fixture.

**Why:** Verified against real geodesy. The mockup's bearings (214°, 198°, 187°,
179°, 165°) decrease monotonically because peaks were laid out left-to-right
across the frame — they are not computed. The viewpoint elevation (3,089 m) is
correct for Gornergrat but its lat/lon sits north of Zermatt.

**Consequence:** The real peaks around Gornergrat span 314° of bearing. A 65°
frame holds 2–4 named summits, not the 8 the mockup implies. The density system
is less load-bearing than the design assumes.

---

## 2026-08-13 — Label lanes are absolute, not per-peak offsets

**Decided:** Chips stack in fixed horizontal bands; leader lines absorb the
varying drop to each summit.

**Why:** Real summits from one viewpoint cluster into a narrow elevation band —
4.9° to 12.7° at Gornergrat. Placing each chip at a fixed offset above its own
anchor let chips in different lanes land within a chip-height of each other.
Caught by the harness as an overlapping pair. Absolute bands make lane
separation exact.

**Consequence for design:** The mockup's comfortable vertical stagger is not
available. Collisions are almost entirely horizontal.

---

## 2026-08-13 — Region scope: Uttarakhand and Himachal only, Indian users

**Decided:** v1 covers Garhwal, Kumaon and Himachal peaks. Other ranges later.

**Consequences, all verified against real coordinates:**

- **Range raised from 60km to 150km.** From Kausani a 60km cutoff keeps *one*
  of eleven visible peaks. Nanda Devi is 69km out, Kamet 120km, Shivling 126km,
  and those distant summits are the reason the viewpoint exists.
- **Curvature correction became load-bearing.** At 126km the earth drops the
  target 1,084m. Omitting it misplaces the label by ~0.5°.
- **Density reversed an earlier finding.** In the Alps a frame holds 2–4 peaks;
  from Kausani a landscape frame holds ten. The design's two-tier chip/dot
  system is essential here, and four lanes may prove too few.
- **Perf split is now mandatory:** `resolveVisiblePeaks` runs on position change
  (~every 50m); only `project` runs per frame.

**Open:** Indian geospatial regulation for downloadable DEM tiles covering
LAC-adjacent terrain. Needs qualified legal review before the pipeline is built.

---

## 2026-08-13 — AR viewfinder defaults to landscape

**Decided:** AR screen defaults to landscape, supports both, never force-rotates.
Recording and library stay portrait.

**Why:** Not vertical framing — the visible skyline spans only 3.6° of elevation
at Kausani, so portrait's vertical FOV is already fifteen times more than needed.
The gain is horizontal resolution: 12.9 px per degree of bearing versus 8.0. A
name chip covers 18.7° of sky in portrait and 11.7° in landscape, and peaks
arrive every ~5°.

**Cost:** Two-handed operation on a cold ridge. Shutter and tap targets must be
reachable at the screen edges.

---

## 2026-08-13 — Distant peaks get dots, not names

**Decided:** Peaks past the `MARGINAL` confidence threshold render as dots and
reveal their name on tap. `HAZY` peaks keep names but rank below `CLEAR` ones
when lanes run out.

**Why:** A label on a summit the user cannot actually see undermines trust in
every label they can.

**Open:** The 80km/120km thresholds are an estimate, not a measurement. Tune
against photographs taken from Kausani and Chopta across seasons — pre-monsoon
haze and post-monsoon clarity will not share one threshold.

---

## 2026-08-13 — KMP module structure and engine harness

**Decided:** Five modules per the architecture in `CLAUDE.md` —
`:engine`, `:data`, `:ui`, `:android`, `:ios` — with `:engine` built out first
as a pure-Kotlin multiplatform module (`commonMain`/`commonTest`, `jvm()`
target) and a green JVM test suite.

**Why:** `:engine` has no platform imports by design, so it is the one module
that can be fully implemented and verified without any platform SDK. Standing
it up first, with the harness green, gives every later module (`:data` for
persistence, `:ui` for screens, `:android`/`:ios` for actuals) a tested
foundation to build against.

**Sandbox constraint:** This environment has JDK 21 and Gradle but no Android
SDK and no Xcode/macOS host. `:engine`, `:data`, and `:ui` are wired into
`settings.gradle.kts` and build with a `jvm()` target only for now.
`:android` and `:ios` are scaffolded as directories with a note explaining why
they aren't wired into the Gradle build yet — `com.android.application` needs
`ANDROID_HOME`, and iOS `konan` targets need a macOS host. Adding
`androidTarget()` to `:engine`/`:data`/`:ui` and including `:android`/`:ios`
in the build is future work for a machine with those toolchains; nothing here
should be read as a decision to drop those platforms.

**Harness anchoring:** Per the existing testing convention, every case uses
real coordinates, never numbers from the design mockup. `Harness.kt` (physically
`engine/src/commonTest/kotlin/com/ridgeline/engine/Harness.kt` — the
`commonTest` source set is what makes it runnable as a JVM test; CLAUDE.md's
`engine/test/Harness.kt` is the conceptual path) uses published coordinates for
Gornergrat and the surrounding Pennine Alps summits (Matterhorn, Dufourspitze,
Weisshorn, Dom, Breithorn, Zinalrothorn, Ober Gabelhorn, Dent Blanche) and
asserts the bearing-spread and label-non-collision properties this log already
established, rather than re-asserting the specific 4.9°–12.7° figure to a
precision this project can't independently re-verify from a decision-log entry
alone. `GarhwalCheck.kt` encodes the Kausani-to-Nanda-Devi/Kamet/Shivling
distances and the 126km curvature drop exactly as given above, since those
figures are this log's own ground truth. `ConfidenceCheck.kt` covers the
80km/120km haze thresholds and the HAZY-ranks-below-CLEAR lane rule.

---

## 2026-08-13 — iOS targets added to :engine, :data, :ui

**Decided:** `:engine`, `:data`, and `:ui` each declare `iosX64()`,
`iosArm64()`, `iosSimulatorArm64()` targets with a static framework
(`Engine`, `Data`, `UI`).

**Why:** The product is Android and iOS from one shared codebase — that's
the whole premise (CLAUDE.md, top of file). Declaring the targets now, even
without a macOS host to compile them on, keeps the module boundaries honest:
`:engine`, `:data`, and `:ui` are meant to be platform-agnostic Kotlin, and a
build that can't even *configure* iOS targets would let platform-specific
assumptions creep in unnoticed.

**Why this is safe in a non-macOS environment:** Kotlin/Native disables
targets the host can't build, with a warning, rather than failing
configuration or the build — unlike the Android Gradle Plugin, which needs
`ANDROID_HOME` just to configure and would break the build outright. That
asymmetry is why `androidTarget()` is still deliberately not added (see the
entry above); `iosArm64()` etc. carry no such risk.
`kotlin.native.ignoreDisabledTargets=true` in `gradle.properties` silences
the resulting "cannot be built on this machine" notice.

**Still required, on a Mac:** the actual Xcode project under `ios/` (Xcode
won't open a bare directory — there's nothing for it to recognize without a
`.xcodeproj`/`.xcworkspace`), and embedding the three frameworks into it. See
`ios/README.md`.

---

## 2026-08-13 — :data layer: schema, repositories, GPX

**Decided:** SQLDelight schema for four tables (`trackEntity`,
`trackPointEntity`, `peakEntity`, `regionEntity`), repository interfaces
(`TrackRepository`, `PeakRepository`, `RegionRepository`) with SQLDelight-
backed implementations, and a hand-rolled GPX reader/writer. All four
targets (jvm, iosX64/iosArm64/iosSimulatorArm64) configure; jvm is also
where this module's tests actually run, against an in-memory SQLite
database via the JDBC driver.

**Schema follows the existing conventions exactly:** client-generated UUID
ids (`newId()`, `kotlin.uuid.Uuid`), `createdAt`/`updatedAt` on `trackEntity`,
soft delete via `deletedAtEpochMs` — see "No backend, no accounts in v1".
`trackPointEntity` has none of that: points are only ever removed as a whole
with their track, so there's no tombstone case to design for.

**`PeakRepository` returns `:engine`'s own `Peak` type directly**, not a
`:data`-local copy — the repository's job is the row↔domain mapping (SQL
columns, comma-joined `aliases`), not introducing a second peak shape callers
would have to convert between before calling `resolveVisiblePeaks`.

**Region download itself is out of scope here.** `RegionRepository` is only
the local source of truth for the catalog and "is this region on device" --
actually fetching DEM tiles and peak data over the network is platform
networking + CDN work that doesn't belong in this pass. Re-syncing the
catalog (`upsertMetadata`) is written to preserve an existing download
state rather than reset it, since a catalog refresh must not silently
un-download something the user already has.

**GPX is hand-rolled (string template writer, regex-based reader), not a
multiplatform XML library.** The shape Ridgeline itself needs is small and
fixed (`trk`/`trkseg`/`trkpt` with `lat`/`lon`/`ele`/`time`), and this avoids
taking on a new KMP dependency's iOS-compile risk in an environment that
can't verify iOS compiles at all. Trade-off: no CDATA support, and a
`<trkpt>` inside a comment would be misread. Real GPX exports (Strava,
Garmin, OsmAnd) use plain double-quoted attributes and unnested elements, so
this is judged good enough for v1 import.

**Bug caught by the full multiplatform build, not by `jvmTest` alone:**
`RegexOption.DOT_MATCHES_ALL` compiles fine for the `jvm()` target but isn't
part of `kotlin.text`'s *common* API (only the JVM actual adds it), so it
broke `compileCommonMainKotlinMetadata` -- the compile step that stands in
for "does this still work on every target," including iOS, on a host that
can't actually compile iOS. Fixed by matching `[\s\S]` instead of relying on
a dot-matches-newlines flag. Lesson for future :data/:ui work in this
sandbox: `./gradlew build` (or at minimum the metadata compile task), not
just `jvmTest`, is what catches a JVM-only stdlib API slipping into
commonMain.

**kotlinx-datetime added as a dependency** (the one new external dependency
in this pass) specifically for ISO-8601 timestamp formatting/parsing at the
GPX text boundary -- correct date/calendar math by hand was judged more
error-prone than pulling in the standard, JetBrains-maintained KMP library
built for exactly this. Everywhere else in `:data`, timestamps stay plain
epoch-millis `Long`, matching the schema.

---

## 2026-08-13 — :ui screen state from the design file

**Decided:** `:ui` now has design tokens (`Colors`, `Typography`) and plain
data-class screen state for all seven screens in `Ridgeline_Standalone.html`
-- Onboarding, Library, AR Viewfinder, Recording, Summary, Detail (Splash
has no real state, so no class for it). No Compose composables yet.

**What "use this [design file]" means here, precisely:** per the existing
rule (see "Design file peak data is decorative, not ground truth"), nothing
from the file's fabricated `PEAKS` array -- names, elevations, distances,
bearings, `xPct`/`bottomPx` pixel positions -- went into any real code path.
What *did* get used: colors, type scale, screen layout/structure, component
shapes, and the state machine each screen's interactive mockup already
encodes (`arState`, `recState`, `libState`, `obStep`, `endConfirm`,
`selectedPeakIdx`, etc.) -- all read directly off the markup and JS in the
file, which is design/UX information, not geodesy.

**`ArViewfinderState` holds `:engine`'s own types directly** --
`List<PlacedLabel>` and `VisiblePeak?` -- rather than a UI-local copy. This
is the one screen where engine and screen state are close enough that a
translation layer would be pure overhead: `layoutLabels()`'s output *is*
what the AR screen renders. `ArViewfinderStateTest.realEnginePipelineFeeds
DirectlyIntoScreenState` runs the actual `resolveVisiblePeaks -> project ->
layoutLabels` pipeline against real Kausani/Garhwal coordinates (the same
ones GarhwalCheck.kt/ConfidenceCheck.kt use) and checks the result lands in
`ArViewfinderState` correctly -- proof the wiring holds end to end, not just
that each piece compiles alone.

**Two screens (Summary, Detail) reference concepts `:data` doesn't persist
yet:** peak sightings during a track, and timestamped notes. Modeled as
UI-only types (`PeakSighting`, `TrackNote`) reusing `:engine`'s `Peak`
rather than inventing a third peak shape. This is a real gap the design
surfaced, not an oversight -- adding the corresponding tables/repository
methods to `:data` is follow-up work, tracked here rather than half-built
into this pass.

**Composables deliberately not written yet.** Screen state compiles and is
JVM-tested; actual Compose Multiplatform UI would be a bigger step (new
plugin, new dependency surface) that this environment cannot visually
verify at all (no emulator, no simulator, no browser preview). Writing a
large amount of unverifiable rendering code isn't a good trade against that
risk -- state modeling first, composables as an explicit next step once
someone can look at the result.

---

## 2026-08-13 — Camera preview and map view actuals

**Decided:** `:engine` and `:data` get a conditionally-added `androidTarget()`
(only when an Android SDK is findable). `:android` is a plain Jetpack
Compose application module (CameraX preview + MapLibre Android SDK map,
depending on `:engine`/`:data`) rather than Compose Multiplatform code
shared from `:ui`. iOS gets equivalent SwiftUI/AVFoundation/MapLibre-iOS
demo code under `ios/Sources/`, importing the Kotlin/Native `Engine`
framework directly.

**Why `androidTarget()` is conditional, and why that's harder than it looks:**
the Android Gradle Plugin needs network access (Google's Maven repo, via
Gradle's `google()`) just to *configure* -- unlike Kotlin/Native's iOS
targets, which disable themselves gracefully on a non-macOS host, AGP fails
the build outright without it. This environment's network policy blocks
`dl.google.com` (and `maven.google.com`, which redirects to it) entirely, so
AGP cannot be resolved here under any circumstances -- confirmed directly,
not assumed, before designing around it.

Gradle's Kotlin DSL `plugins {}` block does not see script-level `val`s (a
`val hasAndroidSdk = ...` computed above it fails with "Unresolved
reference" *inside* the block -- proven by testing it directly). It does,
however, accept a literal `if (System.getenv(...) != null || File(...).exists())`
expression written inline, and correctly skips resolving the plugin when
that's false. This was verified empirically in this exact sandbox (with and
without a fake `local.properties` present, run to run) before relying on it
project-wide -- see `engine/build.gradle.kts`. The same restriction
(no script-level vals) also blocks referencing AGP's `LibraryExtension`
type directly in a script where the plugin wasn't applied -- referencing an
unresolved type is a script *compile* error, independent of any runtime
`if`. Each module's `android.gradle.kts` sidesteps this by being loaded via
`apply(from = "android.gradle.kts")`, itself inside the `if` -- a script
that's never `apply`-ed is never compiled, so the type reference inside it
never has to resolve when there's no SDK.

**Why `:ui` did *not* get Compose Multiplatform, despite that being
CLAUDE.md's stated architecture:** tried it, and even the bare minimum
(`compose.runtime` alone, for the `jvm()` target -- nothing Android-specific)
fails to resolve, because Compose Multiplatform's common runtime has a
transitive dependency on `androidx.arch.core:core-common` and
`androidx.lifecycle:*`, which are *only* ever published to Google's Maven
repo, for every target, not just Android. Confirmed directly (the build
failure lists a chain through `org.jetbrains.compose.ui:ui-desktop`, i.e.
the *desktop/JVM* variant, not anything Android-specific). Adding it would
have made `:ui` -- including its already-written, already-passing screen
state tests from the previous session -- permanently unbuildable in this
environment. Rather than risk that regression for code that couldn't be
verified either way, camera/map demo screens went into `:android` (plain
`androidx.compose`, no multiplatform runtime) and plain SwiftUI on iOS
instead. `:ui`'s screen state is not wired into either demo.

**This is a sandbox-forced deviation, not a recommendation.** On a machine
with real access to Google's Maven repo, Compose Multiplatform in `:ui`
would work fine and is the architecture CLAUDE.md actually calls for.
Revisiting this once there's a working build to verify against is
reasonable; nothing here should be read as "don't do that."

**Everything platform-specific in this pass is unverified.** No Android SDK,
no macOS/Xcode host, in the environment that wrote it -- `:engine`'s and
`:data`'s conditional Android wiring was verified to at least *degrade
safely* (`./gradlew build` still passes with the SDK absent, confirmed
before and after every change), but the Android/iOS platform code itself
(CameraX, MapLibre, AVFoundation, the Kotlin/Native framework Swift import)
has had zero compiler pass over it. First real build happens on the
developer's machine.

---

## 2026-08-13 — :ui gets androidTarget()

**Decided:** `:ui`'s `build.gradle.kts` now adds `androidTarget()`
conditionally, gated exactly like `:engine`/`:data` (see "KMP module
structure and engine harness" and "Camera preview and map view actuals").
`:android` now depends on `project(":ui")` and consumes its screen state
(`OnboardingState`, `ONBOARDING_STEPS`) and design tokens (`Colors`,
`Typography`) directly instead of duplicating them.

**Why this doesn't reopen the CMP problem:** the previous entry ruled out
Compose Multiplatform in `:ui` because its *common runtime* has a hard
transitive dependency on androidx artifacts only published to Google's Maven
repo, which this sandbox cannot reach for any target. `androidTarget()` by
itself carries no such dependency -- it's plain Kotlin/Android, the same
mechanism `:engine` and `:data` already use safely. The two are independent:
`:ui` can target Android without becoming a Compose Multiplatform module.

**Still unverified in this sandbox** -- no SDK here either, same caveat as
`:engine`/`:data`'s `android.gradle.kts`. `./gradlew build` was confirmed to
still pass with the SDK absent (the new `if (hasAndroidSdk)` block never
evaluates), which is the only thing checkable here.

---

## 2026-08-13 — Splash and onboarding screens

**Decided:** `:android` gets real composables for the first two screens in
the design file instead of launching straight into the camera/map demo:
`SplashScreen` (fixed-dwell brand screen, no real init to gate on yet) and
`OnboardingScreen` (renders `:ui`'s `OnboardingState`/`ONBOARDING_STEPS`).
`MainActivity` now drives `Splash -> Onboarding (first launch only) -> Home`,
where Home is the existing camera+map demo screen, renamed in place rather
than moved. "Onboarding complete" is a single `SharedPreferences` boolean,
not a `:data` repository -- it's launch-flow state, not a trail/peak/region
record.

**Icons are standard Material glyphs, not the design file's SVG paths.**
The design file's onboarding icons (mountain outline, camera, download
arrow, running figure) are hand-drawn inline SVG with arc commands that
don't translate cleanly to Compose's `Path` API. Added
`androidx.compose.material:material-icons-extended` (version-managed by the
existing `compose-bom`, no new version to track) and used `Terrain`,
`PhotoCamera`, `CloudDownload`, `DirectionsRun` -- same concepts, standard
glyphs, no hand-traced Bezier math to get subtly wrong. Colors, layout,
copy, and the step/skip/dots state machine are still transcribed from the
design file per the existing rule (see ":ui screen state from the design
file") -- only the icon *paths* are swapped for a library equivalent.

**The manifest's forced `android:screenOrientation="userLandscape"` came
off the `<activity>` tag.** It predates this pass and was locking the
*entire app* to landscape, including these two new portrait screens --
correct for the demo screen alone (see "AR viewfinder defaults to
landscape"), wrong for Splash/Onboarding/Library, which stay portrait.
Landscape is now requested programmatically only while the camera/map demo
screen is on screen (`ComponentActivity.requestedOrientation`, set on
entry, reset to `UNSPECIFIED` on exit), with
`android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"`
added to the activity so that flip doesn't recreate `MainActivity` and lose
`RidgelineApp`'s in-memory route/onboarding state. This is a stopgap for a
single-Activity app with exactly one screen that wants a fixed orientation;
if more screens end up wanting one, a small `LocalActivity`/orientation
helper composable would be worth factoring out rather than repeating the
`DisposableEffect` per screen.

**Unverified in this sandbox**, same as everything else `:android` -- no SDK
here to compile against.
