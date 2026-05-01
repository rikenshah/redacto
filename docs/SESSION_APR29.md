# Session — April 29, 2026

## What We Did

### 1. Committed clean state
- Staged and committed all previously-untracked project files: `build.gradle.kts`, `settings.gradle.kts`, `app/.gitignore`, `.idea/`
- InferenceEngine GPU crash fix (Android 16 SIGSEGV) was committed
- Pushed to `origin/main`: commit `e46b746`

### 2. Discovered SM8750 model (big deal for demo)
Fetched https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/tree/main — a **new file appeared** since last session:

| File | Size | Chip |
|---|---|---|
| `gemma-4-E2B-it.litertlm` | 2.58 GB | Generic CPU |
| `gemma-4-E2B-it_qualcomm_qcs8275.litertlm` | 3.29 GB | Dragonwing IQ-8275 (IoT, not phones) |
| **`gemma-4-E2B-it_qualcomm_sm8750.litertlm`** | **3.02 GB** | **SM8750 = Snapdragon 8 Elite = S25 Ultra NPU** ✅ |

**No code changes needed** — `InferenceEngine.kt` already cascades NPU → CPU. Just download and push.

To use on S25 Ultra when ready:
```bash
hf download litert-community/gemma-4-E2B-it-litert-lm \
  gemma-4-E2B-it_qualcomm_sm8750.litertlm --local-dir model/

adb -s <S25_SERIAL> push model/gemma-4-E2B-it_qualcomm_sm8750.litertlm \
  /sdcard/Android/data/com.example.starterhack/files/gemma4.litertlm

adb -s <S25_SERIAL> shell rm -rf /data/data/com.example.starterhack/cache/
```

### 3. Implemented Image Mode

**Before:** Camera → OCR → LLM → shows *redacted text* with color heatmap  
**After:** Camera / Gallery → OCR with bounding boxes → LLM → black boxes drawn on original image bitmap → shows image in ResultScreen with before/after toggle

#### Files changed

| File | Change |
|---|---|
| `engine/OcrProcessor.kt` | Added `OcrResult`, `OcrElement(text, Rect)`, `processWithBounds()` using ML Kit word-level elements |
| `ui/RedactionUiState.kt` | Added `sourceBitmap: Bitmap?` and `redactedBitmap: Bitmap?` to `Success` |
| `ui/RedactionViewModel.kt` | `redactImage()` now produces a redacted bitmap: OCR → LLM → word diff → Canvas black boxes |
| `ui/components/RedactedImageView.kt` | **NEW** — Compose `Image` composable for displaying bitmaps |
| `ui/screens/ResultScreen.kt` | Branches on `isImageMode` — shows image before/after instead of text heatmap; hides copy button in image mode |
| `ui/screens/ScannerScreen.kt` | Added gallery picker (`PickVisualMedia`) in top-bar for emulator/demo testing without camera |
| `engine/RegexOnlyLlmEngine.kt` | **NEW** — debug-only fallback engine using `RegexFallback`; no model file needed |
| `ui/RedactionViewModel.kt` (factory) | In `DEBUG` builds with no model file, injects `RegexOnlyLlmEngine` instead of showing ModelMissing screen |
| `app/build.gradle.kts` | Added `buildConfig = true` to `buildFeatures` (required for `BuildConfig.DEBUG` in AGP 9.x) |

#### Image redaction algorithm — step by step

**High-level:**
```
OCR → List<OcrElement(text, Rect)>  [word-level, from ML Kit Element objects]
    ↓
fullText = elements joined as string
    ↓
LLM / RegexOnlyEngine → redactedText with [CATEGORY_N] placeholders
    ↓
redactedWordSet = elements whose .text does NOT appear in redactedText
    ↓
Canvas.drawRect(BLACK) over each element in redactedWordSet on a bitmap copy
    ↓
Success(sourceBitmap = original, redactedBitmap = blackBoxed)
```

---

**Step 1 — Camera or gallery captures a Bitmap**

`ScannerScreen` either fires `ImageCapture.takePicture()` (CameraX) or the user picks via `PickVisualMedia`. Both paths produce an `android.graphics.Bitmap` which is passed to `viewModel.redactImage(bitmap)`.

For gallery picks on Android 9+, `ImageDecoder.decodeBitmap()` is called with `ALLOCATOR_SOFTWARE` to avoid getting a hardware-backed bitmap (hardware bitmaps can't be drawn on with Canvas).

---

**Step 2 — ML Kit OCR extracts word-level elements with pixel coordinates**

`OcrProcessor.processWithBounds(bitmap)` calls ML Kit's `TextRecognition` which returns a tree:

```
Text
  └── TextBlock  (paragraph)
        └── Line
              └── Element  ← we collect these (roughly word-level)
                    ├── .text   e.g. "Goldie"
                    └── .boundingBox  e.g. Rect(120, 310, 290, 345) in bitmap pixels
```

We flatten all `Element` objects across all blocks into `List<OcrElement(text, boundingBox)>`. We also join all `TextBlock.text` values into a single `fullText` string for the LLM.

Hardware bitmap guard: if the bitmap's config is `HARDWARE`, it's copied to `ARGB_8888` first — ML Kit requires a software bitmap.

---

**Step 3 — LLM (or regex debug engine) redacts the full text**

`engine.redact(ocrResult.fullText, mode)` sends the plain OCR text to the LLM with a system prompt for the selected mode (HIPAA, Tactical, etc.). The LLM returns redacted text like:

```
Original:  "Goldie R Chisolm  (706) 296-9964  1195 Holly Street"
Redacted:  "[NAME_1]  [PHONE_1]  [LOCATION_1]"
```

In debug/emulator mode, `RegexOnlyLlmEngine` does the same replacement but using regex patterns — phone numbers, SSNs, dates, MRNs get replaced. Names and addresses are LLM-only.

---

**Step 4 — Word diff: which original words are now absent?**

```kotlin
val redactedWordSet = ocrResult.elements
    .map { it.text.lowercase() }
    .filter { word -> !redactedText.contains(word, ignoreCase = true) }
    .toSet()
```

Logic: for each word the OCR found, check if that exact string still exists anywhere in the LLM's output. If the LLM replaced it with `[NAME_1]`, the word "Goldie" will no longer be present → it goes into `redactedWordSet`.

This handles multi-word entities automatically: if "Goldie R Chisolm" → `[NAME_1]`, then "Goldie", "R", and "Chisolm" are each individually absent from the output and all get flagged.

Edge cases:
- Common words ("the", "at", "in") might be in the redacted set if the LLM removed a sentence they were part of — acceptable false-positive for a demo
- Numbers like "62.69" (weight) won't be absent since the LLM keeps non-PHI values → no box drawn

---

**Step 5 — Draw black rectangles on a mutable copy of the bitmap**

```kotlin
val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)  // mutable software copy
val canvas = Canvas(mutable)
val paint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }

ocrResult.elements
    .filter { it.text.lowercase() in redactedWordSet }
    .forEach { canvas.drawRect(RectF(it.boundingBox), paint) }
```

Each flagged `OcrElement.boundingBox` is a `Rect` in the same pixel coordinate space as the original bitmap (ML Kit returns coords relative to the input image). We cast to `RectF` for the Canvas API and fill with solid black.

The original `bitmap` is never modified — `mutable` is a separate copy.

This runs on `Dispatchers.Default` (off the main thread) to avoid janking the UI.

---

**Step 6 — Both bitmaps stored in UiState and displayed**

```kotlin
_uiState.value = RedactionUiState.Success(
    original = ocrResult.fullText,   // OCR text (for reference)
    redacted = redactedText,          // LLM output (for reference)
    backend = engine.activeBackend,
    sourceBitmap = bitmap,            // original unmodified image
    redactedBitmap = mutableBitmap,  // image with black boxes
)
```

`ResultScreen` detects `isImageMode = success.redactedBitmap != null` and renders `RedactedImageView(bitmap)` instead of the text heatmap. The "Original" / "Redacted" toggle swaps between `sourceBitmap` and `redactedBitmap`.

---

Hardware bitmap guard: `OcrProcessor.processWithBounds()` converts hardware bitmaps to ARGB_8888 before passing to ML Kit.

### 4. Emulator setup for testing

- Emulator: `emulator-5554`, ARM64, Android 17 — native libs work
- Cleared old 2.4 GB model from `/sdcard/Android/data/com.example.starterhack/files/` → freed 2.4 GB
- Pushed sample medical record image: `/sdcard/Pictures/sampleMedicalRecord.png`
- Built + installed latest debug APK

**To test image mode on emulator:**
1. Open ShieldText — home screen loads instantly (no model setup; `Regex-Debug` badge visible top-right)
2. Tap camera icon → Scanner screen
3. Tap **photo icon** (top-right) → pick `sampleMedicalRecord.png` from gallery
4. Wait ~2–5s for OCR + regex → navigates to Result screen
5. Result shows medical record with **black boxes** over: phone numbers `(706) 296-9964`, `(202) 452-9485`, `(323) 650-1865`, and date `12/9/2018`
6. Toggle "Original" ↔ "Redacted" buttons to verify before/after

> Names and addresses won't be boxed by regex (needs real LLM). Phone + dates will be.

### 5. Current git state

Changes **staged but NOT committed yet** (waiting for validation):
- `engine/OcrProcessor.kt`
- `ui/RedactionUiState.kt`
- `ui/RedactionViewModel.kt`
- `ui/components/RedactedImageView.kt` (new)
- `ui/screens/ResultScreen.kt`
- `ui/screens/ScannerScreen.kt`
- `engine/RegexOnlyLlmEngine.kt` (new)

Plus unstaged:
- `app/build.gradle.kts` (buildConfig = true)

**Do not push until image mode is validated on device.**

---

## Next Steps

### Immediate (hackathon day)
- [ ] Validate image mode on emulator with sample medical record
- [ ] Commit + push image mode changes
- [ ] Download `gemma-4-E2B-it_qualcomm_sm8750.litertlm` (3.02 GB) for S25 Ultra NPU demo
- [ ] Push SM8750 model to S25 Ultra, verify NPU backend activates in logcat
- [ ] Full end-to-end test on S25 Ultra: scan medical record → black boxes appear

### Nice to have
- [ ] "Save to gallery" button on image result screen
- [ ] Pinch-to-zoom on the redacted image view
- [ ] Latency badge: show time-to-redact in the ZeroTrustBadge or result screen

---

## Key Technical Reminders

- **GPU is permanently disabled** in `InferenceEngine.kt` — Android 16 SIGSEGV from misaligned `libLiteRtOpenClAccelerator.so` (litert 2.1.x). Tracked at https://github.com/google-ai-edge/LiteRT/issues/6299
- **NPU on S24 still broken** — `libLiteRtDispatch_Qualcomm.so` missing. NPU on S25 Ultra with SM8750 model is untested but should work.
- **Model path**: `/sdcard/Android/data/com.example.starterhack/files/gemma4.litertlm` (regardless of which model variant)
- **ADB serial**: S24 = `RFCY71V3QBT`; S25 Ultra serial TBD
- **Build**: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew assembleDebug`
