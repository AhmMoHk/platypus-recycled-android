#!/bin/bash
# Compile the patched dev tree in dependency order: facade Rewired_Core first, then the game assemblies against it.
D=/home/claude/kit/dev/Scripts; O=/home/claude/kit/dev/bin; mkdir -p $O
M="/home/claude/prr/x/steamapps/common/Platypus Reclayed/Platypus_Data/Managed"
CSC=$(ls /usr/lib/dotnet/sdk/10.0.*/Roslyn/bincore/csc.dll | head -1)
build() {   # name, then extra -r refs
  local A=$1; shift
  local refs=()
  for d in "$M"/*.dll; do b=$(basename "$d" .dll); case "$b" in "$A"|Rewired_*|com.rlabrecque*|Assembly-CSharp|Platypus|Platypus.Core) continue;; esac; refs+=("-r:$d"); done
  for x in "$@"; do refs+=("-r:$O/$x.dll"); done
  IFS=$'\n' files=($(find "$D/$A" -name '*.cs'))
  dotnet "$CSC" -nologo -nostdlib -noconfig -t:library -unsafe -nowarn:0618,0414,0169,0649,0108,0114,0162,0219,0168,1998,8632,0067,0659,0661,0660 -langversion:9 -define:UNITY_ANDROID -out:"$O/$A.dll" "${refs[@]}" "${files[@]}" 2>&1 | grep -E "error" | sed 's|^.*/Scripts/||' | sort -u > "$O/$A.err"
  echo "$A: $(wc -l < "$O/$A.err") errors"
}
build Rewired_Core
build Platypus.Core Rewired_Core
build Platypus Rewired_Core Platypus.Core
build Assembly-CSharp Rewired_Core Platypus.Core Platypus
