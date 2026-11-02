# Platypus Reclayed -> native Android (personal port of the user's own Steam copy)

## Source files (extracted)
- Game: /home/claude/prr/x/steamapps/common/Platypus Reclayed  (Unity 2022.3.62f2, Mono, game version 1.4.0)
- Workshop (app 2912430): /home/claude/prr/x/steamapps/workshop/content/2912430/{3617166662 Ship Expansion Pack,
  3664262198 Casinopolis Ships, 3720350725 Perry the Platypus!?}  (add-on mods: ships/skins/entities json + GFX png)
- Archive: /home/claude/prr/steamapps.rar (inner, complete); unrar built from source at /usr/local/bin/unrar-rarlab
- IL disassembly (monodis): /home/claude/prr/il/{Platypus,Platypus.Core,Assembly-CSharp,Tools}.il
  (Platypus.dll 695 classes/3991 methods = engine; MoonSharp.dll = Lua; SimpleJSON.dll; Rewired = input)
- No C# decompiler available (nuget/github blocked); dotnet-sdk-8.0 + mono-utils (monodis) installed via apt.

## Game structure
- Everything is a "mod" in Platypus_Data/StreamingAssets/<mod>/: modmeta.json, Data/*.json, *.plat level scripts,
  Behaviours/*.lua (MoonSharp), GFX/**.png, SFX. Official mods: Platypus Reclayed (main), Platypus Original
  (classic, isLegacy), add-ons All Fish / All Silly Weapons / No Silly Weapons, DLC-like Spooky Platypus, Winter Platypus.
- Main content packed as Unity AssetBundles: <mod>/Bundle/{HighEnd,LowEnd}/<mod name> (UnityFS LZ4HC).
  HighEnd = 4K (2.93 GB; 712 Texture2D mostly DXT5 (fmt 12), many 8192x8192 atlases; 5687 Sprites; 112 AudioClips;
  TextAssets *.plat.asset, content.asset, meta.asset). LowEnd = 1080p (846 MB).
- .plat = level script: metadata(...) then lines `wait(N); command("arg": value, ...);` (~120 commands, see *Command classes)
- types.lua = Lua API (SpawnEntityWorld/Local/Child, GetEntity, GetPlayer, ...).
- Unity scenes level0..13 + sharedassets*.assets: menus/UI/fonts/shaders/audio (sharedassets8.resource = audio).

## Decisions
- Single player only; touch controls (clay joystick/knob/fire from the Platypus II port art/).
- Full screen any aspect; 4K (HighEnd) assets; FPS setting 60/90/120/144.
- Workshop mods listed in the Mods menu like the official ones, without the Steam badge.
- Size: APK small; game data imported on the phone from the user's own game folder (one-time conversion to GPU
  formats ETC2/ASTC + LZ4), drop LowEnd duplicates, Windows binaries, Mono, plugins.

## Stages
1. Asset pipeline + engine core (rendering layers/tiles/sprites, .plat interpreter, player ship, touch controls, shooting)
2. Entities/behaviours (port from IL), weapons, bosses, collisions, Lua (port MoonSharp or own Lua 5.2 interpreter)
3. Menus/HUD/settings (FPS, quality), single-player flow, saves, high scores
4. Mods menu (official + workshop), add-ons

## Architecture (decided 2026-11-01/02)
- **IL->Java translator** (`re/cil/`): md.py (metadata via dnfile + own signature/body decoding, properties,
  custom attribute decoding), il.py (decoder), xlat.py (classes, mangling, bridges, reflection metadata $fmeta/$mmeta/
  $pmeta/$cattrs, enums, delegates), method.py (typed symbolic stack, while/switch dispatcher per region, try/catch/
  finally), calls.py (calls, newobj, delegates via anonymous $call subclasses, byref copy-in/out), jtypes.py (naming,
  ext enums/structs/delegates), stubs.py (compile-only stubs for missing runtime classes into gen/stubs).
  Run: `tools/xlat.sh` -> gen/java (1213 classes: Platypus, Platypus.Core, Tools, MoonSharp, AdvancedPolygonCollider).
  Check: `tools/jcheck.sh` (javac of gen/java + gen/stubs + rt/java) -> build/jcheck/errors.txt
- Conventions: namespace N -> package cs.N (global -> cs.global); nested A/B -> A$B; generic `n -> _n;
  structs = classes with $copy/$set/$clear/$newArray + cs.rt.Struct; enums = primitives (+ class with $info EnumInfo);
  boxed enums = cs.rt.BoxedEnum; byref = cs.rt.RefX cells (struct byref = object); generic methods get leading
  cs.System.Type args; game ctors = $ctor_<Simple>(); external types use Java ctors (+ $init for base(args));
  Unity messages called through hooks $Awake/$Start/$Update...(return Object); mapped BCL instance methods ->
  static helpers cs.System.StringX/ObjectX/Int32X/...; enum-param string overloads suffixed Cmp/Opt;
  IList.Add->IList_Add, HashSet.Add->Add$bool.
- **Runtime** `rt/java/`: cs.rt (Rt, Ref*, Struct, BoxedEnum, EnumInfo, Fmt (.NET formatting), IntroSort (.NET sort
  order), MDArray), cs.System (BCL: collections, LINQ deferred, strings, IO, reflection on metadata, Random (.NET algo)...).
  Android constraint: compile against android-23 jar, dx only (no lambdas/streams/java.util.Base64/String.repeat).
- Scenes: tools `re/tools/scene_dump.py` (UnityPy + TypeTreeGenerator) -> scenes/*.json. Scenes use Unity UI heavily
  (TMP, Image, PText, layout groups, Button w/ UnityEvents). GameplayScene = level5.
- Next: Unity runtime (object model, scene/prefab instantiation from exported JSON, player loop, coroutines, Time),
  desktop headless harness, renderer (GLES + desktop software for screenshots), physics2D, audio, input (Rewired facade
  for touch), platform store (saves), MoonSharp boot (UserData.DefaultAccessMode=Reflection).
