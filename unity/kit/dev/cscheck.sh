#!/bin/bash
# Compile-check one decompiled assembly folder against the game's Managed DLLs (Unity is not available here).
# usage: cscheck.sh <Scripts dir> <AssemblyFolder> [extra .cs dirs...]
S="$1"; A="$2"; shift 2
M="/home/claude/prr/x/steamapps/common/Platypus Reclayed/Platypus_Data/Managed"
CSC=$(ls /usr/lib/dotnet/sdk/10.0.*/Roslyn/bincore/csc.dll | head -1)
refs=()
for d in "$M"/*.dll; do b=$(basename "$d" .dll); [ "$b" = "$A" ] && continue; refs+=("-r:$d"); done
files=$(find "$S/$A" "$@" -name '*.cs')
out=/tmp/claude-0/-home-claude/93834965-24fe-51c2-89be-8ffcbcbe2678/scratchpad/cs_$A.dll
dotnet "$CSC" -nologo -nostdlib -noconfig -t:library -unsafe -nowarn:0618,0414,0169,0649,0108,0114,0162,0219,0168,1998,8632 -langversion:9 -define:UNITY_ANDROID -out:"$out" "${refs[@]}" $files 2>&1 | grep -E "error" | sed 's|^.*/Scripts/||' | sort | uniq
