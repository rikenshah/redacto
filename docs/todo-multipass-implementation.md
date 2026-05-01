# Multi-Pass Redaction Pipeline — Implementation TODO

**Created:** 2026-05-01
**Updated:** 2026-05-01
**Reference:** `docs/prd-multipass-redaction-pipeline.md`
**Decisions log:** `docs/engineering-decisions.md`

**STATUS SUMMARY:**
- S0 Metrics: DONE
- S1 Logging: DONE
- S2 Test data: DONE
- Phase 1 Core Framework: DONE
- Phase 2 ViewModel Integration: DONE
- Phase 3 Medical Prompts: DONE (quality iteration ongoing)
- Phase 4 Testing: PARTIAL (unit tests done, on-device regression pending)
- Phase 5 Financial: DONE (prompts written, benchmarked)
- Phase 6 Benchmarking: PARTIAL (text runner done, UI not built)
- Phase 7 Confidence Score: NOT STARTED

**KEY DEVIATION FROM PRD:** Step 3 changed from deterministic to LLM.
See `docs/engineering-decisions.md` Decision D4 for rationale.

---

## Pre-Implementation Setup

### S0. Fix Metrics Collection (before anything else)
- [ ] S0.1 Update `InferenceMetrics.kt`: remove `currentDrawMa` and `energyJoules` (unreliable), add `timeToFirstTokenMs: Long`, `decodeTokensPerSec: Float`, `peakNativeHeapMb: Float`, `peakRssMb: Float`
- [ ] S0.2 Update `InferenceEngine.redact()`: capture `firstTokenTimeMs` on first `onMessage` callback, capture `lastTokenTimeMs` on last `onMessage` before `onDone`
- [ ] S0.3 Compute TTFT = `firstTokenTimeMs - startTime`
- [ ] S0.4 Compute decode tok/s = `(tokenCount - 1) * 1000 / (lastTokenTimeMs - firstTokenTimeMs)` (guard against div-by-zero when tokenCount <= 1)
- [ ] S0.5 Capture memory: `Debug.getNativeHeapAllocatedSize() / (1024*1024)` before and after inference, record peak
- [ ] S0.6 Capture RSS: read `/proc/self/status` VmRSS line, parse to MB
- [ ] S0.7 Remove `sampleCurrentDrawMa()` method and all battery-related code
- [ ] S0.8 Update `RedactionHudBar.kt` to show TTFT and decode tok/s instead of old metrics
- [ ] S0.9 Update `RedactionUiState.Success` to carry new metric fields
- [ ] S0.10 Verify: every metric shown on HUD has a clear, documented, reliable source — no estimates, no hacks
- [ ] S0.11 Write `InferenceMetricsTest` — verify TTFT < total latency, decode tok/s > 0 when tokens > 1, memory > 0

### S1. Verbose Logging Infrastructure
- [ ] S1.1 Create `PipelineLog` utility object with tagged logging methods (`classify`, `detect`, `redact`, `validate`, `retry`, `error`)
- [ ] S1.2 Every log line must include: step name, round number, elapsed time since pipeline start, and a human-readable message
- [ ] S1.3 Log the full prompt sent to LLM (truncated to first 500 chars in logcat, full in debug)
- [ ] S1.4 Log the full raw LLM response before parsing
- [ ] S1.5 Log parsed results after each parser runs (detection count, categories found, pass/fail)
- [ ] S1.6 Log conversation open/close events with memory info
- [ ] S1.7 Log retry decisions: why validation failed, what missed items were found, which round we're on
- [ ] S1.8 Log final pipeline result: total rounds, total LLM calls, total time, detection count, pass/fail
- [ ] S1.9 Use tag `RedactoPipeline` for all pipeline logs, `RedactoPipeline.Step1` / `Step2` / `Step3` / `Step4` for per-step filtering

### S2. Push Test Data to Device
- [ ] S2.1 Create a shared folder on device at `/sdcard/RedactoTestData/`
- [ ] S2.2 Push `image-data/medical/patient-records/` → `/sdcard/RedactoTestData/medical/patient-records/`
- [ ] S2.3 Push `image-data/medical/lab-results/` → `/sdcard/RedactoTestData/medical/lab-results/`
- [ ] S2.4 Push `image-data/medical/handwritten prescriptions/` → `/sdcard/RedactoTestData/medical/prescriptions/`
- [ ] S2.5 Push `image-data/financial/bank-statement/` → `/sdcard/RedactoTestData/financial/bank-statements/`
- [ ] S2.6 Push `image-data/financial/W-2/` → `/sdcard/RedactoTestData/financial/w2/`
- [ ] S2.7 Push `image-data/legal/` → `/sdcard/RedactoTestData/legal/`
- [ ] S2.8 Verify all files on device with `adb shell ls -R /sdcard/RedactoTestData/`
- [ ] S2.9 This folder persists across app uninstalls (it's outside app scoped storage)

---

## Phase 1: Core Pipeline Framework

### 1.1 Data Models
- [ ] 1.1.1 Create `engine/pipeline/Detection.kt` — data class: `category: String`, `originalText: String`, `placeholder: String`
- [ ] 1.1.2 Create `engine/pipeline/PipelineResult.kt` — data class: `redactedText`, `documentType`, `redactionCategory`, `detections: List<Detection>`, `validationPassed: Boolean`, `roundsUsed: Int`, `totalLlmCalls: Int`, `totalLatencyMs: Long`
- [ ] 1.1.3 Create `engine/pipeline/ClassificationResult.kt` — data class: `documentType: String`, `category: String`
- [ ] 1.1.4 Create `engine/pipeline/ValidationResult.kt` — sealed: `Pass` or `Fail(missedItems: List<Detection>)`

### 1.2 Parsers
- [ ] 1.2.1 Create `engine/pipeline/ClassificationParser.kt` — parses `DOCUMENT_TYPE: ...` and `CATEGORY: ...` lines from LLM output
- [ ] 1.2.2 Handle edge cases: extra whitespace, missing lines, explanatory text mixed in, unrecognized category names
- [ ] 1.2.3 Log: raw input, parsed result, any fallback triggered
- [ ] 1.2.4 Create `engine/pipeline/DetectionParser.kt` — parses `CATEGORY: exact text` lines from LLM output
- [ ] 1.2.5 Handle edge cases: duplicate detections, empty lines, lines without colon, explanatory paragraphs mixed in
- [ ] 1.2.6 Deduplicate detections (same category + same text = one detection)
- [ ] 1.2.7 Log: raw input, number of lines parsed, number of valid detections, any skipped lines
- [ ] 1.2.8 Create `engine/pipeline/ValidationParser.kt` — parses `RESULT: PASS` or `RESULT: FAIL` + missed items
- [ ] 1.2.9 Handle edge cases: response doesn't contain RESULT line, mixed PASS/FAIL signals, validation output includes items already redacted
- [ ] 1.2.10 Log: raw input, result status, number of missed items if FAIL

### 1.3 Deterministic Redactor
- [ ] 1.3.1 Create `engine/pipeline/DeterministicRedactor.kt` — takes original text + detection list, returns redacted text
- [ ] 1.3.2 Sort detections by text length descending (longest first) to avoid substring conflicts
- [ ] 1.3.3 Track placeholder counters per category: `NAME_1`, `NAME_2`, etc.
- [ ] 1.3.4 Handle: detection text not found in original (skip silently, log warning)
- [ ] 1.3.5 Handle: detection text appears multiple times (replace all instances with same placeholder)
- [ ] 1.3.6 Handle: overlapping detections (longer match takes priority)
- [ ] 1.3.7 Return the detection list enriched with `placeholder` field filled in
- [ ] 1.3.8 Log: number of replacements made, number of detections skipped, final text length vs original

### 1.4 Prompt Templates
- [ ] 1.4.1 Create `engine/pipeline/PipelinePrompts.kt` — object holding all prompt templates
- [ ] 1.4.2 Write Step 1 classification prompt (universal, category-agnostic) — per PRD section 3.2
- [ ] 1.4.3 Write Step 2 medical detection prompt — load H1-H18 identifiers to redact + H19-H28 to preserve from SKILL.md files
- [ ] 1.4.4 Write Step 4 medical validation prompt — per PRD section 3.5
- [ ] 1.4.5 Add `fun detectPromptFor(category: String): String` method that returns category-specific Step 2 prompt
- [ ] 1.4.6 Add `fun validatePromptFor(category: String): String` method that returns category-specific Step 4 prompt
- [ ] 1.4.7 Log: which prompt was selected, prompt length in chars

### 1.5 Pipeline Orchestrator
- [ ] 1.5.1 Create `engine/pipeline/RedactionPipeline.kt` — main class orchestrating all 4 steps
- [ ] 1.5.2 Constructor takes `LlmEngine` and `OcrProcessor` references
- [ ] 1.5.3 Main method: `suspend fun process(text: String): PipelineResult`
- [ ] 1.5.4 Implement Step 1: create new conversation, send classification prompt, parse response, close conversation
- [ ] 1.5.5 Log after Step 1: document type, category, latency
- [ ] 1.5.6 Implement Step 2: create new conversation, load category-specific prompt, send detection prompt with input text, parse response, close conversation
- [ ] 1.5.7 Log after Step 2: number of detections, categories found, latency
- [ ] 1.5.8 Implement Step 3: call `DeterministicRedactor.redact()` with detections
- [ ] 1.5.9 Log after Step 3: replacements made, redacted text preview (first 200 chars)
- [ ] 1.5.10 Implement Step 4: create new conversation, send validation prompt with redacted text, parse response, close conversation
- [ ] 1.5.11 Log after Step 4: PASS or FAIL, missed items if any, latency
- [ ] 1.5.12 Implement retry loop: if FAIL, merge missed items into detection list, re-run Steps 3+4, max 3 total rounds
- [ ] 1.5.13 Log each retry: round number, new detections added, cumulative detection count
- [ ] 1.5.14 Build and return `PipelineResult` with all metadata
- [ ] 1.5.15 Log final: total rounds, total LLM calls, total pipeline latency, final detection count, pass/fail

### 1.6 Fallback Handling
- [ ] 1.6.1 If any LLM call in the pipeline throws, catch and log the error
- [ ] 1.6.2 On Step 1 failure: fall back to current `SystemPrompts.build()` single-pass mode
- [ ] 1.6.3 On Step 2 failure: fall back to single-pass mode
- [ ] 1.6.4 On Step 4 failure: treat as PASS (Steps 2+3 already produced output)
- [ ] 1.6.5 Log every fallback: which step failed, error message, what mode we fell back to
- [ ] 1.6.6 Set `PipelineResult.validationPassed = false` on fallback so confidence score can reflect it later

---

## Phase 2: ViewModel Integration

### 2.1 Wire Pipeline into RedactionViewModel
- [ ] 2.1.1 Add `RedactionPipeline` instance to `RedactionViewModel` constructor
- [ ] 2.1.2 Update `redactText()` to call `pipeline.process(text)` instead of `engine.redact(text, mode)`
- [ ] 2.1.3 Map `PipelineResult` to `RedactionUiState.Success` fields
- [ ] 2.1.4 Store the `PipelineResult.detections` list — this replaces `PlaceholderMapper.buildMap()` heuristic
- [ ] 2.1.5 Update `redactImage()` to pass OCR'd text through the pipeline, then apply redaction boxes to bitmap using detection positions
- [ ] 2.1.6 Log: which code path was taken (pipeline vs legacy), final state transition

### 2.2 Update RedactionUiState
- [ ] 2.2.1 Add `pipelineResult: PipelineResult?` field to `RedactionUiState.Success`
- [ ] 2.2.2 Add `documentType: String` field
- [ ] 2.2.3 Add `redactionCategory: String` field
- [ ] 2.2.4 Keep existing flat fields (`latencyMs`, `tokenCount`, etc.) for backward compat with HUD
- [ ] 2.2.5 Populate `fieldsRedacted` from `pipelineResult.detections.size` instead of regex count

### 2.3 Update HUD Display
- [ ] 2.3.1 Show total LLM calls in HUD (e.g., "3 passes" or "3x" next to latency)
- [ ] 2.3.2 Show total pipeline latency (sum of all steps), not just last LLM call
- [ ] 2.3.3 Show validation status: checkmark if PASS, warning if FAIL after max rounds
- [ ] 2.3.4 Log: HUD values rendered

### 2.4 Update Interactive Heatmap
- [ ] 2.4.1 Pass `PipelineResult.detections` directly to `InteractiveRedactionHeatmap` as the placeholder map
- [ ] 2.4.2 Remove `PlaceholderMapper.buildMap()` call — the pipeline now provides exact mappings
- [ ] 2.4.3 Verify tap-to-un-redact still works with pipeline-sourced detections

---

## Phase 3: Medical Prompt Refinement

### 3.1 Step 2 Prompt: Medical Detection
- [ ] 3.1.1 Review `prompts/medical-redaction/2-identifiers/H1-H18-hipaa-identifiers/SKILL.md` for all 18 REDACT identifiers
- [ ] 3.1.2 Review `prompts/medical-redaction/2-identifiers/H19-H28-medical-context/SKILL.md` for all 10 PRESERVE identifiers
- [ ] 3.1.3 Write the detection prompt incorporating: all 18 redact categories with examples, all 10 preserve categories with examples, special rules (age <90 OK, year OK, ZIP first 3 OK, family names are PHI)
- [ ] 3.1.4 Add 2-3 worked examples (input text → expected detection output) to the prompt
- [ ] 3.1.5 Test prompt against `image-data/medical/patient-records/patient-record 1.png` (OCR → detect)
- [ ] 3.1.6 Test prompt against `image-data/medical/lab-results/lab 1.png`
- [ ] 3.1.7 Test prompt against `image-data/medical/handwritten prescriptions/pres 1.jpg`
- [ ] 3.1.8 Iterate on prompt wording based on detection accuracy
- [ ] 3.1.9 Verify preservation: diagnoses, medications, vitals, body locations are NOT detected

### 3.2 Step 4 Prompt: Medical Validation
- [ ] 3.2.1 Review `prompts/medical-redaction/3-compliance/HIPAA/VALIDATION.md` for validation rules
- [ ] 3.2.2 Write the validation prompt: scan for any of the 18 identifier types in redacted text
- [ ] 3.2.3 Include examples of what leaked PII looks like vs. what correctly redacted text looks like
- [ ] 3.2.4 Test: feed a known-good redacted text → should return PASS
- [ ] 3.2.5 Test: feed a redacted text with one intentionally leaked name → should return FAIL + the name
- [ ] 3.2.6 Test: feed a redacted text with preserved clinical terms → should NOT flag them as leaks

### 3.3 Document-Type-Specific Sub-Prompts (stretch)
- [ ] 3.3.1 Review `prompts/medical-redaction/1-documents/personal/patient-records/SKILL.md`
- [ ] 3.3.2 Review `prompts/medical-redaction/1-documents/personal/clinical-notes/SKILL.md`
- [ ] 3.3.3 Review `prompts/medical-redaction/1-documents/personal/lab-results/SKILL.md`
- [ ] 3.3.4 Review `prompts/medical-redaction/1-documents/personal/prescriptions/SKILL.md`
- [ ] 3.3.5 Optionally add document-type-specific hints to Step 2 prompt (e.g., "This is a lab result — pay attention to accession numbers and specimen IDs")

---

## Phase 4: Testing & Validation

### 4.1 Unit Tests
- [ ] 4.1.1 `ClassificationParserTest` — well-formed input, missing lines, extra text, unrecognized category
- [ ] 4.1.2 `DetectionParserTest` — normal lines, duplicates, empty input, mixed explanatory text, no-colon lines
- [ ] 4.1.3 `DeterministicRedactorTest` — basic replacement, substring conflict, duplicate text, missing text, empty detections, overlapping detections
- [ ] 4.1.4 `ValidationParserTest` — PASS response, FAIL with items, no RESULT line, ambiguous response
- [ ] 4.1.5 `PipelinePromptsTest` — correct prompt returned per category, unknown category returns general prompt

### 4.2 On-Device Integration Tests
- [ ] 4.2.1 Run full pipeline on 5 patient records from `/sdcard/RedactoTestData/medical/patient-records/`
- [ ] 4.2.2 Run full pipeline on 5 lab results from `/sdcard/RedactoTestData/medical/lab-results/`
- [ ] 4.2.3 Run full pipeline on 5 prescriptions from `/sdcard/RedactoTestData/medical/prescriptions/`
- [ ] 4.2.4 For each: capture logcat, verify detections match expected PII, verify preserved items were not redacted
- [ ] 4.2.5 Document results: detection count, round count, latency, any false positives/negatives

### 4.3 Regression Check
- [ ] 4.3.1 Run the same 15 documents through the OLD single-pass pipeline
- [ ] 4.3.2 Compare: detection count old vs new, any PII missed by old that new caught, any false redactions
- [ ] 4.3.3 Document the comparison in a test results log

---

## Phase 5: Financial Category

### 5.1 Financial Detection Prompt
- [ ] 5.1.1 Review `prompts/financial-redaction/2-identifiers/personal/F1-F9-identity/SKILL.md`
- [ ] 5.1.2 Review `prompts/financial-redaction/3-compliance/GLBA/IDENTIFIERS.md` for full identifier list
- [ ] 5.1.3 Write Step 2 financial detection prompt with F1-F9 identifiers + preservation rules (keep institution names, 1-800 numbers, dollar amounts)
- [ ] 5.1.4 Write Step 4 financial validation prompt
- [ ] 5.1.5 Test on 5 bank statements from `/sdcard/RedactoTestData/financial/bank-statements/`
- [ ] 5.1.6 Test on 5 W-2 forms from `/sdcard/RedactoTestData/financial/w2/`
- [ ] 5.1.7 Iterate on prompt accuracy

### 5.2 Financial Validation Rules
- [ ] 5.2.1 Review `prompts/financial-redaction/3-compliance/GLBA/VALIDATION.md` for tiered thresholds
- [ ] 5.2.2 Implement tiered validation: CRITICAL 100%, HIGH >=95%, MEDIUM >=80% (different from medical binary)
- [ ] 5.2.3 Map each financial identifier to its priority tier in the validation logic

---

## Phase 6: Benchmarking Feature

### 6.1 Benchmark Data Models
- [ ] 6.1.1 Create `engine/benchmark/BenchmarkConfig.kt` — data class: `category: String` (e.g., "patient-records"), `sampleCount: Int`, `testDataPath: String`
- [ ] 6.1.2 Create `engine/benchmark/BenchmarkSampleResult.kt` — data class: `fileName: String`, `ocrLatencyMs: Long`, `pipelineResult: PipelineResult`, `metrics: InferenceMetrics` (per-step metrics for each LLM call), `totalLatencyMs: Long`
- [ ] 6.1.3 Create `engine/benchmark/BenchmarkReport.kt` — data class: `config: BenchmarkConfig`, `results: List<BenchmarkSampleResult>`, `summary: BenchmarkSummary`
- [ ] 6.1.4 Create `engine/benchmark/BenchmarkSummary.kt` — data class: `avgTotalLatencyMs`, `avgTtftMs`, `avgDecodeTokensPerSec`, `avgTokenCount`, `avgDetectionCount`, `avgRoundsUsed`, `validationPassRate`, `avgPeakMemoryMb`, `samplesProcessed`, `samplesFailed`

### 6.2 Benchmark Runner
- [ ] 6.2.1 Create `engine/benchmark/BenchmarkRunner.kt` — class that runs the pipeline on a batch of images
- [ ] 6.2.2 Constructor takes `RedactionPipeline`, `OcrProcessor`, `Context`
- [ ] 6.2.3 Method: `suspend fun run(config: BenchmarkConfig): BenchmarkReport`
- [ ] 6.2.4 List files from `/sdcard/RedactoTestData/{category}/` using `DocumentFile` or `File` APIs
- [ ] 6.2.5 Take first N files based on `config.sampleCount`
- [ ] 6.2.6 For each file: load bitmap → OCR → pipeline → collect metrics
- [ ] 6.2.7 Log per-sample: file name, OCR time, pipeline time, detection count, pass/fail
- [ ] 6.2.8 Compute summary statistics (averages, pass rate)
- [ ] 6.2.9 Log final summary
- [ ] 6.2.10 Handle errors per sample (skip failed sample, log error, continue to next)

### 6.3 Benchmark UI
- [ ] 6.3.1 Create `ui/screens/BenchmarkScreen.kt` — accessible from HomeScreen (maybe via the overflow/settings)
- [ ] 6.3.2 Category selector: dropdown with options matching test data folders (patient-records, lab-results, prescriptions, bank-statements, w2, legal)
- [ ] 6.3.3 Sample count slider: 1 to 20 (default 3)
- [ ] 6.3.4 "Run Benchmark" button — starts the batch run
- [ ] 6.3.5 Progress indicator: "Processing 2/5..." with current file name
- [ ] 6.3.6 Results display: table with per-sample rows (file, latency, TTFT, tok/s, detections, pass/fail)
- [ ] 6.3.7 Summary card at top: averages for all metrics
- [ ] 6.3.8 Per-sample expandable detail: show full detection list, redacted text preview
- [ ] 6.3.9 "Export results" button: copy summary to clipboard as formatted text

### 6.4 Benchmark ViewModel
- [ ] 6.4.1 Create `ui/viewmodel/BenchmarkViewModel.kt` — manages benchmark state
- [ ] 6.4.2 Expose: `benchmarkState: StateFlow<BenchmarkState>` (Idle, Running(progress), Complete(report), Error)
- [ ] 6.4.3 Method: `fun startBenchmark(config: BenchmarkConfig)`
- [ ] 6.4.4 Runs on `Dispatchers.Default`, updates progress on each sample completion
- [ ] 6.4.5 Log: benchmark start, each sample, benchmark complete

### 6.5 Navigation
- [ ] 6.5.1 Add `BENCHMARK` route to `Routes`
- [ ] 6.5.2 Add composable to `NavGraph`
- [ ] 6.5.3 Add entry point from HomeScreen (settings icon or long-press on backend chip)

---

## Phase 7: Confidence Score (future)

### 6.1 Scoring Logic
- [ ] 6.1.1 Define score calculation: validation pass (green), pass after retries (yellow), fail after 3 rounds (red)
- [ ] 6.1.2 Store score in `PipelineResult`
- [ ] 6.1.3 Display badge on ResultScreen HUD
- [ ] 6.1.4 Display badge on DocumentDetailScreen per version
