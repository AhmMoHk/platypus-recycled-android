# Platypus Reclayed — Android (Native Port)

## 📖 About the Project
This project is an unofficial fan-made native Android port of the classic claymation shoot-'em-up **Platypus Reclayed**. It brings the full PC experience to Android devices with 1:1 gameplay, physics, timing, enemies, bosses, and weapons, complete with custom clay-style touch controls, widescreen support, high-refresh-rate options, and full Steam Workshop mod compatibility.

---

## 📥 Easy Installation (Pre-built APK)
Want to play the game right away on your phone without building the project yourself?
1. Go to the **[Releases](../../releases/latest)** page.
2. Scroll down to the **Assets** section at the bottom of the release notes.
3. Download **`PlatypusReclayed.apk`** and install it directly on your Android device!

*(Note: On the Releases page, you can also find `PlatypusAndroidKit.zip` if you want to easily build the APK using Unity on Windows with your own local Steam Workshop mods).*

---

## ⚙️ Technical Approach
The original game is built on Unity (2022.3, Mono). Instead of re-implementing the gameplay by hand, this repository's toolchain translates the game's own `.NET` assemblies to Java and runs them on a custom, hand-written reimplementation of the Unity engine. No proprietary game assets or source files are stored directly in the source tree—only the porting toolchain and the custom engine runtime.

```text
Platypus.dll, Platypus.Core.dll, MoonSharp.dll,      IL → Java translator        Java sources
UnityEngine.UI.dll, Unity.TextMeshPro.dll, …   ───►  (tools/cil)           ───►  (gen/java)
                                                                                      │
Unity scenes/assets (level*.unity, *.assets)   ───►  exporter (tools/export) ───► JSON + PNG + TTF
                                                                                      │
                                                     engine runtime (engine/java) ◄────┘
                                                     + Android shell → APK
```

---

## 📁 Repository Layout

| Path | What it is |
| :--- | :--- |
| `tools/cil/` | IL → Java static translator (Python): metadata reader, CIL decoder, class/method emitters, call & delegate handling, stub generation |
| `tools/export/` | Unity data exporter (`UnityPy`): scenes, prefabs, sprites, textures, fonts, settings → JSON/PNG/TTF |
| `tools/xlat.sh`, `tools/jcheck.sh` | Translate the assemblies and compile-check the result |
| `engine/java/cs/rt/` | .NET runtime support: struct/byref/enum model, .NET number formatting, .NET-identical sort, JSON |
| `engine/java/cs/System/` | BCL reimplementation (collections, LINQ, strings, IO, reflection over emitted metadata, .NET Random) |
| `engine/java/cs/UnityEngine/` | Unity engine reimplementation (see below) |
| `engine/java/cs/Rewired/` | Input facade replacing Rewired — touch controls mapped to the game's actions |
| `engine/java/cs/Blackdrop/` | Platform facade: local saves, achievements, locale, extra content (Workshop) |

---

## 🔄 Translator & Engine Architecture

### Translator
* `.NET` namespace `N` → Java package `cs.N`; nested `A/B` → `A$B`; generic `` `n `` → `_n`
* `structs` → classes with `$copy`/`$set`/`$clear` value semantics; `enums` → primitives; `byrefs` → `Ref*` cells
* `methods` → a `while(true) switch(pc)` dispatcher per exception region, with a typed symbolic stack
* `delegates` → classes with anonymous `$call` subclasses; multicast via an invocation list
* Reflection metadata (`$fmeta`/`$mmeta`/`$pmeta`/`$cattrs`) is emitted per class, so the game's own reflection, attributes, and Unity serialization keep working
* **1,595 classes** translate cleanly (only 2 methods unsupported); everything compiles against `android-23`

### Engine Runtime
Implemented systems include:
* **Object Model:** `GameObject`/`Transform`/`RectTransform` with real anchor layout.
* **Core Loop:** Player loop with Unity's exact ordering and `deltaTime` clamping, script execution order, coroutines (all yield types), `Invoke`, and `Instantiate`/prefab cloning with reference remapping.
* **Serialization & Events:** Scene & asset loading from exported data, Unity serialization into `$fmeta` fields, `UnityEvents` with persistent calls, and `AssetBundle` emulation.
* **2D Physics:** SAT narrow phase, kinematic contacts, and circle casts.
* **Renderer:** Sorting layers/groups, sprites, 9-slice, lines, trails, canvases with clipping, and a GLES/software backend.
* **Audio, Input & Fonts:** Audio clip decode + software mixer, multi-touch → actions, TrueType reader + SDF rasterizer + atlas packer for TextMeshPro, and full startup sequence (`RuntimeInitializeOnLoad` phases).

### Port Patches (`tools/cil/patches.py`)
Small, verified patches applied to the translated code (the build halts if a patch no longer matches):
* `CameraManager` letterboxing → the camera always covers the full display (`cs.rt.Port.cameraRect`).
* `GameUtils.AdjustXToWideScreen` / `AdjustOriginalPosToWideScreen` → the `±100` widescreen offset becomes `Port.wideOffset(aspect)`: exactly `100` at 16:9 (PC behaviour), and scaled larger on 19.5:9 / 20:9 / 21:9 phones so enemies spawn just off the real screen edge; player bounds come directly from the camera viewport.
* `BackgroundManager` clear-colour quad → drawn only on stages that clear the background, sized to the screen (Unity leaves its order against the sky to chance; on wide screens it previously showed as a white band).
* `DisplayPlayerLives` / `ShipPanelScript` → removed "press fire to join" prompts for a second player.

---

## 🎮 Port Decisions & Features
* **Single-Player Focused:** Second player and external controller checks are removed for a streamlined mobile experience.
* **Clay Touch Controls:** Custom clay joystick (follows the thumb 360° and snaps back, sitting flush against the left edge) + hold-to-fire button on the right, positioned neatly above the lives display.
* **True Widescreen & High FPS:** Fullscreen support at any aspect ratio with dynamic bounds and spawn margins, 4K (`HighEnd`) assets, and selectable frame rates (30 / 60 / 90 / 120 / 144 FPS).
* **Full Mod Support:** All extra content, including Steam Workshop items, appears directly in the Mods menu.
* **Optimized Storage:** Game data is converted once from the PC copy (ASTC 4x4 textures deflated losslessly on disk — reducing the main bundle from 2.0 GB to ~0.5 GB — plus Vorbis audio).

---

## 📱 Android Shell (`android/`)
* **`MainActivity`:** GL surface, frame loop with selected frame cap, audio thread (Java mixer → `AudioTrack`), immersive fullscreen including display cutout, and multi-touch routing.
* **`GlesGfx`:** GLES2 renderer featuring one program per game shader family (sprite with fog/emission/day-cycle/blink, UI, TextMeshPro SDF with outline/underlay, morph-text tints, multi-tint), stencil masks, `RectMask2D` scissor, and ASTC textures uploaded straight from disk.
* **`TouchControls`:** Clay joystick (red/yellow base, blue knob at 47.5% of the base that follows the thumb 360°, clamped, snaps back) and hold-to-fire button (scales to `0.92` while held); each control tracks its own pointer ID.
* **`Settings`:** Frame rate selection, HighEnd (4K) bundles, and 100% unlocked progression option (via the game's own save API).
* **`DataImport`:** First-launch installer that streams picked zip parts (`SAF`, requiring no storage permissions) into `getFilesDir()/data`, tracks completed parts, and cleans up old copies in `Android/data`.
* **APK Specs:** Min Android 8.0 (API 26), Target API 34, pure DEX (no native libraries), `resources.arsc` stored with `zipalign -p 4`, signed with v1 + v2 + v3.

---

## 🛠️ Building from Source

> **Tip:** If you just want to play the game, download the ready-made **`PlatypusReclayed.apk`** from the **[Releases](../../releases/latest)** section instead!

1. **Translate the game's assemblies from your install:**
   ```bash
   tools/xlat.sh # Outputs to gen/java and gen/stubs
   ```
2. **Build the APK:**
   ```bash
   android/build.sh # Outputs build/PlatypusRecycled.apk (javac + dx + aapt + zipalign + apksigner)
   ```
3. **Create the data pack from your Steam copy** (requires PC, Python 3, `UnityPy`, `astcenc`, and `ffmpeg`):
   ```bash
   python3 tools/export/convert_assets.py "<game>/Platypus_Data" out --workshop "<steam>/steamapps/workshop/content/<appid>"
   ```
4. **Package & Install:**
   Zip the pack into parts (each zip holding `data/...`), install the APK, launch it, and select the zip parts using the system file picker. The app unpacks them directly into internal storage with a progress bar.

### Testing & Maintenance Scripts
* **Desktop Harness:** Run the translated game headless on a PC for engine testing:
  ```bash
  desktop/run.sh [frames]
  # Example with software-rendered screenshots and key injection:
  JOPTS="-Dprr.shots=600,1200 -Dprr.input=600:key@13" desktop/run.sh
  ```
* **Upgrading Older Data Packs:**
  * `tools/export/deflate_astc.py <pack>` upgrades `.astc` → `.astcz`.
  * `tools/export/reread_monobehaviours.py <bundle> <marker>` re-reads a bundle's `MonoBehaviours` with its own type trees.

---

## ✅ Status
Verified in the desktop harness (software renderer) at 20:9 aspect ratio and on Android devices:
`Splash` → `Mod Loading (Official + Workshop)` → `Title` → `Profile` → `Mode/Difficulty` → `Gameplay (Ship, Enemies, HUD, Game Over)` with zero runtime errors.

---

*Disclaimer: Platypus Reclayed and all of its content belong to their respective owners. This project is an unofficial fan port and is not affiliated with or endorsed by the game's developers. Please support the official release by purchasing the game on Steam.*
