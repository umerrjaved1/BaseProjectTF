# Seven Wonders 360° View — Design

Date: 2026-06-28
Status: Approved (pending spec review)

## Goal

When a user taps a wonder card in the Seven Wonders screen, the app downloads
that wonder's 360° equirectangular panorama image (if not already cached), saves
it to internal storage, and opens an immersive 360° viewer the user can pan with
touch and device gyroscope.

## Scope

In scope:
- Add a 360° panorama image URL per wonder.
- Whole-card click → download (if needed) → save → open 360° viewer.
- Download via OkHttp (already a dependency) to internal storage; cache so a
  second open is instant and offline.
- Progress UI during download; error handling for failed downloads / no network.
- A self-contained 360° viewer activity (OpenGL ES sphere renderer) with
  touch-drag panning and gyroscope panning.

Out of scope (not now):
- VR / Cardboard stereo mode.
- Delivering URLs via Firebase Remote Config (kept as a future option; URLs live
  in code for now as placeholders).
- Pinch-to-zoom / FOV control (can be a follow-up).

## Decisions

1. **Image source:** Remote URLs, one per wonder, stored as a `panoramaUrl`
   field on the model. Real URLs to be supplied by the user; placeholders used
   until then. (Remote Config is a future swap-in — the download layer doesn't
   care where the URL comes from.)
2. **Viewer:** Self-contained OpenGL ES 2.0 sphere renderer — no third-party
   panorama library. Rationale: maintained equirectangular-360° Android
   libraries are scarce/stale; a sphere renderer is well-understood, has no
   external dependency risk, and gives true 360° projection. Panning via touch
   drag + gyroscope (`TYPE_ROTATION_VECTOR`).
3. **Click target:** Whole card. Matches the existing `WondersSliderAdapter`
   `onClick` pattern and keeps the card layout unchanged.
4. **Storage:** Internal app storage (`filesDir/wonders360/`). No runtime
   permission needed (minSdk 26). Files are private to the app.

## Architecture

```
SevenWondersActivity
  │  builds List<WonderItem> (now with panoramaUrl)
  │  passes onWonderClick callback to adapter
  ▼
SevenWondersAdapter
  │  itemView.setOnClickListener → onWonderClick(wonder)   (no I/O in adapter)
  ▼
SevenWondersActivity.onWonderClick(wonder)
  │  lifecycleScope coroutine:
  │    - PanoramaRepository.getOrDownload(wonder) → File
  │      • if cached file exists → return it
  │      • else OkHttp GET → stream to filesDir/wonders360/<key>.jpg (atomic via .tmp rename)
  │    - show progress dialog while downloading; dismiss on done
  │    - on success → startActivity(Panorama360Activity, filePath)
  │    - on failure → Toast/snackbar, delete partial file
  ▼
Panorama360Activity
  │  GLSurfaceView + Panorama360Renderer (sphere, equirectangular texture)
  │  SensorManager (ROTATION_VECTOR) + touch listener → camera yaw/pitch
  │  back button to exit
```

### Components (each independently testable)

- **`WonderItem.Wonder`** — gains `panoramaUrl: String`. Pure data.
- **`PanoramaRepository`** — `suspend fun getOrDownload(key: String, url: String): Result<File>`.
  Owns cache-path resolution and OkHttp download. No Android UI. Depends on
  OkHttpClient + filesDir. Testable with a fake server / temp dir.
- **`SevenWondersAdapter`** — gains `onWonderClick: (WonderItem.Wonder) -> Unit`
  constructor param. Stays UI-only; no networking.
- **`Panorama360Activity`** — hosts the GLSurfaceView and wires sensors/touch.
- **`Panorama360Renderer`** — `GLSurfaceView.Renderer`; builds a UV sphere,
  uploads the bitmap as a texture, renders from a yaw/pitch camera. Exposes
  `setYaw/ setPitch`. Self-contained GL; no Android framework beyond GLES.
- **`PanoramaTouchController` / sensor glue** — converts drag deltas and
  rotation-vector readings into yaw/pitch on the renderer.

## Data flow / cache key

Cache key = a stable slug of the wonder title (e.g. `great_wall`). File:
`filesDir/wonders360/great_wall.jpg`. Download writes to `great_wall.jpg.tmp`
then renames on success so a half-written file is never treated as cached.

## Error handling

- No network / download fails → toast "Couldn't load 360° view", remove any
  `.tmp`, do not open viewer.
- Corrupt/undecodable image → treated as failure; cached file deleted so a
  retry re-downloads.
- Double-tap while downloading → guard with a per-key in-flight flag so we don't
  download twice or open twice.
- GL not supported (very old device) → minSdk 26 guarantees GLES 2.0, so no
  fallback path needed.

## Testing

- `PanoramaRepository`: unit test with MockWebServer (OkHttp) + temp dir —
  downloads to file, returns cached file on second call, deletes partial on
  error.
- Slug/cache-key function: pure unit test.
- Renderer math (yaw/pitch → view matrix, sphere vertex generation): pure unit
  test where feasible.
- Manual: tap each wonder, confirm download once, instant re-open, pan with
  touch and by physically rotating the device, rotation, back navigation.

## New dependency

None required for the viewer. OkHttp, Glide, coroutines/lifecycle already
present. (Coroutines come via lifecycle-runtime-ktx; confirm
`lifecycleScope`/`Dispatchers.IO` are available — add `kotlinx-coroutines` only
if missing.)

## Open items for user

- Provide the 8 real 360° equirectangular image URLs (or confirm placeholders
  for now).
