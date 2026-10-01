#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if [ -x "$ROOT/gradlew" ]; then
    GRADLE_CMD="$ROOT/gradlew"
else
    GRADLE_CMD="${GRADLE_CMD:-gradle}"
    command -v "$GRADLE_CMD" >/dev/null
fi

echo "[build] Building release hook APK with Gradle/AGP..."
"$GRADLE_CMD" -p "$ROOT" :agent-hook-apk:assembleRelease

APK_SRC="$(find "$SCRIPT_DIR/build/outputs/apk/release" -maxdepth 1 -type f -name '*.apk' | sort | head -n 1)"
if [ -z "$APK_SRC" ] || [ ! -f "$APK_SRC" ]; then
    echo "[build] ERROR: release APK was not produced" >&2
    exit 1
fi

cp -f "$APK_SRC" "$SCRIPT_DIR/build/agent_hook.apk"

echo "[build] Build successful: build/agent_hook.apk"
ls -lh "$SCRIPT_DIR/build/agent_hook.apk"
stat -c '[build] agent_hook.apk bytes=%s' "$SCRIPT_DIR/build/agent_hook.apk"
