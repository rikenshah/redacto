# Redacto v2 — Engineering Decisions & Architecture

**Created:** 2026-05-01
**Branch:** v2
**Last updated:** 2026-05-01

This document records every significant technical decision made during the v2 development, with rationale. It is the ground truth — if the PRD disagrees with this document, this document is correct.

---

## 1. Category System Architecture

### Decision D1: UI categories and redaction categories are fully decoupled

**UI Categories** (10): Medical, Financial, Tax, Home/Lease, Insurance, ID Documents, Immigration, Auto, Legal, Other. These are user-facing organizational labels. A document can belong to multiple UI categories. Selected by the user at save time. No influence on redaction.

**Redaction Categories** (7): Medical, Financial, Legal, Tactical, Journalism, Field Service, General. These are content-driven — the pipeline's Step 1 (Classify) determines which one applies by analyzing the text, regardless of which UI folder the user files it in.

**Why:** A medical bill is both "Medical" and "Financial" to the user, but the redaction engine should analyze the content and apply the right rules. Coupling them would either under-redact or over-redact.

### Decision D2: Human-friendly category names, not compliance jargon

We say "Medical" not "HIPAA", "Financial" not "GLBA". Multiple compliance frameworks can inform a single category.

**Why:** Most users don't know what HIPAA or GLBA are. The redaction categories are named for the content domain, not the regulation.

---

## 2. Multi-Pass Pipeline Architecture

### Decision D3: 4-step pipeline (Classify → Detect → Redact → Validate)

```
Step 1: CLASSIFY (LLM) → document type + redaction category
Step 2: DETECT  (LLM) → list of PII items found
Step 3: REDACT  (LLM) → text with [CATEGORY_N] placeholders
Step 4: VALIDATE (LLM) → PASS or FAIL with missed items
```

**Why:** A single LLM call asking the model to simultaneously understand, detect, replace, and verify produces inconsistent results. Separating concerns lets each step focus on one task with a purpose-built prompt.

### Decision D4: Step 3 uses LLM, not deterministic string replacement

**Original plan (PRD):** Step 3 was deterministic — sort detections by length, do string.replace().

**What happened:** OCR text has errors ("inquire@blucurrent. mail"). The LLM in Step 2 "corrects" these when reporting detections ("inquire@ucal.com"). The detected text doesn't match the original, so string.replace() fails silently. In benchmarks, 6 of 8 detections were skipped.

**Final decision:** Step 3 uses an LLM call. The LLM receives the original text + detection list and produces the redacted output. It handles OCR noise naturally because it understands semantics.

**Tradeoff:** One extra LLM call (~1-5 seconds). Acceptable per Decision D6.

**Fallback:** If the Step 3 LLM call fails, we fall back to deterministic replacement.

### Decision D5: Every LLM call gets a fresh conversation

Steps 1, 2, 3, and 4 each create and close their own conversation object.

**Why:** Prevents context pollution. Step 4 (validation) must be an independent auditor — if it shares context with Step 2 (detection), it's biased by its own prior work.

### Decision D6: Accuracy over latency — no hard latency budget

Multiple LLM calls are acceptable. Inference runs on local NPU at zero marginal cost. Users prefer thorough redaction over fast results.

**Measured impact:** 3-step pipeline (no validation) takes ~2.8s on NPU vs ~1.5s single-pass. Acceptable.

### Decision D7: Max 3 validation rounds (1 initial + 2 retries)

If Step 4 reports FAIL, Steps 3+4 re-run with the missed items added to the detection list. After 3 total rounds, output whatever we have.

**Why:** Diminishing returns. In benchmarks, most entries pass in round 1. Those that fail after 3 rounds have systemic prompt issues, not fixable by retrying.

### Decision D8: Line-based output format, not JSON

LLM responses use `CATEGORY: text` format (one per line), not JSON arrays.

**Why:** More token-efficient for a small on-device model. Simpler to produce reliably. Gemma 4 E2B sometimes struggles with JSON bracket matching.

---

## 3. Image Redaction Pipeline

### Decision D9: Indexed-element approach for image redaction

**Old approach:** OCR → text → LLM redacts text → string-match redacted words back to OCR bounding boxes → draw black rectangles. This broke constantly because the LLM rewrites text.

**New approach:** OCR produces indexed elements: `[0] Patient [1] Jane [2] Smith`. LLM receives indexed elements, returns `1:NAME 2:NAME`. We map indices directly to bounding boxes. Zero string matching.

**Why:** The index→bounding box mapping is lossless. No text matching means no OCR error sensitivity. HUD field count and visual box count are always consistent.

### Decision D10: Chunked processing for long documents

OCR elements are chunked at 150 elements per chunk. Each chunk goes through the detection LLM separately.

**Why:** Context window limit (~4000 tokens). A dense document can have 400+ OCR elements.

### Decision D11: Black redaction boxes, not color-coded

All redaction boxes are plain black.

**Why:** User requested simplicity. Color coding was not asked for.

---

## 4. Prompts Architecture

### Decision D12: 7 category-specific prompt sets

Each redaction category has dedicated Detect and Validate prompts. Classify and Redact prompts are universal (shared across categories).

| Category | Detect focus | Key preserve rules |
|---|---|---|
| Medical | 16 PHI types | Diagnoses, medications, vitals, body locations |
| Financial | Account/routing/card/SSN/TaxID | Dollar amounts, institution names, toll-free numbers |
| Legal | Buyer/seller/tenant names | Property specs, legal terms, dollar amounts |
| Tactical | Victim/witness/minor protection | Suspect descriptions, officer names, crime scene |
| Journalism | Source identity protection | Public officials, reporter names |
| Field Service | Customer PII + security credentials | Equipment details, fault codes |
| General | Broad PII detection (fallback) | Minimal preserve rules |

**Why:** Different document types have radically different rules about what IS and ISN'T PII. A clinical note must preserve "Type 2 diabetes" while redacting "Jane Smith". A police report must preserve suspect descriptions while redacting victim names.

### Decision D13: Prompts embedded as Kotlin constants

The `PipelinePrompts` object holds all prompts as string literals. The SKILL.md files in `prompts/` are design references.

**Why:** Runtime file reading adds complexity. Constants are faster and always available.

---

## 5. Metrics & Benchmarking

### Decision D14: Only show metrics we can measure reliably

**Included (high confidence):**
- Wall-clock latency: `System.currentTimeMillis()` around the full call
- Time to first token (TTFT): timestamp on first `onMessage` callback minus start time
- Decode tok/s: `(tokenCount - 1) * 1000 / (lastTokenTime - firstTokenTime)`
- Output token count: increment counter per `onMessage` callback
- Peak RSS memory: read `/proc/self/status` VmRSS line

**Removed (unreliable):**
- Battery current draw: `BatteryManager.BATTERY_PROPERTY_CURRENT_NOW` is a system-wide instantaneous reading, not per-process. Two samples (before/after) is not meaningful.
- Energy in joules: derived from unreliable current draw × assumed 3.7V. Wrong on multiple levels.
- GPU/NPU load: no public API exposed by Qualcomm.

**Why:** We don't fake numbers. If we can't measure it properly, we don't show it.

### Decision D15: Text-based benchmark dataset for performance measurement

The `shieldtext_bench.jsonl` dataset (85 entries, 5 modes, 3 difficulty levels) is used for performance measurement. Pure text input — no OCR overhead.

**Why:** OCR adds variable latency that obscures inference performance differences. The benchmark should measure the pipeline, not ML Kit.

### Decision D16: Benchmark runs Steps 1-3 only (no validation)

The performance benchmark runs Classify → Detect → Redact. No Step 4 validation.

**Why:** This is a performance test, not a quality test. We want to measure inference speed, not redaction accuracy. Validation adds variable rounds that make latency comparisons noisy.

### Decision D17: ADB-triggered benchmarks via BroadcastReceiver

Benchmarks are triggered via `adb shell am broadcast` with parameters for type, count, and backend. The receiver invokes a callback on the running app's ViewModel, which shares the already-initialized engine.

**Why:** Avoids OOM (creating a second engine doubles 2.4GB memory usage). Allows automated testing without touching the UI. The app must be open (callback registration happens in Compose).

**Usage:**
```bash
# Text benchmark, 5 entries, NPU:
adb shell am broadcast -n com.example.redacto/com.example.starterhack.benchmark.BenchmarkReceiver \
  -a com.example.redacto.BENCHMARK --es type text --ei count 5

# Text benchmark, GPU:
adb shell am broadcast ... --es type text --ei count 5 --es backend GPU

# Image benchmark (old, uses RedactionPipeline):
adb shell am broadcast ... --es category "medical/patient-records" --ei count 3
```

**Monitor:** `adb logcat -s RedactoBenchmark:I`

---

## 6. Measured Performance (as of 2026-05-01)

### GPU vs NPU — hipaa_001 (229 chars, easy)

| Metric | GPU | NPU | NPU Speedup |
|---|---|---|---|
| Total latency | 5,646ms | 2,781ms | 2.0x |
| Step 1 latency | 955ms | 757ms | 1.3x |
| Step 2 latency | 2,258ms | 694ms | 3.3x |
| Step 3 latency | 2,413ms | 1,309ms | 1.8x |
| TTFT (Step 1) | 710ms | 468ms | 1.5x |
| TTFT (Step 2) | 424ms | 159ms | 2.7x |
| TTFT (Step 3) | 518ms | 262ms | 2.0x |
| Decode tok/s (Step 2) | 36.5 | 44.9 | 1.2x |
| Decode tok/s (Step 3) | 24.3 | 44.9 | 1.8x |
| Total tokens | 122 | 83 | — |
| Peak RSS | 1,134MB | 1,684MB | GPU uses less RAM |

### NPU per-step breakdown (patient-record 1, via image pipeline)

| LLM Call | Latency | TTFT | Tokens | Decode tok/s |
|---|---|---|---|---|
| Step 1 (Classify) | 289ms | 82ms | 9 | 43.7 |
| Step 2 (Detect) | 2,626ms | 125ms | 102 | 40.8 |
| Step 3 (Redact) | 4,673ms | 108ms | 189 | 41.4 |
| Step 4 (Validate) | 381ms | 90ms | 12 | 41.2 |

---

## 7. UI/UX Decisions

### Decision D18: Pipeline progress indicator on loading screen

Users see which step the pipeline is on: "Step 2/4: Detecting Medical identifiers…" with step dots (teal = done, active = current, gray = pending).

**Why:** The multi-pass pipeline takes 3-8 seconds. Without progress, users see a blank spinner and think the app is frozen.

### Decision D19: Text snippets save separately from documents

For text-mode redaction, only "Save as text snippet" is offered (visible in Text tab). For image-mode, only "Save version" is offered (visible in Documents tab).

**Why:** Users were confused when text saved as a document and appeared in the wrong tab.

### Decision D20: FAB instead of full-width bottom bar

A small floating action button replaces the full-width bottom bar with Home/Manage buttons.

**Why:** Home and Manage buttons were non-functional. The full-width button overlapped system navigation. FAB is standard Android pattern.

### Decision D21: Category tiles grayed out when empty

Tiles with 0 documents render at 40% opacity. No numbers shown on tiles.

**Why:** Numbers caused inconsistent tile sizes. Grayed-out state visually communicates "nothing here yet" without clutter.

---

## 8. Data & Storage

### Decision D22: Image versions stored as file paths in Room

`DocumentVersion.redactedContent` stores either:
- Plain text with `[CATEGORY_N]` placeholders (for text documents)
- Absolute file path to a PNG on disk (for image documents, starts with `/`)

Display logic checks `startsWith("/")` to decide whether to load as image or render as text.

**Why:** Storing multi-megabyte bitmaps as BLOBs in Room would be slow and memory-intensive. File paths are lightweight.

### Decision D23: FileProvider for sharing images

Images are shared via `FileProvider` URIs with `clipData` set on the intent, so the share sheet shows a thumbnail preview.

**Why:** Direct file paths can't be shared across apps on Android 10+. FileProvider generates content URIs with temporary read permission.

---

## 9. Dropped Features

### Decision D24: PDF upload removed

PDF upload (PdfRenderer + ML Kit OCR per page) was implemented but removed before demo.

**Why:** Not tested end-to-end. Risk of demo failure outweighed the feature value.

### Decision D25: RegexFallback safety net not used

The `RegexFallback` module (SSN, email, phone, MRN patterns) exists in the codebase but is not wired into the pipeline.

**Why:** User explicitly requested LLM-only redaction. The regex safety net was from the original ShieldText design. The multi-pass pipeline with validation provides the safety layer instead.

### Decision D26: NPU uses null SamplerConfig

NPU backend passes `samplerConfig = null` to the conversation. GPU uses `topK=64, topP=0.95, temperature=1.0`.

**Why:** Constrained decoding is unsupported on NPU (errors with "not supported, error 12"). Setting SamplerConfig to null lets the NPU use its default sampling.

**Consequence:** NPU generates more verbose output (~2x more tokens per call), which can equalize or exceed GPU total latency despite higher per-token speed. This is a known tradeoff.

### Decision D27: Redacto logo uses italic teal "o", not boxed "o"

Original design: "Redact" + white "o" inside a teal rounded rectangle. Changed to: "Redact" in navy bold + "o" in teal italic bold.

**Why:** The boxed "o" looked like the number zero.

---

## 10. Known Limitations

1. **NPU re-init within same process fails.** After switching away from NPU, you can't switch back without restarting the app. Root cause: QNN runtime holds DSP state that doesn't clean up on `Engine.close()`.

2. **Step 2 detection text doesn't match OCR text.** The LLM "corrects" OCR errors in its detection output. This is why Step 3 uses LLM (not string replacement) — the LLM can handle the mismatch.

3. **Over-redaction on dense documents.** Some medical documents get 300+ detections — the LLM flags nearly every OCR element as PII. Prompt refinement needed.

4. **Validation false positives.** The validator sometimes flags `[NAME_1]` placeholders as leaks. Parser filter catches this, but the LLM wastes a round.

5. **GPU inference is ~2x slower than NPU** but uses ~550MB less RSS. For the hackathon demo, NPU is preferred.
