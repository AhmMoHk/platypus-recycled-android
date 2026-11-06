# Platypus Reclayed — real-Unity Android port (approved 2026-11-06)

User approved: AssetRipper export + real Unity 2022.3.62f2, Android ARM64 (IL2CPP). User builds on **Windows 11**
(Unity Hub + 2022.3.62f2 + Android Build Support). Deliver: project/port kit + `build_android.bat`.

## Tools here
- AssetRipper 2.0.0 binaries fetched via the user's repo CI (branch `tools-bin`, temp branch `ci-fetch-tools`;
  delete both when done). Linux build: /home/claude/ar/linux/AssetRipper.GUI.Free (headless web API:
  POST /Settings/Update, /LoadFolder Path=, /Export/UnityProject Path=). Source of 2.0.0: /home/claude/assetripper/assetripper
- NuGet and Unity downloads are blocked here (no Unity editor locally). dotnet 8 + 10 SDK installed (apt).
- Base game export (IgnoreStreamingAssets): /home/claude/ar/base/ExportedProject (767 MB, 875 assets, 2022.3.62f2)
  settings: ScriptExportMode=Decompiled, ShaderExportMode=Decompile (free build -> dummy shaders only)

## Findings
- Scripts decompiled per assembly under Assets/Scripts/: Assembly-CSharp, Platypus, Platypus.Core, Blackdrop.*,
  MoonSharp, Rewired_Core, Rewired_Windows*, Steamworks.NET, Unity.Microsoft.GDK*, XblPCSandbox, TMP, UGUI, SimpleJSON...
- UI/TMP script GUIDs are NOT the official package GUIDs (Image.cs 3cf5a444...), so keep the decompiled UGUI/TMP
  copies (or remap GUIDs) — don't just swap in packages.
- Custom shaders (Assets/Shader): Unlit_SpriteShader, Unlit_SpriteReflectionCombinedShader, Unlit_MorphTextShader,
  Unlit_MorphTextFlashShader, Unlit_MultiTintShader, Unlit_UIWorldSpaceTexture (+4 TMP). Bodies are dummies:
  rebuild from the DXBC in the game's Shader assets (disassemble SHEX + RDEF names) -> exact HLSL.
- Mods are AssetBundles in StreamingAssets/<mod>/Bundle/{HighEnd,LowEnd}/ built for Windows -> must be re-exported
  and rebuilt as Android bundles by the Unity build script. Loose mod files (Workshop: Data/GFX, modmeta.json) copy as-is.
- Android: StreamingAssets live inside the APK (not File.IO readable) -> mods need a real folder
  (persistentDataPath) — copy/install on first run or ship as OBB/data parts.
- Steam/Epic/GDK/Xbox platform assemblies must go; add an Android Blackdrop platform (local saves, no achievements UI).

## User requirements (attachment 2026-11-06 21:18)
1. 1:1 visuals/fonts/colors/audio; fix white menu boxes; waterfalls/clouds animate; cables visible; bullets hit
   safe/balloons/spikes/mines; respawn invincibility; 1.0x speed; unlock all; Workshop mods.
2. Remove Steam fully. Video menu: remove Windowed/Borderless/VSync/resolution; add quality Low/Medium/High/Ultra;
   FPS 30/60/90/120/144.
3. Direct touch on all menus; strip controller glyph icons/rings from labels ("Change color O" -> "Change color").
4. Joystick + fire ONLY in active gameplay (hidden in menus/settings/pause/level select); on-screen Pause button
   (opens original pause menu); Back buttons in all menus in the game's own font/style + Android back gesture.
   Assets: 1000045163.png ring base (bottom-left), 1000045164.png blue knob 45-50% (360°, snap back),
   1000045153.png crosshair fire (bottom-right, tap+hold, 0.92 press scale). Key out black.
5. Any aspect ratio (20:9, 19.5:9, 16:9, 16:10, 4:3) without stretching; HUD/menus/bounds adapt.

## Shader recovery (done 2026-11-06)
- tools: prr/shaders/dump_dxbc2.py (DXBC from Shader.compressedBlob), dxbc_dis.py (SM4 disassembler),
  annotate.py (adds Unity cbuffer names from m_ParsedForm) -> prr/shaders/hlsl_ref/*.dis.txt (local only, game code)
- Names come from the stage's m_CommonParameters; some programs keep their layout in the per-program parameter
  blob (m_ParameterBlobIndices) -> those cb slots are inferred (properties in declaration order, 16-byte rows).
- SpriteShader regular pass (exact): horizon = _UseCustomHorizonLine ? _CustomHorizonLine : _BaseHorizonLines[_Layer];
  discard if worldY(v2.y) < horizon or !_DoRegularPass; uv = v2.zw + _Scrolling.xy*_Time.y (clouds/waterfalls scroll);
  c = tex; fog = 1-exp(-_FogDistance*_FogDistanceMultiplier*_FogDensity); c.rgb = lerp(c.rgb,_FogColor.rgb,fog);
  c *= vcol; c = lerp(c, c*_PlatypusDayTint, _DayCycleMultiplier); blink = 1-saturate((_Time.y-_BlinkStart)/_BlinkDuration);
  c.rgb = lerp(c.rgb, 1, blink); out = c * (_EmissionPower.xxx, _Transparency).
- SpriteShader pass 2 = water reflection: 3 sine waves of _Time, _GlobalLayerReflections[_Layer], reflection colours
  _ReflectionColorBase/_ReflectionColorHorizon by v3.z, _MainTex_TexelSize.w>=64 check (cb0[21]) — transliterate.
- MorphText: k = smoothstep(thr-sm/2, thr+sm/2, (r+g+b)/3); out.rgb = lerp(_LowTint,_HighTint,k).rgb * tex.rgb * vcol.rgb;
  out.a = tex.a*vcol.a (no _TMPColor). MultiTint: per channel c in r,g,b: k_c = smoothstep(thr_c±sm_c/2, tex.c);
  out.rgb = saturate(sum tex.c * lerp(low_c,high_c,k_c)); a = tex.a*vcol.a. Blend SrcAlpha/OneMinusSrcAlpha, ZWrite on?, Cull back.

## Next steps
1. Write ShaderLab/HLSL for the 6 shaders (transliterate remaining programs) -> Assets/Shader overrides.
2. Export HighEnd mod bundles (Platypus Reclayed, Original, Spooky, Winter) with AssetRipper LoadFile into the project;
   tag assets with bundle names so the build script rebuilds them for Android.
3. C# changes (Android platform for Blackdrop, strip Steam/GDK/Epic/Xbox, touch controls, pause/back buttons,
   video menu, glyph stripping, FPS, quality presets, unlock all, aspect ratio). Compile-check with dotnet csc
   against Managed/*.dll (no NuGet).
4. Editor BuildScript.cs + build_android.bat (Unity -batchmode -executeMethod), first-run data install for mods.
