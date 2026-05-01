# Operational runbook

Step-by-step recipes for the common operations during development and debugging.

## Initial setup on a fresh S25 Ultra

### 1. Push model files to the device

The app reads models from `/sdcard/Android/data/com.example.starterhack/files/`. Two files needed:

```bash
# NPU-compiled model (3.0 GB) — for NPU variant
adb push gemma4_sm8750.litertlm \
  /sdcard/Android/data/com.example.starterhack/files/gemma4_npu.litertlm

# Generic CPU/GPU model (2.5 GB) — for GPU and CPU variants
adb push model/gemma4_generic.litertlm \
  /sdcard/Android/data/com.example.starterhack/files/gemma4.litertlm
```

(The filenames on disk on the device — `gemma4_npu.litertlm` and `gemma4.litertlm` — must match the names referenced in `RedactionViewModel.ModelVariant`. If you change the variant filename strings, push under the new names.)

Verify:

```bash
adb shell ls -la /sdcard/Android/data/com.example.starterhack/files/
```

You should see both files at their correct sizes:
- `gemma4.litertlm` ~ 2,583,085,056 bytes
- `gemma4_npu.litertlm` ~ 3,016,294,400 bytes

### 2. Build and install

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

(On Windows, set `JAVA_HOME` first if not already set:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```)

### 3. Launch and verify NPU init

```bash
# Force-stop, clear prefs, relaunch — guarantees fresh NPU attempt
adb shell am force-stop com.example.starterhack
adb shell run-as com.example.starterhack rm -f \
  /data/data/com.example.starterhack/shared_prefs/shieldtext_prefs.xml
adb logcat -c
adb shell am start -n com.example.starterhack/.MainActivity

# Watch for NPU success
adb logcat -s InferenceEngine:V
```

Expected sequence (~3 seconds):

```
ShieldTextApp: Pre-init ADSP_LIBRARY_PATH=...
InferenceEngine: Set native library paths: ...
InferenceEngine: Attempting NPU backend (Android 36)
InferenceEngine: NPU init succeeded
```

## Reset to a known-good state

When testing the cascade or debugging stuck state:

```bash
adb shell am force-stop com.example.starterhack
adb shell run-as com.example.starterhack rm -rf \
  /data/data/com.example.starterhack/shared_prefs/ \
  /data/data/com.example.starterhack/cache/
adb logcat -c
adb shell am start -n com.example.starterhack/.MainActivity
```

This clears:
- `shieldtext_prefs.xml` — persisted variant, so default `NPU` is used.
- `cache/` — XNNPack weight caches that are pinned to specific model file hashes.

## Inspect the APK

```bash
# What native libs ended up packaged
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep arm64-v8a
```

Expected output (sizes from latest build):

```
20092072  lib/arm64-v8a/libGemmaModelConstraintProvider.so
 5046904  lib/arm64-v8a/libLiteRt.so
 2794448  lib/arm64-v8a/libLiteRtClGlAccelerator.so
  478912  lib/arm64-v8a/libLiteRtDispatch_Qualcomm.so
 2778176  lib/arm64-v8a/libQnnHtp.so
10975268  lib/arm64-v8a/libQnnHtpV79Skel.so
  679168  lib/arm64-v8a/libQnnHtpV79Stub.so
 2983560  lib/arm64-v8a/libQnnSystem.so
12879480  lib/arm64-v8a/liblitertlm_jni.so
```

(Plus app-internal libs: `libandroidx.graphics.path.so`, `libimage_processing_util_jni.so`, `libmlkit_google_ocr_pipeline.so`, `libsurface_util_jni.so`.)

## Inspect what's extracted at runtime

```bash
# Find the install path
INSTALL_DIR=$(adb shell pm path com.example.starterhack | sed 's|package:||;s|/base.apk||')
echo $INSTALL_DIR

# List extracted libs
adb shell ls -la $INSTALL_DIR/lib/arm64/
```

If this directory is empty: `useLegacyPackaging = true` is missing from `app/build.gradle.kts`.

## Inspect the AAR contents

```bash
# Unpack the cached AAR to see what the Maven dep ships
mkdir -p /tmp/aar-inspect
cd /tmp/aar-inspect
unzip -q ~/.gradle/caches/modules-2/files-2.1/com.google.ai.edge.litertlm/litertlm-android/0.11.0-rc1/*/litertlm-android-0.11.0-rc1.aar
ls jni/arm64-v8a/
```

Expected: only `libLiteRt.so`, `libLiteRtClGlAccelerator.so`, `liblitertlm_jni.so`. None of the QNN libs come from the AAR.

## Pull device-side QNN libs (for reference / fallback)

```bash
# What the device's vendor partition has
adb shell find /vendor /system /odm -name 'libQnn*' 2>/dev/null

# Pull a specific lib for inspection
adb pull /vendor/lib64/rfs/dsp/snap/libQnnHtpV79Skel.so /tmp/device-skel.so
```

## Clear & relaunch one-liner (for iteration)

```bash
adb shell am force-stop com.example.starterhack && \
  adb shell run-as com.example.starterhack rm -rf \
    /data/data/com.example.starterhack/shared_prefs/ \
    /data/data/com.example.starterhack/cache/ && \
  adb install -r app/build/outputs/apk/debug/app-debug.apk && \
  adb logcat -c && \
  adb shell am start -n com.example.starterhack/.MainActivity && \
  sleep 6 && \
  adb logcat -d -s InferenceEngine:V ShieldTextApp:V
```

## Run unit tests

```bash
./gradlew :app:testDebugUnitTest
```

Tests run with `FakeLlmEngine` which bypasses model files; no device or model push required.

## Building from a clean checkout

```bash
git clone <repo>
cd ShieldText

# Pull the six NPU libs (they're committed but documenting the source for fresh setup)
mkdir -p app/src/main/jniLibs/arm64-v8a
cd app/src/main/jniLibs/arm64-v8a
for lib in libLiteRtDispatch_Qualcomm.so libGemmaModelConstraintProvider.so \
           libQnnHtp.so libQnnHtpV79Skel.so libQnnHtpV79Stub.so libQnnSystem.so; do
  curl -sSL -O "https://github.com/google-ai-edge/litert-samples/raw/main/compiled_model_api/qualcomm/llm_chatbot_npu/app/src/main/jniLibs/arm64-v8a/$lib"
done
cd -

./gradlew :app:assembleDebug
```

## Force NPU re-attempt mid-session

If the user has switched to CPU/GPU and wants to try NPU again, the easiest path is to kill the app:

```bash
adb shell am force-stop com.example.starterhack
adb shell am start -n com.example.starterhack/.MainActivity
```

In-app, switching CPU/GPU → NPU within a single process **does not work** due to a Hexagon DSP PD reservation limitation; see [backend-cascade.md](./backend-cascade.md).
