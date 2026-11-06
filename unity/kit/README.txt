Platypus Reclayed - Android port kit (Unity 2022.3.62f2, IL2CPP ARM64)
=======================================================================

بالعربي (مختصر)
---------------
المطلوب على الحاسبة (Windows 11):
  - Unity Hub + Unity 2022.3.62f2 ويا Android Build Support (ومعاه Android SDK & NDK و OpenJDK).
  - نسختك من Platypus Reclayed على Steam (أو أي مجلد بيه Platypus.exe و Platypus_Data).
  - حوالي 25 GB فراغ.

الخطوات:
  1) فك الضغط عن هذا المجلد بأي مكان.
  2) شغّل setup_project.bat
       - يدور اللعبة بمكتبات Steam تلقائياً. إذا ما لقاها يفتحلك نافذة تختار منها مجلد اللعبة،
         أو تكدر تكتب المسار بنفسك، أو تعطيه المسار مباشرة:
           setup_project.bat "E:\Games\Platypus Reclayed"
       - يسوي المشروع بـ  %USERPROFILE%\PRA\Project  (تكدر تغيّره بـ -OutDir "D:\PRA").
       - ياخذ عناصر الـ Workshop مالتك تلقائياً إذا موجودة.
  3) شغّل build_android.bat
       - يلكه Unity 2022.3.62f2 من Unity Hub، أو انطيه مسار Unity.exe.
       - أول مرة تاخذ وكت (30-90 دقيقة) لأن يحوّل كل الرسومات لـ ASTC. المرات الجاية أسرع.
       - الناتج:  %USERPROFILE%\PRA\PlatypusReclayed.apk
  4) انقل الـ APK للموبايل ونصّبه. أول تشغيل ينسخ ملفات المودات (~400 MB) مرة وحدة بشريط التحميل مال اللعبة.

جودة الرسومات بالـ APK:  build_android.bat -Astc 4x4  (أوضح وأكبر)  |  6x6 (الافتراضي)  |  8x8 (أصغر)

English
-------
Needs: Windows, Unity Hub + Unity 2022.3.62f2 with Android Build Support (SDK & NDK, OpenJDK), your copy of
Platypus Reclayed, ~25 GB free.

  setup_project.bat [GameFolder] [-OutDir D:\PRA] [-WorkshopDir ...] [-Force]
      Finds the game (argument, Steam default, every Steam library in libraryfolders.vdf, else a folder picker /
      typed path), exports it with AssetRipper 2.0.0 (downloaded, or tools\AssetRipper_win_x64.zip), copies the
      loose mod files and your Workshop items, removes Steam / Epic / Xbox / Rewired, adds the port and applies
      patches.json. The game folder is only read.
  build_android.bat [Unity.exe] [-Astc 4x4|5x5|6x6|8x8]
      Batch-mode Unity build: player settings (IL2CPP ARM64, API 26+, target 34, landscape, GLES3, ASTC), Android
      rebuild of the Platypus Reclayed / Platypus Original bundles, StreamingAssets index, APK.
      Log: %USERPROFILE%\PRA\build_android.log

What the port changes
  - Input: Rewired is replaced by a small stand-in (keyboard, any gamepad, touch). In menus the touch screen is the
    game's own mouse control (tap = click, the game's Back / hint buttons are tappable); the Android back gesture is
    Escape (back / pause).
  - Clay touch controls, only while playing (hidden in menus, options, pause, level select):
      Overrides\Assets\PlatypusAndroid\Resources\1000045163.png  joystick ring, fixed, bottom-left
      Overrides\Assets\PlatypusAndroid\Resources\1000045164.png  joystick knob, 47.5% of the ring, 360 deg, springs back
      Overrides\Assets\PlatypusAndroid\Resources\1000045153.png  fire, bottom-right, tap or hold, shrinks to 0.92
      plus a Pause button (top-right) that opens the game's pause menu.
  - No controller glyphs in texts ("Change color O" -> "Change color").
  - Video options: Quality (Low / Medium / High / Ultra) and FPS 30 / 60 / 90 / 120 / 144; no resolution,
    window mode or VSync.
  - Everything unlocked; Steam, achievements service and Workshop client removed (Workshop items are bundled).
  - The game's six custom shaders rebuilt from its compiled shaders; real TextMeshPro shaders.
  - Any screen shape: the game keeps its 16:9 picture (4:3 for Platypus Original) with bars, never stretched;
    the touch controls use the whole screen (safe area).
