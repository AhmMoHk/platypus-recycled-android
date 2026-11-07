Platypus Reclayed - Android port kit (Unity 6 or 2022.3.62f2, IL2CPP ARM64)
==========================================================================

بالعربي (مختصر)
---------------
المطلوب على الحاسبة (Windows 11):
  - Unity Hub + Unity 6 (6000.0 LTS) ويا Android Build Support (ومعاه Android SDK & NDK و OpenJDK).
    Unity 6 بالرخصة المجانية يسمح نشيل شعار "Made with Unity". (2022.3.62f2 هم يشتغل بس الشعار يبقى.)
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
       - يلكه Unity 6 من Unity Hub (وإذا ماكو ياخذ 2022.3.62f2)، أو انطيه مسار Unity.exe.
       - أول مرة تاخذ وكت (30-90 دقيقة) لأن يحوّل كل الرسومات لـ ASTC. المرات الجاية أسرع.
       - الناتج:  %USERPROFILE%\PRA\PlatypusReclayed.apk
  4) انقل الـ APK للموبايل ونصّبه. أول تشغيل ينسخ ملفات المودات (~400 MB) مرة وحدة بشريط التحميل مال اللعبة.

تحديث الحزمة على مشروع موجود (بدون تصدير من جديد، ثواني):  setup_project.bat -Update   ثم build_android.bat
سجل اللعبة على الموبايل: Android/data/com.platypusport.reclayed/files/platypus_log.txt
  أو حط 4 أصابع على الشاشة ثانيتين وتطلع قائمة مشاركة السجل.

جودة الرسومات بالـ APK:  build_android.bat -Astc 4x4  (أوضح وأكبر)  |  6x6 (الافتراضي)  |  8x8 (أصغر)

English
-------
Needs: Windows, Unity Hub + Unity 6 (6000.0 LTS; 2022.3.62f2 also works but keeps the Made with Unity splash)
with Android Build Support (SDK & NDK, OpenJDK), your copy of Platypus Reclayed, ~25 GB free.

  setup_project.bat [GameFolder] [-OutDir D:\PRA] [-WorkshopDir ...] [-Force]
      Finds the game (argument, Steam default, every Steam library in libraryfolders.vdf, else a folder picker /
      typed path), exports it with AssetRipper 2.0.0 (downloaded, or tools\AssetRipper_win_x64.zip), copies the
      loose mod files and your Workshop items, removes Steam / Epic / Xbox / Rewired, adds the port and applies
      patches.json. The game folder is only read.
  setup_project.bat -Update
      Puts a newer kit into the project you already have (port files + patches only, no export).
  build_android.bat [Unity.exe] [-Astc 4x4|5x5|6x6|8x8]
      Batch-mode Unity build: player settings (IL2CPP ARM64, API 26+, target 34, landscape, GLES3, ASTC), Android
      rebuild of the Platypus Reclayed / Platypus Original bundles, StreamingAssets index, APK.
      Prefers Unity 6 (no splash), else 2022.3. Log: %USERPROFILE%\PRA\build_android.log
On the phone: Android/data/com.platypusport.reclayed/files/platypus_log.txt, or hold four fingers for two seconds
to share it.

What the port changes
  - Input: Rewired is replaced by a small stand-in (keyboard, any gamepad, touch). In menus a tap presses the button
    under the finger (finger down highlights it, finger up presses it; the game's Back / hint buttons too); the
    Android back gesture is Escape (back / pause). During play touches only drive the joystick, fire and pause.
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
  - Any screen shape: the picture fills the whole screen (the view widens with the screen, nothing is stretched);
    menus and the score stay in the game's own 16:9 frame. The intro films also fill the screen.
