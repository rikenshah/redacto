# ShieldText — Progress & Next Steps

**App**: Zero-trust, on-device PII/PHI redaction  
**Team**: Edge Artists (Bhavik, Jaydeep, Riken, Tirth)  
**Device**: Samsung Galaxy S25 Ultra (SM-S938U1 · Snapdragon 8 Elite SM8750 · HTP V79)  
**Demo mode**: Airplane mode, fully offline  

---

## Original Design Goals

### Visual / UX
- Dark minimal theme, clean font (DM Sans variable weight, bundled offline)
- Landing page with **two clear mode cards**: Text and Image — tap to navigate
- No buried camera icon; modes are equal first-class citizens
- Live HUD on both modes: backend chip (NPU/GPU/CPU), latency, tok/s, current draw (mA), energy (J)
- "Zero Trust" badge on every screen confirming on-device only

### Functional
- **Text mode**: paste/type content → LLM redacts PII/PHI → heatmap highlighting by category (names, locations, dates, contact, IDs)
- **Image mode**: camera or gallery → ML Kit OCR → LLM → black boxes drawn over PII pixels on the original image → before/after toggle
- Five redaction modes with distinct system prompts: HIPAA, Tactical, Journalism, Field Service, Financial
- Regex fallback runs in parallel with LLM, catches structured PII (SSN, phone, email, MRN) the model misses
- All inference on NPU (Hexagon V79) — target <500 ms time-to-first-token for a paragraph

---

## What's Built (as of Apr 30, 2026)

### UI — Done ✅
| Component | File | Notes |
|---|---|---|
| Dark theme + DM Sans | `ui/theme/Type.kt`, `res/font/dm_sans.ttf` | Variable font, 100–900 weight axis, bundled offline |
| Landing screen (two mode cards) | `ui/screens/LandingScreen.kt` | Text + Image cards, ZeroTrustBadge, HUD for last-run metrics |
| Text redaction screen | `ui/screens/HomeScreen.kt` | Mode selector, OutlinedTextField, live HUD during inference |
| Scanner screen | `ui/screens/ScannerScreen.kt` | CameraX capture + gallery picker (PickVisualMedia) |
| Result screen | `ui/screens/ResultScreen.kt` | Before/after toggle, RedactionHeatmap (text), RedactedImageView (image), copy button |
| Live HUD component | `ui/components/HudBox.kt` | Pulsing dot (live), backend chip colored by type, tok/s, latency, mA, joules |
| ZeroTrustBadge | `ui/components/ZeroTrustBadge.kt` | Persistent on every screen |
| Redaction heatmap | `ui/components/RedactionHeatmap.kt` | AnnotatedString with category colors |
| Navigation | `navigation/NavGraph.kt` | LANDING → TEXT/SCANNER → RESULT |

### Inference Engine — Done ✅
| Component | File | Notes |
|---|---|---|
| LlmEngine interface | `engine/LlmEngine.kt` | `initialize`, `redact`, `activeBackend`, `lastMetrics`, `close` |
| InferenceEngine | `engine/InferenceEngine.kt` | NPU via LiteRT-LM; ADSP_LIBRARY_PATH + LD_LIBRARY_PATH set before init; metrics tracking |
| InferenceMetrics | `engine/InferenceMetrics.kt` | latencyMs, tokenCount, tokensPerSec, currentDrawMa, computed energyJoules |
| RegexOnlyLlmEngine | `engine/RegexOnlyLlmEngine.kt` | Debug fallback — no model needed; used automatically in DEBUG when model file absent |
| RedactionMode | `engine/RedactionMode.kt` | HIPAA, TACTICAL, JOURNALISM, FIELD_SERVICE, FINANCIAL |
| SystemPrompts | `engine/SystemPrompts.kt` | Per-mode few-shot system prompts |
| RegexFallback | `engine/RegexFallback.kt` | SSN, email, phone, MRN, DOB patterns; merged into LLM output |
| OcrProcessor | `engine/OcrProcessor.kt` | ML Kit word-level OCR with bounding boxes; hardware-bitmap guard |

### Image Redaction Pipeline — Done ✅
```
CameraX / PickVisualMedia → Bitmap
  → OcrProcessor.processWithBounds() → List<OcrElement(text, Rect)>
  → InferenceEngine.redact(fullText, mode) → redactedText with [TAG_N] placeholders
  → word diff (elements whose .text is absent from redactedText)
  → Canvas.drawRect(BLACK) on bitmap copy for each absent element
  → Success(sourceBitmap, redactedBitmap) → ResultScreen before/after toggle
```

### Native / NPU Setup — Done ✅ (init still crashing, see below)
| Thing | Status |
|---|---|
| `litertlm-android:0.11.0-rc1` | ✅ Includes `libLiteRt.so` required by dispatch |
| `qnn-litert-delegate:2.42.0` + `qnn-runtime:2.42.0` | ✅ Upgraded from 2.34.0 |
| `libLiteRtDispatch_Qualcomm.so` (V79 variant) | ✅ Extracted from LiteRT v2.1.1 GitHub release `litert_npu_runtime_libraries.zip`; in `jniLibs/arm64-v8a/` |
| `useLegacyPackaging = true` | ✅ Forces .so extraction to filesystem so dispatch `readdir` can find libs |
| `ADSP_LIBRARY_PATH` + `LD_LIBRARY_PATH` set to `nativeLibraryDir` | ✅ Set via `Os.setenv` before `Backend.NPU()` call — required so Hexagon DSP finds `libQnnHtpV79Skel.so` |
| `<uses-native-library android:name="libcdsprpc.so" required="false"/>` | ✅ Unlocks vendor DSP RPC lib in linker namespace |
| SM8750 model pushed to device | ✅ `gemma-4-E2B-it_qualcomm_sm8750.litertlm` (3.01 GB) at `/sdcard/Android/data/com.example.starterhack/files/gemma4.litertlm` |

---

## Current Blocker: NPU Init Crash

### Crash sequence
```
I litert  : [litert_dispatch.cc] Loading shared library: .../libLiteRtDispatch_Qualcomm.so  ✅
E litert  : [dispatch_delegate.cc:115] Failed to initialize Dispatch API
E litert  : [dispatch_delegate.cc:130] No usable Dispatch runtime found
F libc    : Fatal signal 6 (SIGABRT)
```

The dispatch .so **loads** successfully. `QnnManager::Create()` inside it then fails. The SIGABRT is uncatchable from Kotlin — it kills the process before any fallback can run.

### What we know
- `libQnnHtp.so`, `libQnnHtpPrepare.so`, `libQnnHtpV79Skel.so`, `libQnnHtpV79Stub.so`, `libQnnSystem.so` are all present in the extracted native lib dir (confirmed via `adb shell ls`)
- Samsung S25 vendor partition has pre-installed QNN at `/vendor/lib64/snap/libQnnHtp.so` and `/vendor/lib64/rfs/dsp/snap/libQnnHtpV79Skel.so`
- `libLiteRtDispatch_Qualcomm.so` is from LiteRT v2.1.1 (December 2025), compiled against QAIRT 2.42.0
- LiteRT v2.1.2–v2.1.4 releases do NOT include `litert_npu_runtime_libraries.zip` — no newer prebuilt dispatch available on GitHub
- `litertlm-android:0.11.0-rc1` uses `libLiteRt.so` from LiteRT v2.1.4 (April 2026) — possible ABI gap with the v2.1.1 dispatch library

### Most likely root causes (in order of likelihood)
1. **ABI mismatch** between `libLiteRtDispatch_Qualcomm.so` (v2.1.1) and `libLiteRt.so` (v2.1.4 via `litertlm-android:0.11.0-rc1`)
2. **ADSP path not reaching the DSP** — Samsung may restrict `ADSP_LIBRARY_PATH` overrides for app processes; DSP can't find our skel
3. **Vendor QNN version conflict** — our bundled `libQnnHtp.so` (QAIRT 2.42.0 Maven) vs Samsung's vendor version that the S25 NPU firmware expects

### Next steps to try (in order)

**Option A — Use vendor skel path (most likely to work)**
```bash
# Check exact vendor DSP path on S25
adb shell ls /vendor/lib/rfsa/adsp/
adb shell ls /vendor/dsp/cdsp/
adb shell ls /vendor/lib64/rfs/dsp/snap/

# Set ADSP_LIBRARY_PATH to include vendor DSP path + our lib dir:
# In configureNativeRuntime(), prepend vendor skel paths before nativeLibDir
```
In `InferenceEngine.kt`, extend the `ADSP_LIBRARY_PATH` to also include `/vendor/lib64/rfs/dsp/snap` or wherever `libQnnHtpV79Skel.so` lives on this device:
```kotlin
val vendorSkelPaths = listOf(
    "/vendor/lib64/rfs/dsp/snap",
    "/vendor/dsp/cdsp",
    "/vendor/lib/rfsa/adsp",
)
val adspPath = (listOf(nativeLibraryDir) + vendorSkelPaths).joinToString(":")
Os.setenv("ADSP_LIBRARY_PATH", adspPath, true)
```

**Option B — Remove bundled QNN libs, rely on vendor**  
Remove `libQnnHtp.so`, `libQnnHtpV79Stub.so`, `libQnnSystem.so` etc. from `jniLibs/arm64-v8a/`. The dispatch library will fall back to dlopen system search path, potentially picking up Samsung's pre-installed vendor QNN which is guaranteed compatible with the S25 NPU firmware. Keep only `libLiteRtDispatch_Qualcomm.so` in jniLibs.

**Option C — Build dispatch from source**  
Clone `google-ai-edge/LiteRT`, check out v2.1.4 tag, build `litert_npu_runtime_libraries` for V79. This produces a dispatch library that links against the same `libLiteRt.so` as `litertlm-android:0.11.0-rc1`. Requires Bazel build environment.

**Option D — Downgrade litertlm to match v2.1.1 dispatch**  
Try `litertlm-android:0.10.0` or whichever release corresponds to LiteRT v2.1.1 (December 2025). The dispatch library was compiled against that era's `libLiteRt.so` ABI.

---

## ADB / Build Cheat Sheet

```bash
# Build
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug

# Install (replace serial)
adb -s <SERIAL> install -r app/build/outputs/apk/debug/app-debug.apk

# Stream relevant logcat
adb -s <SERIAL> logcat -v time | grep -E "InferenceEngine|litert|dispatch|QnnManager|SIGABRT|libc"

# Verify native libs extracted to app's lib dir
adb -s <SERIAL> shell ls /data/app/$(adb -s <SERIAL> shell pm path com.example.starterhack | cut -d: -f2 | sed 's|/base.apk||')/lib/arm64/

# Model push (after downloading)
adb -s <SERIAL> push model/gemma-4-E2B-it_qualcomm_sm8750.litertlm \
  /sdcard/Android/data/com.example.starterhack/files/gemma4.litertlm

# Clear JIT cache after reinstall
adb -s <SERIAL> shell rm -rf /data/data/com.example.starterhack/cache/
```

**Device serials**: S24 = `RFCY71V3QBT` | S25 Ultra = check `adb devices`

---

## Reference Links

- LiteRT-LM Android guide: https://ai.google.dev/edge/litert-lm/android  
- LiteRT GitHub releases (dispatch zip): https://github.com/google-ai-edge/LiteRT/releases  
- litert-samples NPU example: `compiled_model_api/qualcomm/llm_chatbot_npu/` in google-ai-edge/litert-samples  
- Gemma 4 E2B SM8750 model: https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm  
- QNN + LiteRT setup guide: https://ai.google.dev/edge/litert/android/npu/qualcomm  
- GPU crash issue (Android 16): https://github.com/google-ai-edge/LiteRT/issues/6299  
