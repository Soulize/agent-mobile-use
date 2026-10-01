#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

ANDROID_JAR="/usr/lib/android-sdk/platforms/android-23/android.jar"
XPOSED_JAR="/root/xposed_lib/xposed-api-stub.jar"
DX="/usr/lib/android-sdk/build-tools/debian/dx"
AAPT="/usr/bin/aapt"
APKSIGNER="/usr/bin/apksigner"

rm -rf build
mkdir -p build/gen build/classes build/apk

echo "[build] 1. Generating R.java and initial package with aapt..."
"$AAPT" package -f -m -0 arsc \
    -S res \
    -J build/gen \
    -M AndroidManifest.xml \
    -I "$ANDROID_JAR" \
    -F build/apk/unaligned.apk

echo "[build] 2. Compiling Java sources..."
mkdir -p build/stubs_classes
if [ -d "stubs" ]; then
    javac -proc:none -source 1.8 -target 1.8 -cp "$ANDROID_JAR" $(find stubs -name "*.java") -d build/stubs_classes
fi

javac -proc:none -source 1.8 -target 1.8 \
    -cp "$ANDROID_JAR:$XPOSED_JAR:build/stubs_classes" \
    $(find src build/gen -name "*.java") \
    -d build/classes

echo "[build] 3. Converting classes to classes.dex..."
cd build/classes
"$DX" --dex --output=../classes.dex $(find . -name "*.class")
cd "$SCRIPT_DIR"

echo "[build] 4. Adding classes.dex and assets to APK..."
cd build
"$AAPT" add "apk/unaligned.apk" "classes.dex"
cd "$SCRIPT_DIR"
for a in $(find assets -type f); do
    "$AAPT" add "build/apk/unaligned.apk" "$a"
done

echo "[build] 5. Zipaligning APK to 4-byte boundary..."
zipalign -p -f 4 "build/apk/unaligned.apk" "build/apk/aligned.apk"

echo "[build] 6. Signing APK..."
SIGNING_KEYSTORE="${APK_SIGNING_KEYSTORE:-}"
SIGNING_PASSWORD="${APK_SIGNING_PASSWORD:-}"
SIGNING_ALIAS="agentmobileuse"

if [ -n "$SIGNING_KEYSTORE" ]; then
    if [ ! -f "$SIGNING_KEYSTORE" ]; then
        echo "[build] ERROR: APK_SIGNING_KEYSTORE does not exist: $SIGNING_KEYSTORE" >&2
        exit 1
    fi
    if [ -z "$SIGNING_PASSWORD" ]; then
        echo "[build] ERROR: APK_SIGNING_PASSWORD is required for fixed signing" >&2
        exit 1
    fi
    echo "[build] Using configured fixed signing keystore (alias: $SIGNING_ALIAS)"
else
    if [ "${CI:-}" = "true" ]; then
        echo "[build] ERROR: CI builds require APK_SIGNING_KEYSTORE and APK_SIGNING_PASSWORD" >&2
        exit 1
    fi

    echo "[build] No fixed keystore configured; using local debug keystore"
    SIGNING_KEYSTORE="/root/debug.keystore"
    SIGNING_PASSWORD="android"
    SIGNING_ALIAS="androiddebugkey"

    if [ ! -f "$SIGNING_KEYSTORE" ]; then
        keytool -genkey -v -keystore "$SIGNING_KEYSTORE" \
            -storepass "$SIGNING_PASSWORD" \
            -alias "$SIGNING_ALIAS" \
            -keypass "$SIGNING_PASSWORD" \
            -keyalg RSA -keysize 2048 -validity 10000 \
            -dname "CN=Android Debug,O=Android,C=US"
    fi
fi

"$APKSIGNER" sign --ks "$SIGNING_KEYSTORE" \
    --ks-pass "pass:$SIGNING_PASSWORD" \
    --ks-key-alias "$SIGNING_ALIAS" \
    --key-pass "pass:$SIGNING_PASSWORD" \
    --out "build/agent_hook.apk" \
    "build/apk/aligned.apk"

echo "[build] Build successful: build/agent_hook.apk"
ls -lh build/agent_hook.apk
