# Redacto — Code Walkthrough for Demo

**Purpose:** Quick reference for answering technical questions during the hackathon demo.

---

## How LiteRT-LM is Used

### The SDK

We use `com.google.ai.edge.litertlm:litertlm-android:0.11.0-rc1` — Google's on-device LLM runtime. Key classes from the SDK:

| Class | What it does |
|---|---|
| `Engine` | Loads a `.litertlm` model file into memory |
| `EngineConfig` | Configures backend (CPU/GPU/NPU), model path, token limit, cache dir |
| `Backend` | Factory for backend selection: `Backend.CPU()`, `Backend.GPU()`, `Backend.NPU(nativeLibraryDir)` |
| `Conversation` | A chat session — send prompts, receive streaming tokens |
| `ConversationConfig` | Sampling parameters (topK, topP, temperature) |
| `SamplerConfig` | Controls decoding: `topK=64, topP=0.95, temperature=1.0` for GPU/CPU; `null` for NPU |
| `MessageCallback` | Streaming interface: `onMessage(token)`, `onDone()`, `onError()` |
| `Contents` / `Content.Text` | Wraps the prompt text for the conversation API |

### Initialization Flow

```
App launches
  → ShieldTextApp.onCreate()
    → Set ADSP_LIBRARY_PATH (for Hexagon DSP)
    → Extract bundled model from APK assets (first launch only)
  → RedactionViewModel.init
    → checkModelAndInitialize()
      → Load native libs: LiteRt, litertlm_jni, LiteRtDispatch_Qualcomm
      → Try NPU → if fails → try GPU → if fails → try CPU
      → Engine(config).initialize()
      → engine.isReady = true
```

### How We Create an Engine

```kotlin
val config = EngineConfig(
    modelPath = "/sdcard/Android/data/com.example.redacto/files/gemma4_npu.litertlm",
    backend = Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir),
    visionBackend = Backend.CPU(),   // vision sub-model on CPU
    audioBackend = Backend.CPU(),    // audio sub-model on CPU
    maxNumTokens = 4000,
    maxNumImages = 1,
    cacheDir = context.cacheDir.absolutePath,  // AOT kernel cache
)
val engine = Engine(config)
engine.initialize()  // This is where the model actually loads into memory
```

### How We Run Inference

```kotlin
val conversation = engine.createConversation(
    ConversationConfig(
        samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0)
        // NPU uses null (constrained decoding not supported on Hexagon)
    )
)

val contents = Contents.of(listOf(Content.Text(prompt)))

conversation.sendMessageAsync(contents, object : MessageCallback {
    override fun onMessage(message: Message) {
        // Called once per token — we append to StringBuilder
        stringBuilder.append(message.toString())
        tokenCount++
    }
    override fun onDone() {
        // Inference complete — resume coroutine
    }
    override fun onError(throwable: Throwable) {
        // Handle error
    }
}, emptyMap())

conversation.close()  // Release native resources
```

### Key Points for Demo Q&A

- **No internet permission.** The manifest has zero network access. Everything runs on-device.
- **Streaming tokens.** `sendMessageAsync` streams tokens one at a time via `onMessage`. We measure TTFT from the first callback.
- **60-second timeout.** Each inference call has a `withTimeout(60_000L)` to prevent hanging if `onDone` never fires.
- **Conversation lifecycle.** Each pipeline step creates and closes its own conversation. No conversation reuse between steps.

---

## Model Delegation (NPU / GPU / CPU)

### The Three Backends

| Backend | Model File | How it works | Speed |
|---|---|---|---|
| **NPU** | `gemma4_npu.litertlm` (2.8GB) | Qualcomm Hexagon V79 DSP via QNN delegate | ~42 tok/s |
| **GPU** | `gemma4.litertlm` (2.4GB) | Adreno 830 via OpenCL delegate | ~25 tok/s |
| **CPU** | `gemma4.litertlm` (2.4GB) | XNNPack on ARM Cortex cores | Slowest |

### NPU Requires Special Setup

1. **Different model file.** The NPU model (`gemma4_npu.litertlm`) is pre-compiled for the SM8750 chip. It contains `DISPATCH_OP` operations that only work with Qualcomm's dispatch library. The GPU/CPU model has standard TFLite ops.

2. **Native library paths.** The Hexagon DSP needs `ADSP_LIBRARY_PATH` set BEFORE any QNN library loads. We do this in `ShieldTextApp.onCreate()`:
   ```kotlin
   Os.setenv("ADSP_LIBRARY_PATH", "/vendor/lib64/rfs/dsp/snap:...", true)
   ```

3. **Dispatch library.** `libLiteRtDispatch_Qualcomm.so` is bundled in the APK (from the LiteRT-LM AAR). It bridges LiteRT to the QNN runtime on the device.

4. **No sampler on NPU.** Constrained decoding errors with "not supported, error 12" on Hexagon. We pass `samplerConfig = null` for NPU.

### Cascade Fallback

```
User selects NPU
  → Try Backend.NPU(nativeLibDir) + gemma4_npu.litertlm
  → If fails: try Backend.GPU() + gemma4.litertlm
  → If fails: try Backend.CPU() + gemma4.litertlm
```

The cascade is in `RedactionViewModel.checkModelAndInitialize()`. Each step wraps `engine.initialize()` in `runCatching`. The failure happens at `initialize()`, not at `Engine()` construction — `Backend.NPU()` always constructs successfully even without the dispatch lib.

### Backend Switching at Runtime

When the user taps the backend dropdown:
1. `engine.close()` — releases native memory, nulls references, double GC
2. Clear AOT cache (327MB of compiled GPU kernels)
3. Wait 3 seconds for OS to reclaim memory
4. Re-initialize with the new backend

**Limitation:** NPU cannot re-init in the same process after switching away. QNN runtime holds DSP state that doesn't clean up. Need app restart for NPU after using GPU.

---

## The Multi-Pass Pipeline

### Text Redaction (4 steps)

```
Step 1: CLASSIFY (LLM call)
  "What kind of document is this?" → "Medical" / "Financial" / etc.

Step 2: DETECT (LLM call)
  "Find all PII in this text" → NAME: Jane Smith, SSN: 123-45-6789, ...

Step 3: REDACT (LLM call)
  "Replace these items with [CATEGORY_N] placeholders" → redacted text

Step 4: VALIDATE (LLM call)
  "Is anything still leaked?" → PASS or FAIL + missed items
  If FAIL → retry Steps 3+4 (max 3 rounds)
```

Each step gets a fresh conversation. Category-specific prompts are loaded based on Step 1's classification (Medical gets HIPAA rules, Financial gets GLBA rules, etc.).

### Image Redaction (indexed elements)

```
1. OCR scans the image → numbered elements: [0] Patient [1] Jane [2] Smith
2. LLM receives indexed elements, returns: 1:NAME 2:NAME
3. Index 1 → bounding box → draw black rectangle
4. Zero string matching — direct index-to-pixel mapping
```

### 7 Redaction Categories

Each has specialized detect + validate prompts:

| Category | What to redact | What to preserve |
|---|---|---|
| Medical | Names, dates, SSN, MRN, addresses | Diagnoses, medications, vitals |
| Financial | SSN, accounts, cards, routing numbers | Dollar amounts, institution names |
| Legal | Buyer/seller names, addresses | Property specs, legal terms |
| Tactical | Victim/witness names | Suspect descriptions, officer names |
| Journalism | Source names, meeting locations | Public officials, institutions |
| Field Service | Customer names, gate/alarm codes | Equipment details, fault codes |
| General | All PII (broad fallback) | Non-personal info |

---

## Performance Metrics (What the HUD Shows)

| Metric | How measured | What it means |
|---|---|---|
| **TOTAL** | Wall-clock time for entire pipeline | End-to-end user wait time |
| **TTFT** | Timestamp of first `onMessage` callback minus start | How fast the model starts responding |
| **TOK/S** | `(tokens - 1) * 1000 / (lastToken - firstToken)` | Pure decode speed, excludes prefill |

All metrics are reliable software measurements. We removed battery/energy metrics because software-based power measurement on Android is unreliable (system-wide, 1Hz sampling, no per-process isolation).

---

## Key Files Quick Reference

| File | What it does |
|---|---|
| `ShieldTextApp.kt` | Sets DSP paths, extracts bundled model on first launch |
| `InferenceEngine.kt` | LiteRT-LM wrapper — init, infer, metrics, close |
| `LlmEngine.kt` | Interface: initialize, redact, infer, close |
| `RedactionViewModel.kt` | Backend cascade, pipeline wiring, state management |
| `RedactionPipeline.kt` | 4-step text pipeline (classify→detect→redact→validate) |
| `ImageRedactionPipeline.kt` | Indexed-element image pipeline |
| `PipelinePrompts.kt` | All 7 category-specific prompt templates |
| `SystemPrompts.kt` | Legacy single-pass prompts (used as fallback) |
| `OcrProcessor.kt` | ML Kit OCR wrapper |
| `build.gradle.kts` | Dependencies, QNN .so packaging, noCompress for models |

---

## Common Demo Questions

**Q: How is this different from cloud redaction?**
A: No INTERNET permission in the manifest. Zero data leaves the device. The 2.8GB model runs entirely on the Snapdragon NPU.

**Q: Why LiteRT-LM and not ONNX/TensorFlow?**
A: LiteRT-LM is Google's purpose-built runtime for on-device LLM inference. It handles tokenization, conversation state, and streaming natively. It also provides first-class Qualcomm NPU support via the QNN delegate.

**Q: Why Gemma 4 E2B?**
A: It's instruction-tuned (required for following system prompts), small enough for mobile (2.4-2.8GB quantized), and has a pre-compiled NPU variant for SM8750 on HuggingFace.

**Q: Why multi-pass instead of single-pass?**
A: A single LLM call asking to simultaneously classify, detect, replace, and verify produces inconsistent results. Separating concerns lets each step focus with a purpose-built prompt. The validation step catches leaks the detection step missed.

**Q: What happens if inference hangs?**
A: 60-second timeout per LLM call. If it fires, the pipeline falls back to deterministic string replacement or the legacy single-pass prompt.

**Q: Can it run on other phones?**
A: GPU and CPU backends work on any arm64 Android 12+ device with 8GB+ RAM. NPU requires a Snapdragon 8 Elite (SM8750) with the pre-compiled model variant.
