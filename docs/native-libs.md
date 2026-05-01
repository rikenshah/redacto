# Native libraries reference

Everything bundled in `app/src/main/jniLibs/arm64-v8a/` and what arrives via the `litertlm-android` AAR. If a lib is missing or the wrong version, refer to the failure-mode notes below.

## Final lib set in the APK

After build, `unzip -l app-debug.apk | grep arm64-v8a` shows:

| Library | Bytes | Source | Purpose |
|---|---:|---|---|
| `libLiteRt.so` | 5,046,904 | AAR (`litertlm-android-0.11.0-rc1.aar`) | Core LiteRT runtime — TfLite ops, op resolver, kernel registry. Required for any backend. |
| `libLiteRtClGlAccelerator.so` | 2,794,448 | AAR | LiteRT GPU accelerator (OpenCL/OpenGL). Loaded when `Backend.GPU()` is requested. **The OpenGL path is broken on Android 16 — see `npu-enablement.md` failure mode 4.** |
| `liblitertlm_jni.so` | 12,879,480 | AAR | JNI bindings between Kotlin and the LiteRT-LM C++ engine. |
| `libLiteRtDispatch_Qualcomm.so` | 478,912 | Sample's `jniLibs/` | Qualcomm dispatch backend. Registers `DISPATCH_OP` custom op via static-init constructors. NPU-only; without it `gemma4_npu.litertlm` parser hits `unresolved custom op: DISPATCH_OP`. |
| `libGemmaModelConstraintProvider.so` | 20,092,072 | Sample's `jniLibs/` | Provides backend-constraint metadata for Gemma 4 model variants. Without it, `litert_lm_loader` logs warnings like `TF_LITE_PREFILL_DECODE not found for backend constraints. Skipping.` and may misroute sub-graphs. |
| `libQnnHtp.so` | 2,778,176 | Sample's `jniLibs/` (QAIRT 2.42) | QNN HTP backend on the application processor — handles host-side RPC marshalling to the DSP. Reads `ADSP_LIBRARY_PATH` once at dlopen time. |
| `libQnnHtpV79Skel.so` | 10,975,268 | Sample's `jniLibs/` | DSP-side library executed on the Hexagon V79. **This is the file FastRPC tries to load from `ADSP_LIBRARY_PATH`.** Bundling it lets the DSP find a copy via `applicationInfo.nativeLibraryDir`; the Samsung firmware also ships its own at `/vendor/lib64/rfs/dsp/snap/libQnnHtpV79Skel.so`. |
| `libQnnHtpV79Stub.so` | 679,168 | Sample's `jniLibs/` (QAIRT 2.42) | Application-processor-side stub that pairs with the V79 skel. Provides the host API surface for HTP V79 ops. |
| `libQnnSystem.so` | 2,983,560 | Sample's `jniLibs/` (QAIRT 2.42) | QNN core system library — context management, error reporting, transport setup. |

Plus app-internal libs (image/CameraX/MLKit) you can ignore for the NPU story.

## What the AAR contains

Inspected directly:

```
$ unzip -l ~/.gradle/caches/modules-2/files-2.1/com.google.ai.edge.litertlm/litertlm-android/0.11.0-rc1/.../litertlm-android-0.11.0-rc1.aar
jni/arm64-v8a/libLiteRt.so
jni/arm64-v8a/libLiteRtClGlAccelerator.so
jni/arm64-v8a/liblitertlm_jni.so
```

**No QNN libs are in the AAR.** All Qualcomm-specific .so files come from `jniLibs/`. This is why `pickFirsts` in build.gradle.kts was unnecessary — no duplicates between AAR and `jniLibs`.

## Why exact byte counts matter

The first time NPU was attempted, our `jniLibs/` contained QNN libs pulled from the device's `/vendor/lib64/snap/`:

| Lib | Device version | Sample (QAIRT 2.42) version |
|---|---:|---:|
| `libQnnHtp.so` | 1,905,896 | 2,778,176 |
| `libQnnHtpV79Stub.so` | 447,952 | 679,168 |
| `libQnnSystem.so` | 264,176 | 2,983,560 |

Different sizes ⇒ different ABI ⇒ symbol resolution issues at runtime. The dispatch lib is built against the QAIRT 2.42 versions; bundling the device's older copies caused dlopen to silently fail because the dispatch lib couldn't resolve symbols against them. **All six NPU libs must come from the same QAIRT release** for the symbol set to match.

## Where to download

Direct GitHub raw URLs (verified 2026-04-30):

```
https://github.com/google-ai-edge/litert-samples/raw/main/compiled_model_api/qualcomm/llm_chatbot_npu/app/src/main/jniLibs/arm64-v8a/libLiteRtDispatch_Qualcomm.so
https://github.com/google-ai-edge/litert-samples/raw/main/compiled_model_api/qualcomm/llm_chatbot_npu/app/src/main/jniLibs/arm64-v8a/libGemmaModelConstraintProvider.so
https://github.com/google-ai-edge/litert-samples/raw/main/compiled_model_api/qualcomm/llm_chatbot_npu/app/src/main/jniLibs/arm64-v8a/libQnnHtp.so
https://github.com/google-ai-edge/litert-samples/raw/main/compiled_model_api/qualcomm/llm_chatbot_npu/app/src/main/jniLibs/arm64-v8a/libQnnHtpV79Skel.so
https://github.com/google-ai-edge/litert-samples/raw/main/compiled_model_api/qualcomm/llm_chatbot_npu/app/src/main/jniLibs/arm64-v8a/libQnnHtpV79Stub.so
https://github.com/google-ai-edge/litert-samples/raw/main/compiled_model_api/qualcomm/llm_chatbot_npu/app/src/main/jniLibs/arm64-v8a/libQnnSystem.so
```

## What we deliberately do NOT bundle

- `libLiteRtCompilerPlugin_Qualcomm.so` — this is for **classical-model NPU JIT** (image segmentation, object detection). Not needed for LLM. Removed from `jniLibs/`.
- `libQnnHtpPrepare.so` — JIT-compile flow for non-pre-compiled tflite models. We use a pre-compiled `.litertlm`, not JIT.

## Packaging settings

`app/build.gradle.kts`:

```kotlin
packaging {
    jniLibs {
        // LiteRT dispatch does a filesystem readdir on applicationInfo.nativeLibraryDir to
        // load libLiteRtDispatch_Qualcomm.so; default extraction (Android 10+) leaves that
        // dir empty and NPU init fails.
        useLegacyPackaging = true
    }
}
```

**`useLegacyPackaging = true` is required.** Without it, native libs stay inside the APK and aren't extracted to `nativeLibraryDir`. The dispatch lib's lookup mechanism does a filesystem `readdir` on that directory; an empty directory means the dispatch lib effectively can't be found, and NPU init fails silently.

`pickFirsts` was removed — verified there are no duplicate libs across AAR and `jniLibs/`.
