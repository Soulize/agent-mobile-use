#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

if [ -x "$ROOT/gradlew" ]; then
    GRADLE_CMD="$ROOT/gradlew"
else
    GRADLE_CMD="${GRADLE_CMD:-gradle}"
fi

echo "[modern-build] Building hook APK with Gradle/AGP..."
"$GRADLE_CMD" :agent-hook-apk:packageHookApk
cp -f agent-hook-apk/build/dist/agent_hook.apk agent-hook-apk/build/agent_hook.apk

echo "[modern-build] Building Java device tools with D8..."
chmod +x vd-tool-java/build-modern.sh
./vd-tool-java/build-modern.sh

echo "[modern-build] Building Android ARM64 Go daemon..."
chmod +x vd-server-go/build-modern.sh
./vd-server-go/build-modern.sh

echo "[modern-build] Packing KernelSU module..."
chmod +x ksu-module/pack.sh
./ksu-module/pack.sh

echo "[modern-build] Complete."
