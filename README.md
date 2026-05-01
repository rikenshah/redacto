# Redacto

**Zero-trust, on-device privacy document vault for Android.**
Built for the Qualcomm x LiteRT Developer Hackathon — powered by Gemma 4 E2B via Google LiteRT-LM on Snapdragon 8 Elite.

No cloud calls. No INTERNET permission. All AI inference runs entirely on-device.

---

## Team — Edge Artists

| Name | Role |
|---|---|
| Bhavik | UI/UX Design |
| Jaydeep | Performance & AI Model Optimization |
| Riken | Application Development |
| Tirth | Test & Research |

---

## What it does

Redacto turns every smartphone into a zero-trust privacy layer. Users can upload documents (photos, PDFs, or typed text), automatically redact sensitive PII/PHI using an on-device LLM, save multiple named versions, and share specific versions without exposing sensitive data.

### Key features

- **4 input sources** — Gallery, Camera, PDF, or typed text
- **5 redaction modes** — Medical/HIPAA, Tactical, Journalism, Field Service, Financial
- **Interactive redaction** — Toggle category bubbles to reveal/hide redacted fields, tap individual blocks to un-redact
- **Document vault** — Save documents with multiple redaction versions, organize by category
- **Text snippets** — Quick-save redacted text for reuse
- **Color-coded heatmap** — Visual display of what was redacted and why (Names, Dates, SSN, Contact, Location)
- **Live HUD metrics** — Backend (NPU/GPU/CPU), latency, token count, speed (tok/s), fields redacted
- **Version management** — Swipe between versions, create new versions, share individually

---

## Redaction modes

| Mode | Targets | Keeps |
|---|---|---|
| **Medical / HIPAA** | All 18 PHI identifiers + relational references | Non-PHI clinical text |
| **Tactical / First Responder** | Victim, witness, minor names and addresses | Suspect description, vehicle, plate |
| **Journalism** | Source names, meeting locations, contact methods | Public officials, institutions, dates |
| **Field Service** | Customer PII, gate codes, Wi-Fi passwords | Equipment details, fault codes |
| **Financial / Legal** | SSN, account/card/routing numbers, tax IDs | Dollar amounts, institution names |

---

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                    Jetpack Compose UI                      │
│  Home · Upload · TextInput · Result · DocDetail · Camera  │
└──────────────────────┬───────────────────────────────────┘
                       │ StateFlow
┌──────────────────────▼───────────────────────────────────┐
│  RedactionViewModel          DocumentViewModel            │
│  Engine lifecycle + redact   Room CRUD + save flow        │
└─────┬──────────────────────────────────┬─────────────────┘
      │                                  │
┌─────▼──────┐  ┌────────▼───────┐  ┌───▼──────────┐
│ LlmEngine  │  │  OcrProcessor  │  │  Room DB      │
│ (interface)│  │  (ML Kit)      │  │  (Document,   │
└─────┬──────┘  └────────────────┘  │   Version,    │
      │                              │   Snippet)    │
┌─────▼──────────────────────────┐  └──────────────┘
│        InferenceEngine          │
│  NPU → GPU → CPU cascade       │
│  LiteRT-LM + Gemma 4 E2B       │
└─────────────────────────────────┘
```

---

## Screen map

```
HomeScreen
  ├── [tab: Documents] → Category grid → CategoryScreen → DocumentDetailScreen
  ├── [tab: Text]      → Snippet cards with heatmap preview
  ├── [bottom: + Add]  → UploadSheet (Gallery / Camera / PDF / Text)
  └── [bottom: Manage] → ManageCategoriesScreen

UploadSheet → Gallery / Camera / PDF / Text → ResultScreen
ResultScreen → Toggle bubbles + tap-to-un-redact + save
DocumentDetailScreen → Version pager (swipe) + version list + create new version
```

---

## Model

**Gemma 4 E2B Instruct** — `litert-community/gemma-4-E2B-it-litert-lm` on HuggingFace
License: Apache 2.0

| File | Size | Use |
|---|---|---|
| `gemma-4-E2B-it.litertlm` | ~2.4 GB | CPU/GPU inference |
| `gemma-4-E2B-it_qualcomm_sm8750.litertlm` | ~2.8 GB | Snapdragon 8 Elite NPU |

### Deploy to device

```bash
# NPU variant (S25 Ultra)
adb push gemma4_npu.litertlm /sdcard/Android/data/com.example.redacto/files/gemma4_npu.litertlm

# CPU/GPU variant
adb push gemma4.litertlm /sdcard/Android/data/com.example.redacto/files/gemma4.litertlm
```

---

## Building and running

### Prerequisites
- Android Studio Meerkat or later
- Android SDK 36, JDK 21 (bundled with Android Studio)
- Device: Samsung Galaxy S25 Ultra (or any arm64 Android 12+ with 8 GB RAM)

### Build & install
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew installDebug
```

### If `INSTALL_FAILED_UPDATE_INCOMPATIBLE`
Signature mismatch from a prior install. Uninstall first, but stash model files:
```bash
adb shell cp /sdcard/Android/data/com.example.redacto/files/gemma4.litertlm /sdcard/Download/
adb uninstall com.example.redacto
./gradlew installDebug
adb shell cp /sdcard/Download/gemma4.litertlm /sdcard/Android/data/com.example.redacto/files/
```

---

## Build config

```
applicationId: com.example.redacto
namespace: com.example.starterhack
compileSdk: 36
minSdk: 31
targetSdk: 36
abiFilters: arm64-v8a
largeHeap: true (required for 2.4 GB model)
No INTERNET permission
```

---

## Target hardware

**Primary:** Samsung Galaxy S25 Ultra
- SoC: Snapdragon 8 Elite (SM8750)
- NPU: Hexagon V79 via QNN delegate
- Speed: ~85+ tok/s on NPU

**Fallback:** Any arm64 Android 12+ device
- GPU (Adreno 830): ~10-20 tok/s
- CPU: slower, always available

---

## Known issues

- `Backend.NPU()` construction succeeds even without dispatch library — failure only surfaces at `Engine.initialize()`
- `largeHeap=true` is mandatory in AndroidManifest — without it the app gets killed during model load
- AGP 9 + KSP requires `android.disallowKotlinSourceSets=false` in gradle.properties
- Model files total ~5.4 GB on device — don't `adb uninstall` without stashing them first
