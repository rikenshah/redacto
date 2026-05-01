# Benchmark Results

**Date:** 2026-05-01
**Device:** Samsung Galaxy S25 Ultra (Snapdragon 8 Elite, SM8750)
**Model:** Gemma 4 E2B Instruct (gemma4.litertlm 2.4GB, gemma4_npu.litertlm 2.8GB)
**Pipeline:** 3-step (Classify → Detect → Redact), no validation step
**Dataset:** shieldtext_bench.jsonl, 30 easy entries across 5 modes

---

## 1. GPU vs NPU — 30 Easy Entries

### Per-Step Averages

| Metric | GPU | NPU | NPU vs GPU |
|---|---|---|---|
| **Step 1 (Classify) latency** | 773ms | 345ms | **2.2x faster** |
| **Step 2 (Detect) latency** | 1,586ms | 624ms | **2.5x faster** |
| **Step 3 (Redact) latency** | 2,475ms | 4,060ms | 0.6x (GPU faster) |
| **Step 1 TTFT** | 381ms | 99ms | **3.8x faster** |
| **Step 2 TTFT** | 375ms | 104ms | **3.6x faster** |
| **Step 3 TTFT** | 366ms | 92ms | **4.0x faster** |
| **Decode tok/s (Step 1)** | 25.3 | 41.7 | **1.6x faster** |
| **Decode tok/s (Step 2)** | 24.9 | 41.7 | **1.7x faster** |
| **Decode tok/s (Step 3)** | 24.5 | 41.7 | **1.7x faster** |
| **Step 1 avg tokens** | 10 | 10 | Same |
| **Step 2 avg tokens** | 30 | 22 | NPU fewer |
| **Step 3 avg tokens** | 51 | 163 | NPU 3.2x more |

### Totals

| Metric | GPU | NPU |
|---|---|---|
| **Avg total latency** | 4,855ms | 5,062ms |
| **Avg total tokens** | 92 | 195 |
| **Avg input chars** | 208 | 208 |
| **Peak RSS** | 1,375MB | 1,934MB |

### By Mode

| Mode | Entries | GPU avg | NPU avg |
|---|---|---|---|
| HIPAA | 4 | 4,160ms | 1,977ms |
| FINANCIAL | 4 | 4,734ms | 2,360ms |
| FIELD_SERVICE | 5 | 5,284ms | 2,325ms |
| TACTICAL | 7 | 5,430ms | 14,201ms |
| JOURNALISM | 10 | 4,564ms | 2,348ms |

---

## 2. Analysis

### NPU is faster per-token but generates more output

- NPU decode speed: **41.7 tok/s** vs GPU **24.5 tok/s** (1.7x)
- NPU TTFT: **92-104ms** vs GPU **366-381ms** (3.6-4.0x)
- But NPU Step 3 generated **163 tokens avg** vs GPU **51 tokens** (3.2x more)
- This equalizes total latency: NPU 5.1s vs GPU 4.9s

**Root cause:** NPU uses no SamplerConfig (set to null because constrained decoding is unsupported on NPU). This makes NPU more verbose. GPU uses topK=64, topP=0.95, temperature=1.0, which constrains output length.

### TACTICAL mode outlier on NPU

NPU averaged **14,201ms** for TACTICAL entries vs GPU's **5,430ms**. One or more TACTICAL entries triggered very long LLM output on NPU due to the unconstrained sampling.

### Memory

NPU uses **~560MB more RSS** (1,934 vs 1,375MB). The NPU model file is larger (2.8GB vs 2.4GB) and the QNN runtime allocates additional buffers.

---

## 3. Single-Entry Comparison (hipaa_001, 229 chars)

| Metric | GPU | NPU |
|---|---|---|
| Total latency | 5,646ms | 2,781ms |
| Step 1 | 955ms | 757ms |
| Step 2 | 2,258ms | 694ms |
| Step 3 | 2,413ms | 1,309ms |
| TTFT (Step 2) | 424ms | 159ms |
| Decode tok/s (Step 3) | 24.3 | 44.9 |
| Total tokens | 122 | 83 |
| Peak RSS | 1,134MB | 1,684MB |

On this short entry, NPU is **2.0x faster overall** because the output is short enough that the verbosity difference doesn't dominate.

---

## 4. Previous Benchmark (ShieldText-main, single-pass)

For reference, the ShieldText-main branch ran 85 entries on GPU with single-pass redaction:
- Standard model: 80.5% overall score, 12.8 tok/s avg
- Fine-tuned model: 70.3% overall score, 9.0 tok/s avg

These numbers are not directly comparable because:
1. Different pipeline (single-pass vs 3-step)
2. Different scoring methodology
3. Different model version (main had older litertlm)
4. Fine-tuned model had wrong output format ([REDACTED] vs [CATEGORY_N])

---

## 5. How to reproduce

```bash
# Launch app
adb shell am start -n com.example.redacto/com.example.starterhack.MainActivity

# Wait for engine init (check logcat)
adb logcat -s InferenceEngine:I

# Run GPU benchmark (30 easy entries)
adb shell am broadcast -n com.example.redacto/com.example.starterhack.benchmark.BenchmarkReceiver \
  -a com.example.redacto.BENCHMARK --es type text --ei count 85 --es difficulty easy

# For NPU: force-stop, clear prefs, relaunch
adb shell am force-stop com.example.redacto
adb shell run-as com.example.redacto rm -f /data/data/com.example.redacto/shared_prefs/shieldtext_prefs.xml
adb shell am start -n com.example.redacto/com.example.starterhack.MainActivity
# Wait for NPU init, then run same benchmark command

# Monitor results
adb logcat -s RedactoBenchmark:I
```

---

## 6. Metrics methodology

| Metric | How measured | Confidence |
|---|---|---|
| Latency | `System.currentTimeMillis()` around full `engine.infer()` call | HIGH — wall-clock, includes all overhead |
| TTFT | Timestamp on first `onMessage` callback minus start time | HIGH — direct measurement |
| Decode tok/s | `(tokenCount - 1) * 1000 / (lastTokenTime - firstTokenTime)` | HIGH — excludes prefill |
| Token count | Increment counter per `onMessage` callback | MEDIUM — assumes 1:1 callback:token |
| Peak RSS | `/proc/self/status` VmRSS line | HIGH — kernel-reported |
