# Modern Build Toolchain Experiment

This branch isolates build-system modernization from runtime behavior changes.

## Current result

The modern toolchain has been validated by GitHub Actions end-to-end.

- Successful workflow: Modern build toolchain run #7
- Hook APK: Gradle/AGP build succeeded
- Java device tools: D8 outputs succeeded
- Go daemon: android/arm64 cross-build succeeded
- KernelSU ZIP: integrity check succeeded
- Artifact upload: succeeded
- Legacy source/build scripts remain untouched

This proves the old Java 8 runtime, `dx`, API 23 build platform, and hard-coded
`/root` / `/usr/lib/android-sdk` paths are build-environment legacy constraints,
not requirements of the current project source.

## Scope

The experiment keeps the existing runtime architecture and legacy Xposed API 82.
It changes only how artifacts are built:

- Hook APK: manual `aapt -> javac -> dx -> zipalign -> apksigner` becomes Gradle + Android Gradle Plugin.
- Java device tools: `dx` becomes `D8`.
- Go daemon: tests run natively, then the final binary is explicitly cross-built for `android/arm64`.
- KernelSU packaging: the existing `ksu-module/pack.sh` remains unchanged.

The original build scripts are intentionally retained as a fallback while parity is verified.

## Experimental versions

- Android Gradle Plugin: 9.3.0
- Gradle: 9.5.0
- JDK: 17
- compileSdk: 36
- targetSdk: 28 (unchanged to avoid runtime behavior changes)
- minSdk: 26 (unchanged)
- Android Build Tools: 36.0.0
- Xposed API: 82 as `compileOnly`
- Go: version from `vd-server-go/go.mod`

## Why API 82 is not being migrated yet

Build modernization and Hook API migration are separate risk domains.
The current code is written against the legacy Xposed API and LSPosed still provides that API at runtime.
Keeping API 82 as `compileOnly` lets this branch verify AGP/D8/JDK migration without changing Hook semantics.

A later branch can evaluate `libxposed` only after artifact parity is established.

## New build entry points

Full package:

```bash
./build-modern.sh
```

Hook APK only:

```bash
gradle :agent-hook-apk:packageHookApk
```

Java device DEX tools only:

```bash
./vd-tool-java/build-modern.sh
```

Go daemon only:

```bash
./vd-server-go/build-modern.sh
```

## Compatibility strategy

The Gradle build uses a separate modern manifest under
`agent-hook-apk/src/main/AndroidManifest.xml`.
The original root manifest is left in place so the legacy `agent-hook-apk/build.sh`
continues to work during the experiment.

The legacy Quick Settings stubs are not used by Gradle because compileSdk 36 already
contains those framework APIs.

## Exit criteria before replacing the legacy build

1. Modern CI produces a complete installable KernelSU ZIP.
2. APK package name, version, min/target SDK and Xposed metadata match the legacy build.
3. The modern APK installs and LSPosed recognizes/loads the module.
4. `agent_tools.dex` and `agent_vd.dex` run on the target device with D8 output.
5. `vd_server` is confirmed as Android ARM64.
6. The resulting module passes one real-device smoke test.
7. Only after parity is proven should the old `dx/aapt` scripts be retired.

## Follow-up

If the experiment succeeds, the next cleanup step is to add and commit a Gradle Wrapper,
then make the CI and local build use `./gradlew` exclusively.
