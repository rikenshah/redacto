# Backend cascade & variant selection

## Overview

The app exposes three model variants in the UI dropdown:

| Variant | Model file | Backend | Description |
|---|---|---|---|
| `NPU (SM8750)` | `gemma4_npu.litertlm` (3.0 GB) | `Backend.NPU(nativeLibDir)` | Hexagon V79 — fastest, lowest power |
| `GPU` | `gemma4.litertlm` (2.5 GB) | `Backend.GPU()` | Adreno via OpenCL/OpenGL — broken on Android 16 (see below) |
| `CPU` | `gemma4.litertlm` (2.5 GB) | `Backend.CPU()` | XNNPACK — universal fallback, slowest |

Defined in `RedactionViewModel.ModelVariant`.

## Default variant

`ModelVariant.NPU` is the default for fresh installs (no SharedPreferences entry for `selected_variant`). This guarantees the cascade exercises NPU on first launch.

## Cascade behavior

When the app launches or the user picks a variant from the dropdown, `RedactionViewModel.checkModelAndInitialize()` runs a cascade defined by `requested → ladder`:

```kotlin
val ladder = when (requested) {
    ModelVariant.NPU -> listOf(ModelVariant.NPU, ModelVariant.GPU, ModelVariant.CPU)
    ModelVariant.GPU -> listOf(ModelVariant.GPU, ModelVariant.CPU)
    ModelVariant.CPU -> listOf(ModelVariant.CPU)
}
```

For each variant in the ladder:
1. Check the variant's model file exists on disk; if not, skip.
2. Call `engine.initialize(file.absolutePath, variant.backend)`.
3. If init succeeds, set state to `Idle` and stop the cascade.
4. If init throws, capture the exception and try the next variant.

If every variant fails, surface `RedactionUiState.Error` with the last exception's message.

## Why each variant pairs with a specific model

The variants don't just differ in `Backend.X()` — they also point at different model files:

- **NPU model** (`gemma4_npu.litertlm`): pre-compiled with QNN context binaries containing `DISPATCH_OP` custom ops that delegate to the Hexagon DSP. Cannot run on CPU or GPU — those backends throw `unresolved custom op: DISPATCH_OP`.
- **Generic model** (`gemma4.litertlm`): standard tflite ops only. Works on both CPU (via XNNPACK) and GPU (via OpenCL).

This is why an earlier "engine-level" cascade was wrong: it tried to fall back from NPU to GPU using the **same** `gemma4_npu.litertlm` model, which then failed with `Input tensor not found`. The cascade has to live at the viewmodel layer where each variant pairs with its correct file.

## Why we don't persist fallback variants

Earlier the cascade persisted the working backend on success:

```kotlin
// REMOVED:
if (variant != requested) {
    _selectedVariant.value = variant
    prefs.edit().putString("selected_variant", variant.name).apply()
}
```

Problem: if NPU init transiently failed (e.g., DSP busy, cache miss) and we fell to CPU, the next launch would skip NPU forever — even after the transient cause cleared. The user's *requested* choice should stay on disk; the *active* backend for the current session is exposed via `engine.activeBackend` for the UI.

Now: only `selectModelVariant()` writes to prefs (when the user explicitly picks). Fallbacks happen in-memory only.

## In-process re-init limitation

**Once NPU has been the active backend in a process and you switch to CPU/GPU, you cannot return to NPU within the same process.** The Hexagon DSP's PD (Protection Domain) reservation can't be re-acquired, manifesting as:

```
E QnnDsp: Failed to find available PD ... err: 1002
E tflite : Encountered unresolved custom op: DISPATCH_OP.
```

What works:
- Fresh install / first launch: NPU works.
- App relaunched after kill: NPU works again.
- NPU active → switch to CPU/GPU: works.

What doesn't work:
- CPU/GPU active → switch back to NPU: fails. Cascade falls back to CPU.

Workaround: kill and relaunch the app. UI doesn't currently call `Process.killProcess(myPid())` automatically on NPU re-selection, but adding that would solve the UX issue cleanly.

## Engine close / reuse

`InferenceEngine.initialize()` always closes any prior `Engine` instance before creating a new one:

```kotlin
engine?.runCatching { close() }
engine = null
isReady = false
tryInitBackend(modelPath, preferredBackend, nativeLibDir)
```

`InferenceEngine.close()` is also called from `RedactionViewModel.selectModelVariant()` and `onCleared()`. These are belt-and-suspenders — even with double-close, the second one is a no-op since `engine = null` after first.

## Test-only bypass

If `engine` is not an `InferenceEngine` (e.g., `RegexOnlyLlmEngine` in debug, `FakeLlmEngine` in unit tests), the model-file check is skipped entirely and state goes straight to `Idle`:

```kotlin
if (engine !is InferenceEngine) {
    _uiState.value = RedactionUiState.Idle
    return
}
```

This lets unit tests run without staging real `.litertlm` files.
