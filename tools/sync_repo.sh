#!/bin/bash
# copy the port sources (no game data) into the GitHub working tree
set -e
P=/home/claude/prr
R=/home/claude/platypus-recycled-android
rsync -a --delete "$P/rt/java/" "$R/engine/java/"
rsync -a --delete --exclude __pycache__ --exclude ext_index.json "$P/re/cil/" "$R/tools/cil/"
rsync -a --delete --exclude __pycache__ "$P/re/tools/" "$R/tools/export/"
rsync -a --delete --exclude debug.keystore "$P/android/" "$R/android/"
rsync -a --delete "$P/desktop/" "$R/desktop/"
cp "$P"/tools/*.sh "$R/tools/"
cp "$P/notes/PLAN.md" "$R/notes/"
