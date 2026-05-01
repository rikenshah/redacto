# ShieldText — Implementation Plan
**App**: Zero-trust, on-device PII/PHI redaction for frontline professionals  
**Team**: Edge Artists (Bhavik, Jaydeep, Riken, Tirth)  
**Target device**: Samsung Galaxy S25 Ultra (Snapdragon 8 Elite)  
**Stack**: Kotlin + Jetpack Compose, LiteRT-LM, ML Kit OCR, QNN NPU delegate  
**Starting point**: StarterHack (bare Compose scaffold, minSdk=31)  

---

## Verified Dependency Coordinates

All coordinates confirmed against live Maven repos and official docs (April 2026):

```kotlin
// LiteRT-LM inference engine — group is litertlm NOT litert
implementation("com.google.ai.edge.litertlm:litertlm-android:latest.release")

// QNN NPU delegate — on Maven Central (NOT a manual .so hunt)
implementation("com.qualcomm.qti:qnn-litert-delegate:2.34.0")
implementation("com.qualcomm.qti:qnn-runtime:2.34.0")

// ML Kit OCR
implementation("com.google.mlkit:text-recognition:16.0.1")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
```

---

## Model: Gemma 4 E2B

**Source**: `litert-community/gemma-4-E2B-it-litert-lm` on Hugging Face  
**License**: Apache 2.0 — no gating, free to download  
**File format**: `.litertlm`  
**Total size**: ~2.58 GB (0.79 GB text decoder + 1.12 GB embeddings memory-mapped + vision on demand)  
**E4B alternative**: `litert-community/gemma-4-E4B-it-litert-lm` (~4 GB) — use only if RAM allows

### Model delivery for hackathon (ADB push — confirmed approach)

Do NOT bundle in `assets/` — 2.58 GB APK is rejected by the Play Store pipeline and
causes OOM during packaging. Push directly to the app's private files dir:

```bash
# Download on home network before hackathon day
huggingface-cli download litert-community/gemma-4-E2B-it-litert-lm \
  --local-dir ./model/

# Push to device
adb push ./model/gemma-4-E2B-it.litertlm \
  /sdcard/Android/data/com.example.starterhack/files/gemma4.litertlm
```

The app reads from `context.getExternalFilesDir(null)` — no MANAGE_EXTERNAL_STORAGE
permission needed for this path.

---

## Risk Register (All Researched & Mitigated)

| Risk | Status | Mitigation |
|---|---|---|
| LiteRT-LM not on Maven | **CLEAR** | `com.google.ai.edge.litertlm:litertlm-android:latest.release` is public |
| Gemma 4 not in .litertlm format | **CLEAR** | E2B + E4B both on HuggingFace, Apache 2.0, no gating |
| QNN .so files not available | **CLEAR** | `com.qualcomm.qti:qnn-litert-delegate:2.34.0` on Maven Central |
| 2.58 GB model on hackathon Wi-Fi | **MITIGATED** | Pre-download at home; ADB push on day — bring USB drive backup |
| NPU init failure on device | **MITIGATED** | NPU → GPU → CPU fallback cascade (see Phase 2) |
| OOM during inference | **MITIGATED** | Embeddings are memory-mapped; `largeHeap=true`; sliding window at 512 tokens |
| engine.initialize() blocking UI | **MITIGATED** | Runs on `Dispatchers.IO`; splash/loading state in ViewModel |

---

## Phase 1 — Dependencies & Manifest
**Owner**: Riken | **Time**: 30 min

### `app/build.gradle.kts`

Add the dependencies above plus restrict ABI to `arm64-v8a`:

```kotlin
android {
    defaultConfig {
        ndk { abiFilters += "arm64-v8a" }
    }
}
```

### `AndroidManifest.xml`

```xml
<application
    android:largeHeap="true"
    ...>
    <uses-permission android:name="android.permission.CAMERA" />
</application>
```

No INTERNET permission needed — zero-trust, offline-only.

---

## Phase 2 — Inference Engine
**Owner**: Riken + Jaydeep | **Time**: 2 h

### File layout

```
engine/
  InferenceEngine.kt      ← Engine lifecycle, generate(), fallback cascade
  RedactionMode.kt        ← enum: HIPAA, TACTICAL, JOURNALISM, FIELD_SERVICE, FINANCIAL
  SystemPrompts.kt        ← per-mode few-shot prompt strings
  RegexFallback.kt        ← parallel SSN / email / phone patterns
```

### Correct LiteRT-LM API (Engine, not LlmInference)

The API shipped in the public release uses `Engine`/`EngineConfig`, not `LlmInference`:

```kotlin
class InferenceEngine(private val context: Context) {

    private lateinit var engine: Engine
    private lateinit var conversation: Conversation

    suspend fun initialize(modelPath: String) = withContext(Dispatchers.IO) {
        // Try NPU first, cascade to GPU, then CPU
        val backend = tryNpu() ?: tryGpu() ?: Backend.CPU()
        val config = EngineConfig(
            modelPath = modelPath,
            backend = backend
        )
        engine = Engine(config)
        engine.initialize()          // up to 10s — must be off main thread
        conversation = engine.createConversation()
    }

    private fun tryNpu(): Backend? = runCatching {
        Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir)
    }.getOrNull()

    private fun tryGpu(): Backend? = runCatching {
        Backend.GPU()
    }.getOrNull()

    suspend fun redact(text: String, mode: RedactionMode): String =
        withContext(Dispatchers.Default) {
            val prompt = SystemPrompts.build(mode, text)
            // Run LLM and regex in parallel
            val llmDeferred = async { runLlm(prompt) }
            val regexHits  = RegexFallback.scan(text)
            val llmResult  = llmDeferred.await()
            RegexFallback.merge(llmResult, regexHits)
        }

    private suspend fun runLlm(prompt: String): String {
        val sb = StringBuilder()
        conversation.sendMessage(prompt).collect { token -> sb.append(token) }
        return sb.toString()
    }
}
```

### `RedactionMode.kt`

```kotlin
enum class RedactionMode(val label: String) {
    HIPAA("Medical / HIPAA"),
    TACTICAL("Tactical / First Responder"),
    JOURNALISM("Journalism / Whistleblower"),
    FIELD_SERVICE("Field Service"),
    FINANCIAL("Financial / Legal")
}
```

### `SystemPrompts.kt` structure

Each mode gets a system instruction + 5 few-shot "Input → Output" pairs from
the use-cases docs. Key constraints baked into every prompt:

- Temperature set to near-zero via `EngineConfig` options (check API for param name)
- "Respond with ONLY the redacted text. Replace sensitive items with [CATEGORY_N]."
- Examples drawn from `edgeArtists-context/use-cases/` docs

### `RegexFallback.kt` patterns

```kotlin
object RegexFallback {
    private val patterns = mapOf(
        "SSN"   to Regex("""\b\d{3}-\d{2}-\d{4}\b"""),
        "EMAIL" to Regex("""[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}"""),
        "PHONE" to Regex("""\b(\+1[-.\s]?)?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}\b"""),
        "MRN"   to Regex("""\bMRN[:\s#]*\d{6,10}\b""", RegexOption.IGNORE_CASE),
        "DOB"   to Regex("""\b(0[1-9]|1[0-2])[-/](0[1-9]|[12]\d|3[01])[-/](19|20)\d{2}\b""")
    )

    fun scan(text: String): List<MatchResult> = patterns.flatMap { (_, re) -> re.findAll(text).toList() }

    fun merge(llmOutput: String, hits: List<MatchResult>): String {
        // force-redact anything regex caught that the LLM missed
        ...
    }
}
```

---

## Phase 3 — OCR Pipeline
**Owner**: Tirth | **Time**: 1 h

```
engine/
  OcrProcessor.kt         ← ML Kit TextRecognizer wrapper
  CameraPermission.kt     ← CameraX permission handling
```

```kotlin
class OcrProcessor {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun process(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                val text = result.textBlocks.joinToString("\n") { it.text }
                cont.resume(text.normalizeWhitespace())
            }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    private fun String.normalizeWhitespace() = trim().replace(Regex("""\s{2,}"""), " ")
}
```

---

## Phase 4 — ViewModel & State
**Owner**: Riken | **Time**: 45 min

```
ui/
  RedactionViewModel.kt
  RedactionUiState.kt
```

```kotlin
sealed interface RedactionUiState {
    data object Idle    : RedactionUiState
    data object Loading : RedactionUiState     // engine init or inference running
    data class  Success(val original: String, val redacted: String, val backend: String) : RedactionUiState
    data class  Error(val message: String) : RedactionUiState
}
```

ViewModel holds `InferenceEngine` and `OcrProcessor`. Exposes:
- `selectedMode: StateFlow<RedactionMode>`
- `uiState: StateFlow<RedactionUiState>`
- `fun selectMode(mode: RedactionMode)`
- `fun redactText(input: String)`
- `fun redactImage(bitmap: Bitmap)`

Engine initializes lazily on first use (show loading state during warmup).

---

## Phase 5 — Compose UI
**Owner**: Bhavik + Riken | **Time**: 2–3 h

```
ui/screens/
  HomeScreen.kt           ← mode selector + text input
  ScannerScreen.kt        ← CameraX preview
  ResultScreen.kt         ← before/after + heatmap
ui/components/
  ModeChip.kt
  RedactionHeatmap.kt
  BeforeAfterToggle.kt
  ZeroTrustBadge.kt
```

### Key components

**ModeSelector** — horizontally scrollable `LazyRow` of `FilterChip`s.

**RedactionHeatmap** — use `buildAnnotatedString` with `SpanStyle` to overlay
highlights on redacted spans. Color encodes category: red = name, orange = location,
yellow = date, etc. This is the visual "wow" moment for judges.

**BeforeAfterToggle** — `AnimatedContent` crossfade between raw and redacted text.

**ZeroTrustBadge** — persistent pill on every screen:
`"100% On-Device  •  Airplane Mode Safe  •  [backend]"`
Swap `[backend]` dynamically from `Success.backend` field (NPU / GPU / CPU).

---

## Phase 6 — NPU Profiling & Optimization
**Owner**: Jaydeep | **Time**: ongoing alongside Phase 5+**

```bash
# Push LiteRT benchmark binary
adb push benchmark_model /data/local/tmp/
adb shell chmod +x /data/local/tmp/benchmark_model

# Benchmark with NPU
adb shell /data/local/tmp/benchmark_model \
  --graph=/sdcard/Android/data/com.example.starterhack/files/gemma4.litertlm \
  --use_npu=true \
  --num_runs=10

# Capture Perfetto trace for OCR → LLM hand-off latency
adb shell perfetto -c /dev/stdin --txt -o /data/misc/perfetto-traces/trace.pb
```

**Target**: <500ms time-to-first-redacted-token for a standard paragraph.

### Sliding window for long docs

Chunk input at 512 tokens, infer sequentially, concat. Implement in `InferenceEngine`:

```kotlin
fun chunkText(text: String, maxTokens: Int = 512): List<String> {
    val sentences = text.split(Regex("""(?<=[.!?])\s+"""))
    // bin sentences greedily into chunks ≤ maxTokens
    ...
}
```

---

## Phase 7 — Prompt Engineering
**Owner**: Tirth | **Time**: 1–2 h (runs in parallel with Phase 5)**

Use `edgeArtists-context/use-cases/` as the source for few-shot examples.
Document each prompt in `engine/SystemPrompts.kt` with inline comments explaining
*why* each example was chosen.

**HIPAA prompt** → 18 PHI identifiers + relational ("patient's daughter Lisa")  
**Tactical prompt** → keep suspect/vehicle, redact victim/witness  
**Journalism prompt** → source identity, meeting locations, political affiliations  
**Field Service** → customer PII + security details, keep technical specs  
**Financial** → SSNs, account numbers, transaction IDs

---

## Hackathon Critical Path

```
Hour  0:00–0:30   Phase 1  — deps, manifest, abiFilters
Hour  0:30–2:30   Phase 2  — InferenceEngine (HIPAA mode only, CPU backend first)
Hour  2:30–3:30   Phase 3  — OcrProcessor + CameraPermission
Hour  3:30–4:15   Phase 4  — ViewModel + UiState wiring
Hour  4:15–7:00   Phase 5  — HomeScreen + ResultScreen (Scanner last)
Hour  7:00–8:00   Phase 6  — NPU enablement + first profiler run
Hour  8:00–9:30   Phase 7  — remaining modes + prompt tuning
Hour  9:30–10:30  Polish   — ZeroTrustBadge, demo dataset, edge case testing
Hour 10:30+       Buffer   — ScannerScreen, benchmark slide, README
```

---

## Pre-Hackathon Checklist (Do Before April 30)

- [ ] Run `huggingface-cli download litert-community/gemma-4-E2B-it-litert-lm` on home Wi-Fi
- [ ] Copy model to USB drive as backup
- [ ] Verify `adb push` works to the S25 Ultra's files dir
- [ ] Add `google()` and `mavenCentral()` to `settings.gradle.kts` repositories block
- [ ] Sync project in Android Studio to confirm all deps resolve (no network needed day-of)
- [ ] Install Snapdragon Profiler and verify it connects to the S25 Ultra
- [ ] Pre-generate 50 fake PII document test cases for demo + judge testing
- [ ] Study `google-ai-edge/gallery` v1.0.12 source for Engine initialization pattern

---

## Reference Sources

- LiteRT-LM Android guide: https://ai.google.dev/edge/litert-lm/android
- LiteRT-LM GitHub (Kotlin getting started): https://github.com/google-ai-edge/LiteRT-LM
- Gemma 4 E2B model: https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm
- Gemma 4 E4B model: https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm
- QNN delegate Maven: https://central.sonatype.com/artifact/com.qualcomm.qti/qnn-litert-delegate
- QNN + LiteRT guide: https://ai.google.dev/edge/litert/android/npu/qualcomm
- Gallery source (reference impl): https://github.com/google-ai-edge/gallery
- Gemma 4 + QNN deep dive: https://medium.com/google-developer-experts/bringing-multimodal-gemma-4-e2b-to-the-edge-a-deep-dive-into-litert-lm-and-qualcomm-qnn-4e1e06f3030c
