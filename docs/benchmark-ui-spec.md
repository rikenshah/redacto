# Benchmark UI Screen — Specification

**Created:** 2026-05-01
**Status:** Spec only — not yet implemented
**Reference implementation:** ShieldText-main `BenchmarkScreen.kt`

---

## Overview

An in-app benchmark screen that lets users run the text-based benchmark dataset on GPU and/or NPU, view per-step performance metrics, and compare backends side-by-side.

The ADB-triggered benchmark already works. This spec adds a visual frontend for it.

---

## State Machine

```
Ready → Running → Done
  ↑                 │
  └── reset() ──────┘
  
Ready → Running → Failed
  ↑                  │
  └── reset() ───────┘
```

```kotlin
sealed interface BenchmarkUiState {
    data object Ready : BenchmarkUiState
    data class Running(val progress: BenchmarkProgress) : BenchmarkUiState
    data class Done(val results: BenchmarkResults) : BenchmarkUiState
    data class Failed(val message: String) : BenchmarkUiState
}

data class BenchmarkProgress(
    val backend: String,        // "GPU" or "NPU"
    val currentEntry: Int,
    val totalEntries: Int,
    val currentId: String,      // e.g. "hipaa_001"
)
```

---

## Screen: Ready

### Layout

```
┌──────────────────────────────────┐
│ ← Benchmark                      │
├──────────────────────────────────┤
│                                  │
│  Performance Benchmark           │
│  Runs redaction pipeline on text │
│  dataset. Measures per-step      │
│  latency, TTFT, and tok/s.      │
│                                  │
│  Test cases: 3 / 85             │
│  ═══════●═══════════════════     │
│                                  │
│  Difficulty: [All ▼]             │
│                                  │
│  ┌──────────┐  ┌──────────┐     │
│  │  Run GPU  │  │  Run NPU │     │
│  └──────────┘  └──────────┘     │
│                                  │
│  ┌────────────────────────┐     │
│  │  Run Both & Compare    │     │
│  └────────────────────────┘     │
│                                  │
└──────────────────────────────────┘
```

### Controls

1. **Test count slider** — Range 1 to 85 (or filtered count). Default: 3. Shows "N / total".
2. **Difficulty dropdown** — Options: All, Easy, Medium, Hard. Filters available entries.
3. **Run GPU button** — Runs selected entries on GPU backend.
4. **Run NPU button** — Runs selected entries on NPU backend. If NPU init fails, shows error.
5. **Run Both & Compare** — Runs GPU first, then switches to NPU, shows comparison.

### Backend switching

- For "Run GPU": if currently on NPU, switch via `selectModelVariant(GPU)`, wait for engine ready.
- For "Run NPU": force requires NPU model present. If NPU init fails (falls back to GPU), show warning: "NPU unavailable — fell back to GPU".
- For "Run Both": run GPU entries, store results, switch to NPU (may need app restart hint if in-process NPU re-init fails), run NPU entries, show comparison.

---

## Screen: Running

```
┌──────────────────────────────────┐
│ ← Benchmark                      │
├──────────────────────────────────┤
│                                  │
│  Running on GPU                  │
│  hipaa_003 (HIPAA / easy)        │
│                                  │
│  ████████████░░░░░░  5 / 12      │
│                                  │
│  Do not leave this screen        │
│                                  │
└──────────────────────────────────┘
```

- Progress bar with entry count
- Current entry ID, mode, and difficulty
- Backend label

---

## Screen: Done (single backend)

```
┌──────────────────────────────────┐
│ ← Benchmark                      │
├──────────────────────────────────┤
│  Results — GPU                   │
│  12 entries, 208 avg chars       │
├──────────────────────────────────┤
│  Per-Step Averages               │
│  ─────────────────────────────── │
│  Step 1 (Classify)               │
│    Latency    TTFT    Tok/s      │
│    773ms      381ms   25.3       │
│                                  │
│  Step 2 (Detect)                 │
│    Latency    TTFT    Tok/s      │
│    1,586ms    375ms   24.9       │
│                                  │
│  Step 3 (Redact)                 │
│    Latency    TTFT    Tok/s      │
│    2,475ms    366ms   24.5       │
├──────────────────────────────────┤
│  Totals                          │
│    Avg latency:  4,855ms         │
│    Avg tokens:   92              │
│    Peak RSS:     1,375MB         │
├──────────────────────────────────┤
│  By Mode                         │
│    HIPAA        4,160ms          │
│    FINANCIAL    4,734ms          │
│    FIELD_SVC    5,284ms          │
│    TACTICAL     5,430ms          │
│    JOURNALISM   4,564ms          │
├──────────────────────────────────┤
│  ┌────────────────────────┐     │
│  │      Run Again         │     │
│  └────────────────────────┘     │
└──────────────────────────────────┘
```

---

## Screen: Done (comparison — both backends)

```
┌──────────────────────────────────┐
│ ← Benchmark                      │
├──────────────────────────────────┤
│  GPU vs NPU Comparison           │
│  12 entries                      │
├──────────────────────────────────┤
│  Per-Step         GPU     NPU    │
│  ──────────────────────────────  │
│  S1 Latency      773ms   345ms  │
│  S1 TTFT         381ms    99ms  │
│  S1 Tok/s         25.3    41.7  │
│  ──────────────────────────────  │
│  S2 Latency    1,586ms   624ms  │
│  S2 TTFT         375ms   104ms  │
│  S2 Tok/s         24.9    41.7  │
│  ──────────────────────────────  │
│  S3 Latency    2,475ms 4,060ms  │
│  S3 TTFT         366ms    92ms  │
│  S3 Tok/s         24.5    41.7  │
├──────────────────────────────────┤
│  Totals          GPU     NPU    │
│  ──────────────────────────────  │
│  Avg latency   4,855ms 5,062ms  │
│  Avg tokens       92     195    │
│  Peak RSS     1,375MB 1,934MB   │
├──────────────────────────────────┤
│  NPU is 1.7x faster per-token   │
│  but generates 2.1x more tokens │
│  ┌────────────────────────┐     │
│  │      Run Again         │     │
│  └────────────────────────┘     │
└──────────────────────────────────┘
```

Green highlighting for the faster value in each row.

---

## Navigation

- Route: `Routes.BENCHMARK`
- Entry point: accessible from HomeScreen (e.g., long-press on backend chip in HudBox, or a settings/debug menu)
- Back button returns to HomeScreen

---

## Data Flow

```
BenchmarkScreen
  └── BenchmarkViewModel
        ├── TextBenchmarkRunner (existing)
        ├── loads shieldtext_bench.jsonl from assets
        ├── filters by difficulty
        ├── runs Steps 1-3 via engine.infer()
        ├── collects per-step InferenceMetrics
        └── aggregates into BenchmarkResults
```

The existing `TextBenchmarkRunner` does the actual work. The ViewModel wraps it with UI state management and progress reporting. No new engine code needed.

---

## Differences from ShieldText-main benchmark

| Aspect | ShieldText-main | Redacto v2 |
|---|---|---|
| Pipeline | Single-pass `engine.redact()` | 3-step pipeline (Classify→Detect→Redact) |
| Scoring | Entity recall, format, preservation | No scoring — performance only |
| Models compared | Standard vs fine-tuned | GPU vs NPU (same model) |
| Metrics | Latency, tok/s, current draw | Latency, TTFT, decode tok/s, RSS (no battery) |
| Backend | GPU only (hardcoded) | User-selectable GPU or NPU |
| Trigger | In-app UI only | ADB broadcast + in-app UI (planned) |
