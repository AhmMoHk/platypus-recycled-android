#!/bin/bash
# Translate the game assemblies to Java and generate stubs for runtime classes not yet hand-written.
set -e
ROOT=/home/claude/prr
M="$ROOT/x/steamapps/common/Platypus Reclayed/Platypus_Data/Managed"
rm -rf $ROOT/gen/java $ROOT/gen/stubs
cd $ROOT/re
STUBS=$ROOT/gen/stubs RTDIR=$ROOT/rt/java python3 -m cil.run $ROOT/gen/java "$M/Platypus.dll" "$M/Platypus.Core.dll" "$M/Tools.dll" "$M/MoonSharp.dll" "$M/DigitalRuby.AdvancedPolygonCollider.dll" "$M/UnityEngine.UI.dll" "$M/Unity.TextMeshPro.dll" "$@"
python3 -m cil.patches $ROOT/gen/java
