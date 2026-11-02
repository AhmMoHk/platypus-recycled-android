#!/bin/bash
# Compile translated code + runtime + stubs (desktop javac) to check the translation.
ROOT=/home/claude/prr
OUT=$ROOT/build/jcheck
rm -rf $OUT && mkdir -p $OUT
unset JAVA_TOOL_OPTIONS
find $ROOT/gen/java $ROOT/gen/stubs $ROOT/rt/java -name '*.java' > $OUT/files.txt
javac -nowarn -J-Xmx4g -encoding UTF-8 -Xmaxerrs 100000 -d $OUT/classes @$OUT/files.txt > $OUT/errors.txt 2>&1
echo "exit $?"; grep -c "error:" $OUT/errors.txt
