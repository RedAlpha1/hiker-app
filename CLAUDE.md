# Ridgeline

Trail and run recording with live AR peak identification. Android and iOS from
one Compose Multiplatform UI. Offline-first, built for the field: Himalayan
sunlight, gloves, all-day battery.

**v1 scope:** Uttarakhand and Himachal peaks only, Indian users. AR ships in v1.
Solo developer, no deadline — correctness over speed.

## Before you change anything

Read `DECISIONS.md`. It records *why* each architectural choice was made, and
several of them are counter-intuitive enough that they look like bugs. In
particular, do not "fix" any of the following without reading the entry first:

- `VisibilityConfig.maxRangeM` is 150km, not the 60km that suits Alpine
  viewpoints. Lowering it discards most of the visible skyline from Indian
  viewpoints.
- Label lanes are absolute horizontal bands, not offsets from each peak's own
  anchor. Per-peak offsets let chips in different lanes collide.
- Curvature and refraction correction is not optional at these ranges. At 126km
  the earth drops the target over a kilometre.

## Architecture

Shared logic in `commonMain`; platform code only where it must be.

```
:engine     pure Kotlin — geodesy, DEM abstraction, occlusion, projection,
            label layout. No platform imports. Fully unit-testable on JVM.
:data       SQLDelight, repositories, region download, GPX
:ui         Compose Multiplatform screens
:android    actuals + foreground service
:ios        actuals + Swift interop
```

**expect/actual boundaries:** location, device attitude, camera preview,
background execution, map view. Everything else is shared.

**Two native islands:** the map (MapLibre Native via `AndroidView` /
`UIKitView`) and the camera preview. CMP overlays draw on top of both. CMP has
no map story — do not look for one.

## Engine performance contract

`resolveVisiblePeaks` is expensive: a 150km raycast at 40m steps is ~3,750 DEM
lookups per peak. It runs **on position change** (roughly every 50m), never per
frame.

`project` is cheap trigonometry over a handful of sightings. It runs per frame.

Do not move raycasting into the frame loop. Do not cache projections across
position changes.

## Data

- **Elevation:** Copernicus GLO-30. ~2–4m RMSE in mountainous terrain, which is
  why `terrainToleranceM` exists.
- **Peaks:** hand-curated. OSM coverage of the Indian Himalaya is thin.
  Transliteration is not standardised — Chaukhamba/Chaukhambha,
  Trisul/Trishul — so `Peak` carries `nameLocal` (Devanagari) and `aliases`.
  Match on any variant.
- **No backend, no accounts.** Local-only. Region tiles are static files on a
  CDN. Weather is a third-party API.
- **Keep sync deferrable:** repository interfaces over all data access,
  client-generated UUID keys, `createdAt`/`updatedAt` on every row, soft
  deletes. Adding sync later must not become a migration.

## Do not use the design file as ground truth

`Ridgeline_Standalone.html` contains fabricated coordinates, bearings and
distances. Its bearings decrease monotonically because peaks were laid out
left-to-right across the mockup frame. Never lift a number from it into a test
fixture. Real coordinates for testing live in `engine/test/`.

## Testing

`engine/test/Harness.kt` asserts against real Pennine Alps geometry.
`GarhwalCheck.kt` and `ConfidenceCheck.kt` cover the Indian viewpoints.

Every geometry change must keep the harness green. When adding a case, anchor it
to a real viewpoint and real summits — synthetic-only tests have already missed
one label-collision bug that real coordinates caught.

## Conventions

- Comments explain *why*, not *what*. The non-obvious constraints above are the
  reason this codebase is worth commenting at all.
- Screen state as plain data classes; the design is still exploratory and will
  change.
- Feature-based module boundaries, not layer-based. No shared abstractions
  across features until a third caller appears.

## Open questions

- Indian geospatial regulation for downloadable DEM tiles covering LAC-adjacent
  terrain. Needs qualified legal review before the region pipeline ships.
- Haze thresholds (80km/120km) are estimates. Tune against photographs from
  Kausani and Chopta across seasons.
- Compass accuracy in the field is unmeasured. A heading-stability spike on a
  real device should happen before the AR UI is designed around it.
