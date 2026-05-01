# Benchmarking Session — ShieldText Domain Accuracy Testing

**Date:** April 30 – May 1, 2026
**Author:** Jaydeep Shah (Team Lead, Performance & AI Model Optimization)
**Team:** Edge Artists
**Branch:** `branchmarking` (10 commits ahead of `master`)
**Device:** Samsung Galaxy S25 Ultra (Snapdragon 8 Elite SM8750, Android 16 / API 36)

---

## 1. Objective

Build an on-device benchmarking system to compare the **standard Gemma 4 E2B** model against a **fine-tuned Gemma 4 E2B** model on PII/PHI redaction quality across all 5 ShieldText domain modes. Both models run on GPU (Adreno 830) for a fair hardware comparison. The goal: determine whether fine-tuning improves redaction accuracy enough to justify deployment.

---

## 2. Dataset Preparation

### Source
- **ai4privacy/pii-masking-400k** — 17,046 English entries with labeled PII spans and masked outputs

### Curation Pipeline
Two-phase approach: automated curation + hand-crafted contextual entries.

**Phase 1 — Automated (60 entries):**
- Filtered 17K entries down to 2,014 quality candidates (>80 chars, <600 chars, 3+ entities, no synthetic-looking names, PII density <60%)
- Assigned modes via keyword matching (medical terms → HIPAA, financial terms → FINANCIAL, password/gate code → FIELD_SERVICE) and label composition (CREDITCARDNUMBER/TAXNUM → FINANCIAL)
- Remapped labels per mode (e.g., GIVENNAME+SURNAME → `[NAME_N]` for HIPAA, `[CUSTOMER_N]` for FIELD_SERVICE, `[SOURCE_N]` for JOURNALISM)
- Merged adjacent GIVENNAME+SURNAME into single FULLNAME entities
- Sampled 12 per mode, balanced across easy/medium/hard difficulty
- Fixed position overlap bugs (short entity values matching inside longer strings)

**Phase 2 — Hand-crafted (25 entries):**
- 5 entries per mode targeting contextual reasoning that generic PII data can't test:
  - **HIPAA**: relational identifiers ("the patient's daughter Lisa"), contextual PHI (depression diagnosis linked to patient), medication dosages tied to identified patients
  - **TACTICAL**: keep suspect descriptions (race, height, clothing, vehicle, plate) while redacting victim/witness names — tests the model's ability to distinguish what to keep
  - **JOURNALISM**: keep public officials (Lloyd Austin, Elizabeth Warren) while redacting confidential sources — tests entity role understanding
  - **FIELD_SERVICE**: contextual security info ("key is under the mat", "back door is usually unlocked") — not just passwords but natural-language security risks
  - **FINANCIAL**: keep dollar amounts and institution names while redacting account numbers, SSNs, routing numbers — tests financial entity boundaries

**Final dataset:** 85 entries (17 per mode), stored as `shieldtext_bench.jsonl` in app assets.

### JSONL Schema
```json
{
  "id": "hipaa_ctx_001",
  "mode": "HIPAA",
  "difficulty": "hard",
  "input": "raw text with PII",
  "expected_output": "redacted text with [CATEGORY_N] placeholders",
  "entities": [{"value": "...", "category": "NAME", "placeholder": "[NAME_1]"}],
  "entity_count": 5,
  "source_uid": "handcrafted"
}
```

### Decision: Why 85 entries?
- On-device GPU inference takes ~5-10 seconds per entry
- 85 entries × 2 models × ~7s ≈ ~20 minutes total — acceptable for a hackathon demo
- Enough for statistical validity across 5 modes and 3 difficulty levels
- Added a slider (1-85) so we can run quick 3-entry sanity checks during iteration

---

## 3. Scoring Methodology

Three metrics, weighted into an overall score:

### Entity Recall (weight: 50%)
For each ground-truth PII entity, check if the original value is **absent** from the model's output (i.e., it was redacted in some form). This is intentionally lenient — it doesn't require exact placeholder format, just that the PII was removed.

### Format Compliance (weight: 25%)
Fraction of bracket placeholders in the output that match the `[CATEGORY_N]` pattern (e.g., `[NAME_1]`, `[SSN_2]`). Distinguishes models that use ShieldText's structured format from those that output generic `[REDACTED]`.

### Text Preservation (weight: 25%)
Fraction of non-PII words from the input that are retained in the output. Ensures the model doesn't destroy context while redacting — e.g., "left heel wound not improving" should be preserved, not eaten.

### Overall Score
`entityRecall * 0.5 + formatScore * 0.25 + preservationScore * 0.25`

### Decision: Why not exact string matching?
Exact match against `expected_output` would fail for minor formatting differences (extra spaces, slightly different placeholder numbering). Entity-level scoring is more robust and reflects what actually matters: was the PII removed?

---

## 4. Architecture

### Design Principle: No changes to core app
The benchmark module is self-contained. Only 2 existing files were modified:
- `NavGraph.kt` — added `Routes.BENCHMARK` route (3 lines)
- `LandingScreen.kt` — added a "Benchmark" card below Text/Image cards

### New Files
| File | Purpose |
|---|---|
| `assets/shieldtext_bench.jsonl` | 85-entry benchmark dataset |
| `benchmark/BenchmarkDataset.kt` | JSONL parser, reads from Android assets |
| `benchmark/BenchmarkScorer.kt` | Entity recall, format compliance, preservation scoring |
| `benchmark/BenchmarkResult.kt` | Data classes for per-entry + aggregate results, per-mode/difficulty breakdowns |
| `benchmark/BenchmarkRunner.kt` | Orchestrator: creates own `InferenceEngine`, runs models sequentially |
| `ui/BenchmarkViewModel.kt` | State machine (Ready → Running → Done/Failed), test count control |
| `ui/screens/BenchmarkScreen.kt` | Full UI: model status, slider, progress bar, results tables |

### Engine Isolation
The `BenchmarkRunner` creates its **own `InferenceEngine` instances**, separate from the main app's engine. This ensures:
- The benchmark doesn't interfere with the app's current model state
- Each model gets a clean init/close cycle
- The main app's engine is untouched after benchmark completes

### Backend Cascade
The runner tries GPU first, falls back to CPU if GPU init fails:
```
GPU → CPU (with logging of which backend actually ran)
```
The progress UI shows the actual backend (e.g., "Standard Gemma4 (GPU)") so the user always knows what's running.

---

## 5. Technical Challenges & Solutions

### Challenge 1: applicationId mismatch
**Problem:** The Gemini agent (Android Studio) changed `applicationId` from `com.example.starterhack` to `com.example.shieldtext`, causing `ClassNotFoundException` on launch and model files not being found (they live at the `starterhack` path).
**Solution:** Reverted `applicationId` to `com.example.starterhack`. Models on device must be at `/sdcard/Android/data/com.example.starterhack/files/`.

### Challenge 2: Fine-tuned model missing vision encoder
**Problem:** `LiteRtLmJniException: NOT_FOUND: TF_LITE_VISION_ENCODER not found in the model`. The fine-tuned `.litertlm` was compiled text-only, but the engine config required `visionBackend = Backend.CPU()` and `maxNumImages = 1`.
**Solution:** Added a `textOnly` parameter to `LlmEngine.initialize()`. When `textOnly = true`, sets `visionBackend = null`, `audioBackend = null`, `maxNumImages = null`. The `BenchmarkRunner` uses `textOnly = true` for the fine-tuned model, `textOnly = false` for standard.

### Challenge 3: Chat template incompatibility
**Problem:** `Failed to apply template: unknown method: map has no method named get (in template:238)`. The fine-tuned model was exported with the Gemma 4 chat template from HuggingFace (`google/gemma-4-E2B-it`), which uses `map.get()` — a Jinja feature LiteRT-LM's template parser doesn't support. The standard model (from `litert-community`) was compiled with a compatible template.
**Root cause:** During the LoRA merge + export pipeline, `tokenizer.save_pretrained()` bundled the HuggingFace-native template into the merged model directory, and `litert_torch.generative.export_hf` embedded it into the `.litertlm` file.
**Solution:** Created a re-export notebook (`ReExport_FixedTemplate.ipynb`) that:
1. Copies the merged model from Google Drive
2. Downloads the compatible `tokenizer_config.json` from `google/gemma-3-1b-it` (Gemma 3's template is what LiteRT-LM was built for)
3. Patches the merged model's `tokenizer_config.json` with the compatible `chat_template`
4. Removes the standalone `chat_template.jinja` file
5. Re-exports to `.litertlm`

### Challenge 4: maxNumImages = 0 is invalid
**Problem:** Setting `maxNumImages = 0` for text-only mode caused `max number of images must be positive or null`.
**Solution:** Changed to `maxNumImages = null` (let engine pick default from model).

### Challenge 5: AGP / Kotlin / Gradle version compatibility
**Problem:** Original app used AGP 9.2.0 / Kotlin 2.2.10 / Gradle 9.4.1 which the user's Android Studio (2025.3.4 Narwhal) didn't support. Also, LiteRT-LM 0.11.0-rc1 was compiled with Kotlin metadata 2.3.0, requiring Kotlin ≥ 2.2.0 to read.
**Solution:** Downgraded to AGP 8.8.0 / Kotlin 2.2.0 / Gradle 8.11.1 / compileSdk 35 / targetSdk 35. Kotlin 2.2.0 is the sweet spot — compatible with both the Studio version and LiteRT-LM's metadata.

### Challenge 6: Gradle daemon socket issue in CLI sandbox
**Problem:** Gradle builds from Claude Code's terminal failed with "Could not connect to the Gradle daemon" due to sandbox restrictions on local TCP socket connections.
**Solution:** User builds from their own terminal with:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew installDebug
```

---

## 6. Benchmark Results

**Run configuration:** 85 entries, 5 modes, GPU backend, Samsung S25 Ultra

### Overall Comparison

| Metric | Standard | Fine-tuned | Delta |
|---|---|---|---|
| **Overall Score** | 80.5% | 70.3% | -10.2% |
| **Entity Recall** | 79.3% | 71.7% | -7.6% |
| **Format Score** | 79.8% | 71.7% | -8.1% |
| **Preservation** | 83.7% | 65.9% | -17.8% |

### Performance (GPU)

| Metric | Standard | Fine-tuned | Notes |
|---|---|---|---|
| **Avg Latency** | 5,693ms | 10,626ms | FT is ~1.9x slower |
| **Avg Tok/s** | 12.8 | 9.0 | FT 30% slower throughput |
| **Total Tokens** | 6,255 | 8,109 | FT generates 30% more tokens |
| **Avg Draw** | 101 mA | 301 mA | FT draws 3x more power |

### Per-Mode Entity Recall

| Mode | Standard | Fine-tuned | Winner |
|---|---|---|---|
| **FIELD_SERVICE** | 82.1% | **95.3%** | Fine-tuned (+13.2%) |
| **FINANCIAL** | 83.8% | **85.5%** | Fine-tuned (+1.7%) |
| **HIPAA** | **95.7%** | 39.9% | Standard (+55.8%) |
| **JOURNALISM** | **71.1%** | 61.3% | Standard (+9.8%) |
| **TACTICAL** | 63.7% | **76.8%** | Fine-tuned (+13.1%) |

### By Difficulty

| Difficulty | Standard | Fine-tuned |
|---|---|---|
| **Easy** | 72.2% | 61.1% |
| **Medium** | 75.8% | **82.5%** |
| **Hard** | **87.3%** | 74.7% |

---

## 7. Analysis & Interpretation

### Why the fine-tuned model scores lower overall
The fine-tuned model was trained on the `ai4privacy/pii-masking-400k` dataset with a generic instruction prompt ("Mask all Personally Identifiable Information") and learned to output `[REDACTED]` or `[REDACTED NAME]` style placeholders. ShieldText's benchmark expects the structured `[CATEGORY_N]` format (`[NAME_1]`, `[SSN_2]`). This format mismatch penalizes the FT model on both **Format Score** and **Preservation** metrics.

### Where fine-tuning genuinely helps
- **FIELD_SERVICE** (+13.2%): The FT model better identifies contextual security information (gate codes, WiFi passwords, "key is under the mat")
- **TACTICAL** (+13.1%): Better at distinguishing victim/witness names from suspect descriptions
- **Medium difficulty** (+6.7%): Fine-tuning improved handling of moderately complex multi-entity scenarios

### Where it hurts
- **HIPAA** (-55.8%): The standard model's system prompt engineering for HIPAA is highly effective; the FT model's generic training doesn't capture relational PHI ("the patient's daughter Lisa")
- **Preservation** (-17.8%): The FT model tends to over-redact, removing non-PII context

### Performance gap
The fine-tuned model is 4.7GB vs 2.4GB standard (different quantization granularity from different export pipelines). This directly causes the ~2x latency difference and 3x power draw. Not a fair perf comparison — would need identical export settings.

### What would make the comparison fair
1. Re-train the FT model using ShieldText's exact system prompts and `[CATEGORY_N]` output format
2. Export both models with identical `litert_torch` settings and quantization
3. Use the same chat template (now fixed via `ReExport_FixedTemplate.ipynb`)

---

## 8. Models on Device

| File | Size | Source | Backend | textOnly |
|---|---|---|---|---|
| `gemma4.litertlm` | 2.4GB | litert-community/gemma-4-E2B-it-litert-lm | GPU/CPU | false |
| `gemma4_npu.litertlm` | 2.8GB | litert-community (SM8750 compiled) | NPU | false |
| `gemma4_ft.litertlm` | 4.7GB | Custom fine-tuned + re-exported with fixed template | GPU/CPU | true |

Path: `/sdcard/Android/data/com.example.starterhack/files/`

---

## 9. Fine-Tuning Pipeline Summary

### Training (Colab, NVIDIA RTX PRO 6000)
- Base model: `google/gemma-4-E2B-it` (2B parameters)
- Method: QLoRA via `peft` — LoRA rank 8, alpha 16, dropout 0.05
- Target modules: q/k/v/o_proj.linear + gate/up/down_proj.linear
- Dataset: ai4privacy/pii-masking-400k, 3,000 examples, 1 epoch
- Trainable params: 2,850,816 (0.06% of total)
- Training time: 217 seconds
- Final loss: 4.3064

### Export
- Tool: `litert_torch.generative.export_hf`
- Quantization: `dynamic_wi4_afp32` (INT4 weights, FP32 activations)
- Flags: `-p 256 --cache_length 1024 --externalize_embedder`
- Critical fix: replaced chat template with Gemma 3 compatible version before export

### Known Limitation
The `.litertlm` format does not include the vision encoder section (`TF_LITE_VISION_ENCODER`), so the fine-tuned model cannot be used for image-based redaction. Text-only mode must be enabled in the engine config.

---

## 10. Deliverables

| Deliverable | Location |
|---|---|
| Benchmark dataset (85 entries) | `app/src/main/assets/shieldtext_bench.jsonl` |
| Curation scripts | `benchmark/curate_dataset.py`, `benchmark/contextual_entries.py` |
| Re-export notebook (template fix) | `benchmark/model/ReExport_FixedTemplate.ipynb` |
| Benchmark screenshots | `benchmark/Screenshot_20260501_010320_ShieldText.jpg`, `...010338_ShieldText.jpg` |
| Screen recording | `benchmark/Screen_Recording_20260501_025104_Redacto.mp4` |
| GPU1 standard model logs | `benchmark/gpu1_standard_results.log` |
| App code (branchmarking) | `ShieldText-main/` on `branchmarking` branch (10 commits) |

---

## 11. Commit History (branchmarking branch)

```
747f243 feat: add test count slider to benchmark screen
bd2da58 fix: use null for maxNumImages in textOnly mode, run fine-tuned first
cf14357 fix: update LlmEngine implementations for textOnly parameter
14f7f86 fix: handle fine-tuned models without vision encoder
9e14ac2 fix: restore applicationId to com.example.starterhack
d3b0e7f fix: add GPU init error logging and show active backend in progress
f87142e fix: add GPU → CPU fallback in BenchmarkRunner
fc0ec00 feat: allow benchmark to run with 1 or 2 models
9a6878e fix: downgrade AGP 9.2.0 → 8.9.0 for Android Studio compatibility
7fda6d6 feat: add domain accuracy benchmark for standard vs fine-tuned model
```

---

## 12. Key Takeaways for Judges

1. **On-device benchmarking is feasible and fast.** 85 test cases across 5 domains run in ~20 minutes on the S25 Ultra GPU — no cloud, no API calls, no data leaves the phone.

2. **The standard model (litert-community Gemma 4 E2B) is strong out of the box.** 80.5% overall accuracy with 12.8 tok/s on GPU, 95.7% entity recall on HIPAA — effective for production use without any fine-tuning.

3. **Fine-tuning shows domain-specific gains** in FIELD_SERVICE (+13.2%) and TACTICAL (+13.1%), but requires training data that matches the app's output format. The current FT model was trained with generic redaction labels, not ShieldText's structured `[CATEGORY_N]` format.

4. **Chat template compatibility is a real deployment hazard.** The Gemma 4 HuggingFace template uses Jinja features (`map.get()`) that LiteRT-LM's on-device parser doesn't support. This required a non-obvious fix: swapping the template with an older Gemma 3 version before export. This is undocumented and would block any team trying to fine-tune + deploy.

5. **Model size directly impacts performance and power.** The 4.7GB FT model draws 3x more power (301mA vs 101mA) and runs 1.9x slower than the 2.4GB standard model. For battery-constrained on-device use, quantization strategy matters as much as model quality.

6. **NPU compilation is the next frontier.** The standard model on NPU achieves ~85+ tok/s (vs 12.8 on GPU). Getting the fine-tuned model compiled for the SM8750 NPU would be the true test of whether fine-tuning can deliver both quality AND speed on-device.
