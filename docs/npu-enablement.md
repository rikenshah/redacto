# NPU enablement on Snapdragon 8 Elite (SM8750)

**Outcome:** NPU init succeeds in ~2.5 seconds on first launch. `gemma4_npu.litertlm` runs on the Hexagon V79 via the QNN HTP backend.

This doc walks the failure modes encountered, in the order they surfaced, with the actual log lines that pointed at each one. Each section ends with the fix.

---

## Environment

| Item | Value |
|---|---|
| Device | Samsung Galaxy S25 Ultra |
| Chipset | Snapdragon 8 Elite for Galaxy (SM8750-AC) |
| NPU | Hexagon V79 |
| Android | 16 (API 36) |
| Maven dep | `com.google.ai.edge.litertlm:litertlm-android:0.11.0-rc1` |
| Model | `gemma4_npu.litertlm` (3.0 GB, NPU-compiled multimodal Gemma 4 E2B) |

---

## Failure mode 1 — Dispatch lib symbol mismatch (RESOLVED)

### Symptom

```
E tflite : Encountered unresolved custom op: DISPATCH_OP.
E tflite : Node number 0 (DISPATCH_OP) failed to prepare.
```

### Root cause

`libLiteRtDispatch_Qualcomm.so` registers the `DISPATCH_OP` custom op via static-init constructors when dlopened. Initially, the dispatch lib bundled in `jniLibs/` was either missing or a version mismatched against the AAR's `libLiteRt.so`, so dlopen failed silently and the static init never ran.

The `NPU_ISSUE_REPORT.md` in `litert-samples/compiled_model_api/qualcomm/llm_chatbot_npu` documented this exact failure as `LiteRtGetEnvironmentOptions` symbol missing in the AAR. **Per the developer this report is outdated** — with the right lib set the issue doesn't happen.

### Fix

Pull the exact six libs from the official Qualcomm sample's `jniLibs/arm64-v8a/`:

- `libLiteRtDispatch_Qualcomm.so` (478,912 bytes)
- `libGemmaModelConstraintProvider.so` (20,092,072 bytes)
- `libQnnHtp.so` (2,778,176 bytes — QAIRT 2.42)
- `libQnnHtpV79Skel.so` (10,975,268 bytes)
- `libQnnHtpV79Stub.so` (679,168 bytes — QAIRT 2.42)
- `libQnnSystem.so` (2,983,560 bytes — QAIRT 2.42)

Source: `https://github.com/google-ai-edge/litert-samples/raw/main/compiled_model_api/qualcomm/llm_chatbot_npu/app/src/main/jniLibs/arm64-v8a/<lib>.so`

Initial code had `libQnnHtp.so` etc. pulled from the device's vendor partition — different bytes, slightly different ABI, mismatch. Replacing all six with the sample's QAIRT 2.42 set fixed the dlopen.

After this, log shows:

```
I litert : [qnn_manager.cc:125] Loading qnn shared library from "libQnnHtp.so"
I litert : [qnn_manager.cc:134] Loaded qnn shared library
I tflite : Replacing 1 out of 1 node(s) with delegate (DispatchDelegate) for subgraph 0
```

---

## Failure mode 2 — `pickFirsts` masking nothing (RESOLVED)

### Symptom

User suspected the `pickFirsts` block in `app/build.gradle.kts` was selecting wrong-version libs.

### Investigation

Inspected the AAR: `~/.gradle/caches/.../litertlm-android-0.11.0-rc1.aar` ships only:

- `libLiteRt.so`
- `libLiteRtClGlAccelerator.so`
- `liblitertlm_jni.so`

**Zero overlap** with the six QNN/dispatch libs in our `jniLibs`. `pickFirsts` had nothing to dedupe; it was a no-op.

### Fix

Removed `pickFirsts` entirely from `app/build.gradle.kts`. Build still succeeds with no duplicate-file errors, confirming there were no hidden duplicates being silently resolved.

Also removed `libLiteRtCompilerPlugin_Qualcomm.so` from `jniLibs/` — that's for classical-model NPU JIT, not LLM, and the official LLM sample doesn't bundle it.

---

## Failure mode 3 — Hexagon DSP can't find `libQnnHtpV79Skel.so` (RESOLVED — KEY FIX)

### Symptom

```
W apps_std_imp.c:1185: apps_std_fopen_with_env_fd failed with 0xd for /vendor/dsp/cdsp/./libQnnHtpV79Skel.so (No such file or directory)
E remote_handle_open_domain: dynamic loading failed for libQnnHtpV79Skel.so
E QnnDsp <E> Failed to find available PD for contextId 5 ... err: 1002
E litert: [qnn_manager.cc:556] Failed to create QNN context: 1002
E tflite : Failed to initialize kernel.
```

### Root cause

The skel runs **on the Hexagon DSP**, not the application processor. The DSP firmware loads it via FastRPC from a DSP-accessible path, NOT from `/data/app/.../lib/arm64-v8a/`. The DSP searches a hardcoded fallback list (`/vendor/dsp/cdsp/`, `/vendor/lib/rfsa/adsp/`, etc.) plus whatever is in `ADSP_LIBRARY_PATH`.

Verified the actual location on Samsung S25 Ultra:

```
$ adb shell find /vendor /system /odm -name 'libQnnHtpV79Skel.so'
/vendor/lib64/hw/audio/libQnnHtpV79Skel.so
/vendor/lib64/rfs/dsp/snap/libQnnHtpV79Skel.so      ← Samsung-specific path
```

The Samsung path `/vendor/lib64/rfs/dsp/snap/` is NOT in FastRPC's hardcoded list, so the DSP needs `ADSP_LIBRARY_PATH` to include it.

`ADSP_LIBRARY_PATH` was being set inside `InferenceEngine.initialize()` via `android.system.Os.setenv(...)`. But `libQnnHtp.so` reads the env var **once** when dlopened, and our pre-init `System.loadLibrary("LiteRtDispatch_Qualcomm")` was triggering the dlopen *before* we'd set the path. By the time we set it, QnnHtp had already cached an empty path.

### Fix

Created `ShieldTextApp : Application` and seeded the env vars in `onCreate()` — runs before any LiteRT lib loads:

```kotlin
class ShieldTextApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val nativeLibDir = applicationInfo.nativeLibraryDir
        val paths = listOf(
            nativeLibDir,
            "/vendor/lib64/rfs/dsp/snap",   // Samsung S25 Ultra V79 skel
            "/vendor/lib64/hw/audio",       // Samsung alternate
            "/vendor/dsp/cdsp",
            "/vendor/lib64",
            "/vendor/lib64/snap",
            "/system/lib64",
        ).joinToString(":")
        android.system.Os.setenv("ADSP_LIBRARY_PATH", paths, true)
        android.system.Os.setenv("LD_LIBRARY_PATH", paths, true)
    }
}
```

Wired into `AndroidManifest.xml` as `android:name=".ShieldTextApp"`.

After this, the DSP successfully loads the skel and creates a QNN PD.

---

## Failure mode 4 — OpenGL `CreateSharedMemoryManager` unimplemented on Android 16 (RESOLVED — KEY FIX)

### Symptom

```
E delegate_opengl.cc:218: Failed to create DelegateKernelLiteRt: UNIMPLEMENTED: CreateSharedMemoryManager is not implemented.
=== Source Location Trace: ===
third_party/odml/litert/ml_drift/delegate/gpu_backend_opengl.cc:169
third_party/odml/litert/ml_drift/delegate/delegate_kernel.cc:337
third_party/odml/litert/ml_drift/delegate/delegate_kernel_litert.cc:167
E tflite : Failed to initialize kernel.
```

### Root cause — the most subtle failure

This error is from the **GPU OpenGL backend**. When you set `visionBackend = Backend.GPU()` on the NPU `EngineConfig` (per the dev guide for Gemma 4 multimodal), the engine init still tries to create the GPU vision sub-backend. On Android 16, `litert::ml_drift`'s OpenGL path hits an unimplemented `CreateSharedMemoryManager` and throws.

**Critically, this throw is what was masquerading as the "NPU failure"**. NPU itself was working — it had registered DispatchDelegate on multiple subgraphs and created QNN contexts of size 1.18 GB. But the unrelated GPU vision sub-backend killed the whole `Engine.initialize()` call.

The dev guide example explicitly shows:
```kotlin
visionBackend = Backend.GPU(),       // Use GPU for images
audioBackend = Backend.CPU()         // Use CPU for audio
```

That assumption breaks on Android 16.

### Fix

Set vision/audio sub-backends to CPU for NPU and GPU paths in `InferenceEngine.kt`:

```kotlin
PreferredBackend.NPU -> {
    val config = EngineConfig(
        modelPath = modelPath,
        backend = Backend.NPU(nativeLibraryDir = nativeLibDir),
        visionBackend = Backend.CPU(),    // NOT Backend.GPU() — Android 16 OpenGL bug
        audioBackend = Backend.CPU(),
        maxNumTokens = 4000,
        cacheDir = context.cacheDir.absolutePath,
    )
    engine = Engine(config).also { it.initialize() }
}
```

Same change applied to the `GPU` and `CPU` branches for consistency. Our text-only redaction flow doesn't actually run vision, so the cost of CPU vision is zero.

---

## Failure mode 5 — Constrained decoding error 12 on NPU (RESOLVED)

### Symptom (from runtime, not init)

User reported: *"tried npu — constrained reporting is not supported on npu error 12"*

### Root cause

I had originally written:
```kotlin
ExperimentalFlags.enableConversationConstrainedDecoding = isNpu
```

That flips constrained decoding ON when running NPU. The NPU executor doesn't support constrained decoding and returns "error 12: not supported" when `createConversation` is called with the flag set.

The gallery sample passes `enableConversationConstrainedDecoding` as an external opt-in parameter, not tied to NPU. Constrained decoding is only useful for structured-output use cases (JSON schema, tool calls) which we don't need for free-form PII redaction.

### Fix

```kotlin
ExperimentalFlags.enableConversationConstrainedDecoding = false
```

Force off for all backends. `samplerConfig = null` for NPU is unchanged (still required per gallery sample — NPU uses runtime-default sampler).

---

## Failure mode 6 — In-process re-init limitation (NOT RESOLVED — known constraint)

### Symptom

After NPU has successfully run in a process, switching to CPU/GPU and back to NPU produces:

```
E QnnDsp: Failed to find available PD for contextId 5 ... err: 1002
E tflite : Encountered unresolved custom op: DISPATCH_OP.
```

### Root cause

Once a non-NPU `Engine` has been instantiated and closed in the process, the Hexagon DSP's PD (Protection Domain) reservation can't be re-acquired by a subsequent NPU `Engine` in the same process. This is a QNN/LiteRT-LM constraint, not something fixable from app code without restarting the process.

### Workaround

The viewmodel's variant cascade naturally handles "NPU first, then fall to GPU/CPU on first failure." Falling AWAY from NPU mid-session is fine. Falling BACK TO NPU mid-session is not.

UX consequence: if the user manually picks CPU/GPU and then later wants NPU back, they need to kill the app and relaunch.

A future enhancement could call `Process.killProcess(myPid())` when the user picks NPU after a non-NPU session — Android will relaunch the activity automatically. Not yet implemented.

---

## What it looks like when it works

Successful NPU init log sequence:

```
15:54:27.482  ShieldTextApp: Pre-init ADSP_LIBRARY_PATH=/data/app/.../lib/arm64:/vendor/lib64/rfs/dsp/snap:...
15:54:27.664  InferenceEngine: Attempting NPU backend (Android 36)
15:54:27.681  litert: [qnn_manager.cc:401] Adding shared library dir to path
15:54:27.690  litert: [qnn_manager.cc:125] Loading qnn shared library from "libQnnHtp.so"
15:54:27.691  litert: [qnn_manager.cc:134] Loaded qnn shared library
15:54:27.788  tflite: Replacing 1/1 nodes with delegate (DispatchDelegate) — subgraph 0
15:54:28.361  tflite: DispatchDelegate — subgraph 1
15:54:28.740  tflite: DispatchDelegate — subgraph 1 (decoder)
15:54:28.744  tflite: DispatchDelegate — subgraph 4
15:54:30.171  InferenceEngine: NPU init succeeded   ← total init: 2.51s
```

---

## Things that DIDN'T turn out to be the problem

For the next debugger to save time:

- **Maven version (0.11.0-rc1)**: Initially suspected, but the exact same version is used by the official Qualcomm sample which works. There's no newer published version (verified Maven Central + dl.google.com Maven). Building from source via Bazel is theoretically Solution A in `NPU_ISSUE_REPORT.md` but turned out to be unnecessary.
- **`pickFirsts` in build.gradle.kts**: User suspected it was masking version conflicts. Verified by inspecting AAR contents — there were zero duplicates. Removed for cleanliness.
- **`useLegacyPackaging = true`**: Required and kept. LiteRT's dispatch lookup does a filesystem `readdir` on `applicationInfo.nativeLibraryDir`; without legacy packaging that dir is empty (libs stay inside the APK).
- **Bundling `libQnnHtpV79Skel.so` in `jniLibs/`**: Helpful but not sufficient on its own. The skel runs on the DSP and can only be loaded by FastRPC from `ADSP_LIBRARY_PATH`-listed directories. We bundle our copy *and* point the path at the Samsung vendor location for redundancy.
- **NPU model file naming**: The variant enum reverted to `gemma4_npu.litertlm` and `gemma4.litertlm` to match what's already on `/sdcard/Android/data/.../files/` from prior pushes — the actual filenames don't matter as long as the variant + file map agrees.
