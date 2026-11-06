#!/bin/bash
# Desktop headless run of the translated game (for testing the engine without a phone)
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT=$ROOT/build/desktop
if [ "$1" != "--norebuild" ]; then
  rm -rf "$OUT" && mkdir -p "$OUT"
  find "$ROOT/rt/java" "$ROOT/gen/java" "$ROOT/gen/stubs" "$ROOT/desktop/src" -name '*.java' > "$OUT/sources.txt"
  javac -nowarn -encoding UTF-8 -J-Xmx6g -d "$OUT" @"$OUT/sources.txt" 2> "$OUT/javac.log" || { grep -A3 "error:" "$OUT/javac.log" | head -40; exit 1; }
else shift; fi
java -Xmx6g -Xss16m -Dprr.data="$ROOT/data" -cp "$OUT" prr.Harness "$@"
