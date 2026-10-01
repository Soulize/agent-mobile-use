#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
ANDROID_API="${ANDROID_API:-36}"
BUILD_TOOLS_VERSION="${ANDROID_BUILD_TOOLS:-36.0.0}"

if [ -z "$SDK_ROOT" ]; then
    echo "[build] ERROR: ANDROID_SDK_ROOT or ANDROID_HOME must be set" >&2
    exit 1
fi

ANDROID_JAR="$SDK_ROOT/platforms/android-$ANDROID_API/android.jar"
D8="$SDK_ROOT/build-tools/$BUILD_TOOLS_VERSION/d8"

test -f "$ANDROID_JAR"
test -x "$D8"

rm -rf bin/classes bin/d8-tools bin/d8-vd
mkdir -p bin/classes bin/d8-tools bin/d8-vd

echo "[build] Compiling Java tools against Android API $ANDROID_API..."
javac -proc:none --release 8 -cp "$ANDROID_JAR"     src/com/agent/ToolMain.java     src/com/agent/DaemonMain.java     -d bin/classes

echo "[build] Verifying compiled classes have source counterparts..."
orphans=0
for cls in bin/classes/com/agent/*.class; do
    base="$(basename "$cls" .class)"
    top="${base%%\$*}"
    if [ ! -f "src/com/agent/${top}.java" ]; then
        echo "[build] ERROR: orphan class ${base}.class has no src/com/agent/${top}.java" >&2
        orphans=$((orphans + 1))
    fi
done
if [ "$orphans" -ne 0 ]; then
    echo "[build] FAILED: ${orphans} orphan class(es) found" >&2
    exit 1
fi

echo "[build] D8 -> agent_tools.dex"
"$D8" --release --min-api 26 --lib "$ANDROID_JAR"     --output bin/d8-tools     bin/classes/com/agent/ToolMain*.class
mv bin/d8-tools/classes.dex bin/agent_tools.dex

echo "[build] D8 -> agent_vd.dex"
"$D8" --release --min-api 26 --lib "$ANDROID_JAR"     --output bin/d8-vd     bin/classes/com/agent/DaemonMain*.class
mv bin/d8-vd/classes.dex bin/agent_vd.dex

echo "[build] Build complete:"
ls -lh bin/agent_tools.dex bin/agent_vd.dex
stat -c '[build] agent_tools.dex bytes=%s' bin/agent_tools.dex
stat -c '[build] agent_vd.dex bytes=%s' bin/agent_vd.dex
