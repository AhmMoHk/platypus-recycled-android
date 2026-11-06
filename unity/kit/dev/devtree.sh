#!/bin/bash
# Build a patched copy of the decompiled Scripts tree for compile checks: drop removed assemblies, overlay the kit.
SRC=/home/claude/ar/full/ExportedProject/Assets/Scripts
DST=/home/claude/kit/dev/Scripts
KIT=/home/claude/kit/PlatypusAndroidKit/Overrides/Assets/Scripts
rm -rf "$DST"; mkdir -p "$DST"
for a in "$SRC"/*/; do
  n=$(basename "$a")
  case "$n" in Rewired_Windows|Rewired_Windows_Functions|com.rlabrecque.steamworks.net|Blackdrop.Platforms.Steam|Blackdrop.Platforms.EpicGames|Unity.Microsoft.GDK|Unity.Microsoft.GDK.Tools|XblPCSandbox) continue;; esac
  cp -r "$a" "$DST/"
done
rm -rf "$DST/Rewired_Core"/*/ "$DST/Rewired_Core"/*.cs "$DST/Assembly-CSharp/Rewired"
cp -r "$KIT"/. "$DST/"
python3 /home/claude/kit/tools/apply_patches.py "$DST" 2>&1 | tail -3
# runtime scripts outside asmdef folders compile into Assembly-CSharp (Editor/ folders are editor-only, skipped here)
mkdir -p "$DST/Assembly-CSharp/_PlatypusAndroid"
find /home/claude/kit/PlatypusAndroidKit/Overrides/Assets/PlatypusAndroid -name '*.cs' -not -path '*/Editor/*' -exec cp {} "$DST/Assembly-CSharp/_PlatypusAndroid/" \;
