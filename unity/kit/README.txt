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

الإعدادات الجديدة:
  - Video:  Quality (Low / Medium / High)  |  Screen (Original = 16:9 والباقي أسود، Fullscreen = 16:9 ممطوطة على كل الشاشة)
            |  FPS (30 / 60 / 90 / 120)
  - Gameplay > Controls (ونفسها بقائمة الإيقاف):  حجم أزرار اللمس، شفافيتها، شفافية زر الإيقاف.
  - إذا ضلت اللعبة على 60 فريم بموبايل شاومي: الإعدادات > الشاشة > معدل التحديث > مخصص 120Hz، وشيل اللعبة من
    Game Turbo إذا موجودة بيها.

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
to share it. Besides the game's own messages it has [Display] (screen, refresh rate), [Splash] (intro films), [Load]
(loading steps and scene progress), [Perf] (frame rate every 15 s) and [State] (cameras and canvases) lines.

What the port changes
  - Input: Rewired is replaced by a small stand-in (keyboard, any gamepad, touch). In menus a tap presses the button
    under the finger (finger down highlights it, finger up presses it; the game's Back / hint buttons too); the
    Android back gesture is Escape (back / pause). During play touches only drive the joystick, fire and pause.
  - Clay touch controls, only while playing (hidden in menus, options, pause, level select), see-through:
      Overrides\Assets\PlatypusAndroid\Resources\1000045163.png  joystick ring, fixed, bottom-left
      Overrides\Assets\PlatypusAndroid\Resources\1000045164.png  joystick knob, 47.5% of the ring, 360 deg, springs back
      Overrides\Assets\PlatypusAndroid\Resources\1000045153.png  fire, bottom-right, tap or hold
      Overrides\Assets\PlatypusAndroid\Resources\pause_button.png pause, top-right corner (tap)
    A held control turns darker and shrinks a little. Options > Gameplay > Controls (and the pause menu's options):
    Touch Controls Size 50-150% (default 100%), Touch Controls Opacity 10-100% (60%), Pause Button Opacity (70%).
    The mouse options (sensitivity, mouse hint, P2 mouse) are gone.
  - One player: player 2's HUD ("Press Fire To Join") and ship panel are gone. Ship screen: player 1 is in at once;
    the ships stand in a row (chosen one big, fully opaque, in the middle; neighbours smaller and see-through either
    side; all three of the game's ships at once), the arrows / a tap on a neighbour / a gamepad slide the row (ship packs with more ships too);
    "Change Color" colours every ship in the row; the stats sit at the far left; Back returns to level select.
  - No controller glyphs in texts ("Change color O" -> "Change color").
  - Video options:
      Quality  Low / Medium / High - the size the game is drawn at (Low 70% and at most 720 lines, Medium 85% and at
               most 1080 lines, High the screen's own size) and texture detail (Low: half-size where possible).
      Screen   Original = the game's 16:9 picture, the rest of the screen pure black (films, menus and play alike).
               Fullscreen = the same 16:9 picture stretched over the whole screen, as a PC emulator does; everything
               stretches together and taps still land where they are drawn.
      FPS      30 / 60 / 90 / 120; the phone's display is switched to a matching refresh rate.
    No resolution, window mode or VSync rows.
  - Smooth frames: incremental garbage collection, Android Frame Pacing, buffered logging, no HDR buffer on Low.
    The app is not filed as a game (android:isGame / appCategory), so phone game boosters do not hold it at 60 Hz;
    the refresh rate is asked for through Unity, the window's display mode and the game surface, and again
    whenever the display drops below it. The game already blends its 60-per-second logic between frames,
    so 90 / 120 FPS is real motion. The log gets a [Perf] line every 15 s (frame rate, slowest frame).
  - Sprites: dynamic batching on (Unity bakes each sprite's colour, transparency and mirroring into its vertices,
    which the game's shaders read; Unity 6 had it off), and animated sprites keep the texture of the picture they
    show (Unity 6 drew a stale texture from the property block: clouds with the moon's texture, blue squares, odd
    explosions).
  - A black backdrop is drawn first every frame (no leftover garbage / TV static around the intro films); the last
    picture of the Claymatic film stays behind the first loading bar, as on PC.
  - Asset bundles go into the APK as "<name>.unity3d", which Android stores uncompressed, so they load in place
    (Unity 6 compressed the bare names and loading the main menu took minutes). The build log lists how the biggest
    APK files are stored; a bundle that is still compressed is copied out on the first start instead.
  - While a loading screen is up Unity gets more time per frame for loading (shorter loads).
  - Progress as in the original: only level 1 open at first, Nasty locked until earned (Options > Unlock All Content
    still opens everything). Reset Progress keeps the screen, quality and touch options.
  - Steam, achievements service and Workshop client removed (Workshop items are bundled).
  - The game's six custom shaders rebuilt from its compiled shaders; real TextMeshPro shaders.
