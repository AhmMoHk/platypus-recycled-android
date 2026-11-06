# Platypus Recycled — Android (native port)

Personal Android port of **Platypus Reclayed** (Unity 2022.3, Mono), built from the owner's own legally
purchased copy. **No game assets, code or data from the game are stored in this repository** — only the
porting toolchain and the engine runtime written for it. You need your own copy of the game to build a
playable APK.

## Approach

The game is Unity + Mono, so instead of re-implementing the gameplay by hand the port **translates the
game's own .NET assemblies to Java** and runs them on a hand-written reimplementation of the Unity engine.
That keeps gameplay, physics, timing, enemies, bosses and weapons 1:1 with the PC version.

```
Platypus.dll, Platypus.Core.dll, MoonSharp.dll,      IL → Java translator        Java sources
UnityEngine.UI.dll, Unity.TextMeshPro.dll, …   ───►  (tools/cil)           ───►  (gen/java)
                                                                                      │
Unity scenes/assets (level*.unity, *.assets)   ───►  exporter (tools/export) ───► JSON + PNG + TTF
                                                                                      │
                                                     engine runtime (engine/java) ◄────┘
                                                     + Android shell → APK
```

## Layout

| Path | What it is |
|---|---|
| `tools/cil/` | IL → Java static translator (Python): metadata reader, CIL decoder, class/method emitters, call & delegate handling, stub generation |
| `tools/export/` | Unity data exporter (UnityPy): scenes, prefabs, sprites, textures, fonts, settings → JSON/PNG/TTF |
| `tools/xlat.sh`, `tools/jcheck.sh` | translate the assemblies; compile-check the result |
| `engine/java/cs/rt/` | .NET runtime support: struct/byref/enum model, .NET number formatting, .NET-identical sort, JSON |
| `engine/java/cs/System/` | BCL reimplementation (collections, LINQ, strings, IO, reflection over emitted metadata, .NET `Random`) |
| `engine/java/cs/UnityEngine/` | Unity engine reimplementation (see below) |
| `engine/java/cs/Rewired/` | input facade replacing Rewired — touch controls mapped to the game's actions |
| `engine/java/cs/Blackdrop/` | platform facade: local saves, achievements, locale, extra content (Workshop) |

## Translator

* .NET namespace `N` → Java package `cs.N`; nested `A/B` → `A$B`; generic `` `n `` → `_n`
* structs → classes with `$copy/$set/$clear` value semantics; enums → primitives; byrefs → `Ref*` cells
* methods → a `while(true) switch(pc)` dispatcher per exception region, with a typed symbolic stack
* delegates → classes with anonymous `$call` subclasses; multicast via an invocation list
* reflection metadata (`$fmeta/$mmeta/$pmeta/$cattrs`) is emitted per class, so the game's own reflection,
  attributes and Unity serialization keep working
* 1595 classes translate cleanly (2 methods unsupported); everything compiles against android-23

## Engine runtime

Implemented: object model (GameObject/Transform/RectTransform with the real anchor layout), the player loop
with Unity's exact ordering and `deltaTime` clamping, script execution order, coroutines (all yield types),
`Invoke`, Instantiate/prefab cloning with reference remapping, scene & asset loading from the exported data,
Unity serialization into `$fmeta` fields, UnityEvents with persistent calls, 2D physics (SAT narrow phase,
kinematic contacts, circle casts), the renderer (sorting layers/groups, sprites, 9-slice, lines, trails,
canvases with clipping and a GLES/software backend), audio (clip decode + software mixer), input (multi-touch
→ actions), fonts (TrueType reader + SDF rasterizer + atlas packer for TextMeshPro), AssetBundle emulation,
and the startup sequence (`RuntimeInitializeOnLoad` phases).

## Port decisions

* single player only — the second player and controller checks are removed
* touch controls: clay joystick (knob follows the thumb 360°, snaps back) + hold-to-fire button
* true fullscreen at any aspect ratio, dynamic bounds and spawn margins
* 4K (HighEnd) assets; frame rate selectable 60 / 90 / 120 / 144
* all content, including Steam Workshop items, listed in the Mods menu — without the Steam badge
* game data is converted once on the phone from your own copy (GPU texture formats + Vorbis), so the APK
  stays small

## Android shell (`android/`)

* `MainActivity` — GL surface, frame loop with the selected frame cap (60/90/120/144), audio thread
  (Java mixer → `AudioTrack`), immersive fullscreen including the display cutout, multi-touch routing
* `GlesGfx` — GLES2 renderer: one program per game shader family (sprite with fog/emission/day-cycle/blink,
  UI, TextMeshPro SDF with outline/underlay, morph-text tints, multi-tint), stencil masks, RectMask2D scissor,
  ASTC textures uploaded straight from disk
* `TouchControls` — clay joystick (red/yellow base, blue knob at 47.5% of the base that follows the thumb 360°,
  clamped, snaps back) and the hold-to-fire button (scales to 0.92 while held); each control tracks its own pointer id
* `Settings` — frame rate, HighEnd (4K) bundles, 100% unlocked progression (through the game's own save API)
* `DataImport` — one-time copy of the converted data folder with the system folder picker

## Building

1. Translate the game's assemblies from your install: `tools/xlat.sh` → `gen/java`, `gen/stubs`
2. Build the APK: `android/build.sh` → `build/PlatypusRecycled.apk` (javac + dx + aapt + apksigner; min Android 7.0)
3. Make the data pack from your own copy (PC, Python 3 + UnityPy + astcenc + ffmpeg):
   `python3 tools/export/convert_assets.py "<game>/Platypus_Data" out --workshop "<steam>/steamapps/workshop/content/<appid>"`
4. Install the APK, start it, pick the `out` folder when asked.

`desktop/run.sh [frames]` runs the translated game headless on a PC for testing the engine.
