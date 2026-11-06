#!/bin/bash
# Build the APK: engine runtime + translated game code + Android shell.
#   tools/xlat.sh must have produced gen/java and gen/stubs first.
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK=/usr/lib/android-sdk
BT=$(ls -d $SDK/build-tools/*/ | head -1)
JAR=$SDK/platforms/android-23/android.jar
OUT=$ROOT/build/apk
rm -rf "$OUT" && mkdir -p "$OUT/classes" "$OUT/dex"
find "$ROOT/rt/java" "$ROOT/gen/java" "$ROOT/gen/stubs" "$ROOT/android/src" -name '*.java' > "$OUT/sources.txt"
echo "javac: $(wc -l < "$OUT/sources.txt") files"
javac -nowarn -encoding UTF-8 -source 8 -target 8 -J-Xmx6g -bootclasspath "$JAR" -cp "$JAR" \
      -d "$OUT/classes" @"$OUT/sources.txt" 2> "$OUT/javac.log" || { grep -B1 -A3 "error:" "$OUT/javac.log" | head -80; exit 1; }
echo "dx"
"$BT/dx" -JXmx6g --dex --multi-dex --min-sdk-version=24 --output="$OUT/dex" "$OUT/classes"
echo "aapt"
"$BT/aapt" package -f -M "$ROOT/android/AndroidManifest.xml" -S "$ROOT/android/res" -A "$ROOT/android/assets" \
      -I "$JAR" -F "$OUT/app.unaligned.apk"
(cd "$OUT/dex" && "$BT/aapt" add "$OUT/app.unaligned.apk" classes*.dex > /dev/null)
"$BT/zipalign" -f 4 "$OUT/app.unaligned.apk" "$OUT/app.aligned.apk"
KS="$ROOT/android/debug.keystore"
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -storepass android -keypass android -alias prr \
      -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Platypus Recycled" > /dev/null 2>&1
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android --ks-key-alias prr \
      --out "$ROOT/build/PlatypusRecycled.apk" "$OUT/app.aligned.apk"
ls -la "$ROOT/build/PlatypusRecycled.apk"
