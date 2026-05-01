# Diagnostics — error decoder & adb commands

When something breaks, this is where to start. Each error pattern below has been observed during enablement and points at a specific root cause.

## Useful logcat filters

```bash
# Engine init markers + key error classes
adb logcat -d | grep -E "ShieldTextApp|InferenceEngine|tflite.*Failed|CreateSharedMemoryManager|init succeeded|context create from binary|qnn_manager|delegate_opengl|gpu_backend_opengl|DispatchDelegate|DISPATCH_OP|Failed to find available PD"

# Just the InferenceEngine stage transitions
adb logcat -d -s InferenceEngine:V

# Full DSP/QNN error trail
adb logcat -d *:E | grep -E "QnnDsp|qnn_manager|fastrpc|adsprpc|skel|dispatch_api"

# Window of time around a known bad timestamp
adb logcat -d -v time | grep -E "<your-pid>" | grep "15:54:2[5-9]"

# Find which subgraphs got NPU-delegated vs CPU-delegated
adb logcat -d | grep -E "Replacing.*delegate|partition"
```

## Error → cause → fix table

### `Encountered unresolved custom op: DISPATCH_OP`

```
E tflite : Encountered unresolved custom op: DISPATCH_OP.
E tflite : Node number 0 (DISPATCH_OP) failed to prepare.
```

**Cause:** `libLiteRtDispatch_Qualcomm.so` static-init didn't run, so `DISPATCH_OP` was never registered with the tflite op resolver. Either dlopen failed silently (wrong lib version) or the lib was unloaded before model parse.

**Likely scenarios:**
- Dispatch lib missing from `jniLibs/`.
- Wrong-version dispatch lib (mismatched against `libLiteRt.so` from AAR).
- `useLegacyPackaging = true` missing from `build.gradle.kts` — lib never extracted to `nativeLibraryDir`.
- Trying to switch to NPU after a non-NPU `Engine` was already created in the same process (in-process re-init limitation — see [backend-cascade.md](./backend-cascade.md)).

**Fix:** Verify all six libs are in `jniLibs/arm64-v8a/` with byte counts matching [native-libs.md](./native-libs.md). If switching mid-session, kill and relaunch.

---

### `Failed to find available PD`

```
E QnnDsp <E> Failed to find available PD for contextId 5 on deviceId 0 coreId 0 with context size estimate 1324262400
E QnnDsp <E> context create from binary failed on contextId 5, err = 1002
E litert: [qnn_manager.cc:556] Failed to create QNN context: 1002
E tflite : Failed to initialize kernel.
```

**Cause:** Hexagon DSP can't load `libQnnHtpV79Skel.so` from any path it knows about. Either the skel isn't on the device's vendor partition AND we didn't seed `ADSP_LIBRARY_PATH` early enough, or another process owns the DSP PD.

**Fix:**
- Verify `ShieldTextApp.onCreate()` ran (look for `Pre-init ADSP_LIBRARY_PATH=` log line on launch).
- Verify the path includes `/vendor/lib64/rfs/dsp/snap` (Samsung S25 Ultra location for the skel).
- Confirm device has the skel: `adb shell find /vendor /system /odm -name 'libQnnHtpV79Skel.so'`
- If the path is correctly seeded but it still fails — see in-process re-init limitation.

---

### `UNIMPLEMENTED: CreateSharedMemoryManager`

```
E delegate_opengl.cc:218: Failed to create DelegateKernelLiteRt: UNIMPLEMENTED: CreateSharedMemoryManager is not implemented.
=== Source Location Trace: ===
third_party/odml/litert/ml_drift/delegate/gpu_backend_opengl.cc:169
E tflite : Failed to initialize kernel.
```

**Cause:** GPU OpenGL backend on Android 16. The `litert::ml_drift` GPU delegate's OpenGL path tries to create a shared memory manager via an unimplemented stub. Affects both `Backend.GPU()` as the main backend AND as a `visionBackend` sub-backend on NPU.

**Fix:**
- Set `visionBackend = Backend.CPU()` (not `Backend.GPU()`) on `EngineConfig`. The dev guide example showing `visionBackend = Backend.GPU()` is wrong on Android 16.
- For the GPU main backend itself: no fix from app side. Use NPU or CPU.

---

### `constrained reporting is not supported on NPU error 12` (during inference)

User-visible error during `redact()` call.

**Cause:** `ExperimentalFlags.enableConversationConstrainedDecoding = true` while NPU backend active. NPU executor doesn't support constrained decoding.

**Fix:** Force the flag to `false` for all backends:
```kotlin
ExperimentalFlags.enableConversationConstrainedDecoding = false
```
(See `InferenceEngine.kt:redact()`.)

---

### `Input tensor not found`

```
LiteRtLmJniException: Failed to create engine: NOT_FOUND: ERROR: [...] Input tensor not found
```

**Cause:** Model/backend mismatch. Most commonly: trying to load `gemma4_npu.litertlm` (NPU-compiled, contains `DISPATCH_OP`) on a CPU or GPU backend. The model has tensors that don't match the backend's expected graph shape.

**Fix:** Each variant must use its paired model file. NPU → `gemma4_npu.litertlm`. CPU/GPU → `gemma4.litertlm`. The viewmodel cascade handles this automatically (see [backend-cascade.md](./backend-cascade.md)).

---

### `Cannot locate symbol "LiteRtGetEnvironmentOptions"`

Documented in `litert-samples/.../NPU_ISSUE_REPORT.md` but **outdated** per the LiteRT-LM developer. Per their feedback, with the right lib set this doesn't happen on `litertlm-android:0.11.0-rc1`.

**If you see this:** check that `libLiteRtDispatch_Qualcomm.so` came from `litert-samples/main` and matches the byte count in [native-libs.md](./native-libs.md).

---

### `Pre-init ADSP_LIBRARY_PATH=` doesn't appear at launch

**Cause:** `ShieldTextApp` not registered as the application class.

**Fix:** Verify `app/src/main/AndroidManifest.xml` has:

```xml
<application
    android:name=".ShieldTextApp"
    ...>
```

Without this, the env vars get set inside `InferenceEngine.initialize()` (too late — `libQnnHtp.so` has already cached the path).

---

### App goes straight to "Using CPU backend" on launch (skipping NPU)

**Cause:** SharedPreferences has `selected_variant=CPU` from a prior session.

**Fix:** Clear prefs and relaunch:

```bash
adb shell am force-stop com.example.starterhack
adb shell run-as com.example.starterhack rm /data/data/com.example.starterhack/shared_prefs/shieldtext_prefs.xml
adb shell am start -n com.example.starterhack/.MainActivity
```

Or pick `NPU (SM8750)` from the dropdown manually. (Note: this won't work mid-session if a non-NPU engine has already been active — see in-process re-init limitation in [backend-cascade.md](./backend-cascade.md).)

---

## Inspecting state on device

```bash
# Check model files are in place
adb shell ls -la /sdcard/Android/data/com.example.starterhack/files/

# Find skel libraries the DSP could potentially load
adb shell find /vendor /system /odm -name 'libQnnHtpV79Skel.so'

# Check SharedPreferences (the persisted variant)
adb shell run-as com.example.starterhack cat /data/data/com.example.starterhack/shared_prefs/shieldtext_prefs.xml

# Check XNNPack cache files (CPU/GPU caches state per model)
adb shell run-as com.example.starterhack ls /data/data/com.example.starterhack/cache/

# Check what jniLibs got extracted
adb shell run-as com.example.starterhack ls -la /data/app/<full-app-path>/lib/arm64/

# Or via the package manager:
adb shell pm dump com.example.starterhack | grep nativeLibraryDir
```

## Things to try when stuck

1. **Force-stop + clear prefs + clear cache + reinstall + relaunch.** Eliminates state corruption.
2. **Check `Pre-init ADSP_LIBRARY_PATH=` appears.** If not, `ShieldTextApp` isn't running.
3. **Check `qnn_manager.cc:125 Loading qnn shared library` appears.** If not, dispatch lib didn't load.
4. **Check `Replacing N out of M nodes with delegate (DispatchDelegate)`.** If not, DISPATCH_OP isn't registered.
5. **Compare your `jniLibs/` byte counts** against [native-libs.md](./native-libs.md). One byte off ⇒ wrong version.
