# ShieldText engineering docs

This folder captures everything learned wiring up on-device LLM inference for Gemma 4 E2B on the Samsung Galaxy S25 Ultra (Snapdragon 8 Elite, SM8750-AC, Hexagon V79). The state of these docs reflects work through **2026-04-30**.

| Document | Purpose |
|---|---|
| [npu-enablement.md](./npu-enablement.md) | **Read this first.** Full root-cause walkthrough for NPU on SM8750 — six layered failure modes and how each was fixed. The headline result: NPU init succeeds in 2.5s on first launch. |
| [native-libs.md](./native-libs.md) | What `.so` files we bundle in `app/src/main/jniLibs/arm64-v8a/`, where each comes from, and what happens if any are missing. |
| [backend-cascade.md](./backend-cascade.md) | How `RedactionViewModel` cascades NPU → GPU → CPU, why fallbacks aren't persisted to disk, and the in-process re-init limitation. |
| [diagnostics.md](./diagnostics.md) | Common errors, what each one means, and the exact `adb logcat` filters that surface them. |
| [runbook.md](./runbook.md) | How to push models, clear prefs/cache, install/launch, and inspect device-side QNN paths. |

## TL;DR for someone who needs working NPU today

1. Bundle six libs in `jniLibs/arm64-v8a/` (see [native-libs.md](./native-libs.md)).
2. Seed `ADSP_LIBRARY_PATH` in `Application.onCreate()` **before** any `litertlm` lib loads — see `ShieldTextApp.kt`. This is the single most important fix.
3. Set `visionBackend = Backend.CPU(), audioBackend = Backend.CPU()` (NOT `Backend.GPU()` per the dev guide) — the GPU sub-backend hits an unimplemented `CreateSharedMemoryManager` on Android 16 that takes the whole engine init down.
4. `ExperimentalFlags.enableConversationConstrainedDecoding = false` for all backends — the NPU executor returns "error 12: not supported" if you turn it on.
5. NPU works **once per process**. After switching to CPU/GPU, you cannot return to NPU without killing the process — Hexagon DSP PD reservation can't be re-acquired.
