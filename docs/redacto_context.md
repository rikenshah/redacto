# Redacto — Full Project Context & Design Brief

## Project Overview

**App name:** Redacto  
**Platform:** Android (Kotlin, Jetpack Compose)  
**Hackathon:** Qualcomm × LiteRT Developer Hackathon (Edge Artists team)  
**Deadline:** May 1, 2026  
**Target device:** Samsung Galaxy S25 Ultra (Snapdragon 8 Elite, SM8750)  
**Core premise:** Zero-trust, on-device PII/PHI redaction and document vault. No internet permission. No cloud calls. Text never leaves the device.

---

## What This App Does

Redacto is a **privacy document vault** that allows users to:
1. Upload documents (photo, gallery, PDF, or typed text)
2. Automatically redact sensitive PII/PHI using on-device AI (Gemma 4 E2B via LiteRT-LM)
3. Save multiple redacted versions of the same document with custom names
4. Organize documents into categories (Medical, Financial, Tax, etc.)
5. Share specific versions of documents without exposing sensitive data
6. Browse, manage, and view all document versions

---

## Tech Stack

### Core AI
- **Primary model:** `google/gemma-4-E2B-it` via LiteRT-LM
- **Model file:** `gemma4.litertlm` (pushed via ADB to `/sdcard/Android/data/com.example.redacto/files/`)
- **Backend cascade:** NPU (Hexagon via QNN) → GPU → CPU
- **NPU libraries:** `com.qualcomm.qti:qnn-litert-delegate:2.34.0`, `com.qualcomm.qti:qnn-runtime:2.34.0`
- **LiteRT-LM:** `com.google.ai.edge.litertlm:litertlm-android:latest.release`
- **Inference config:** temperature=0.05, topK=1 (greedy), topP=1.0

### Android Stack
- Language: Kotlin
- UI: Jetpack Compose + Material 3
- Architecture: MVVM with StateFlow
- OCR: Google ML Kit Text Recognition v2
- Camera: CameraX
- Build: AGP 9.2.0, Kotlin 2.2.10, compileSdk 36, minSdk 31, targetSdk 36, ABI arm64-v8a
- `largeHeap=true` required (2.4GB model)
- No INTERNET permission in manifest

### Safety net
- `RegexFallback` runs in parallel with LLM inference
- Catches: SSN, email, phone, MRN, credit card, IPv4, DOB
- `mergeIntoLlm()` force-redacts anything the model missed

---

## App Architecture

```
UI (Jetpack Compose)
  HomeScreen → CategoryScreen → DocumentDetailScreen
  UploadSheet → RedactionScreen → (saved)
  TextSnippetsScreen
  ModelSetupScreen (shown if model file missing)

ViewModel
  RedactionViewModel — owns LlmEngine lifecycle
  DocumentViewModel — owns document/version CRUD

Engine
  LlmEngine (interface) → InferenceEngine (real) / FakeLlmEngine (tests)
  OcrProcessor (ML Kit)
  RegexFallback

Local Storage
  Room database — documents, versions, categories
  External files dir — original document files
  No cloud sync
```

---

## Redaction Modes / Categories

Each category maps to a redaction profile:

| Category | Redacts | Keeps |
|---|---|---|
| Medical / HIPAA | All 18 PHI identifiers, relational refs | Non-PHI clinical text |
| Financial / Legal | SSN, account numbers, card numbers, routing, tax IDs | Dollar amounts, institution names |
| ID Documents | Name, DOB, ID numbers, address | Document type, issuing authority |
| Home / Lease | Personal info, gate codes, Wi-Fi passwords | Property address, rent amount |
| Tax | SSN, EIN, account numbers | Dollar amounts, form types |
| Immigration | Personal identifiers, case numbers | Country names, dates of travel |
| Auto | Owner name, VIN, license plate, address | Make, model, year |

---

## PII Redaction Categories (Toggle Bubbles)

These appear as scrollable bubble toggles on the redaction screen:

- **Names** — color: `#0D1B2A` (near black)
- **Dates** — color: `#185FA5` (blue)
- **SSN / ID** — color: `#A32D2D` (red)
- **Financial** — color: `#854F0B` (amber)
- **Contact** — color: `#534AB7` (purple)
- **Location** — color: `#854F0B` (amber-brown)

---

## Document Versioning

- One original document can have unlimited versions
- Each version = a named snapshot with specific toggle states saved
- A document can belong to multiple categories simultaneously (e.g. Driving License → ID + Auto)
- Default versions created on upload: "Original" + "Fully redacted"
- User can create additional versions from any existing version
- Version names are user-editable (e.g. "For Landlord", "For Bank", "For HR")

---

## Brand & Design System

### App Name
**Redacto** — the "o" is styled as a teal redaction block in the wordmark

### Colors
```
Navy (primary):     #0D1B2A
Teal (accent):      #0F6E56
Teal light:         #1D9E75
Teal wash:          #E8F8F2
Off-white bg:       #F7F7F5
Card bg:            #FFFFFF
```

### Category Colors
```
Medical:    #185FA5 (blue)    bg: #EBF5FF
Financial:  #854F0B (amber)   bg: #FEF4E6
Insurance:  #534AB7 (purple)  bg: #F2F0FF
Home:       #993556 (pink)    bg: #FFF0F5
Tax:        #3B6D11 (green)   bg: #EAF3DE
ID docs:    #5F5E5A (gray)    bg: #F2F0FF
Auto:       #854F0B (amber)   bg: #FEF4E6
```

### Design Language
- Clean, minimal — inspired by Linear, Apple Notes, Google Stack
- Background: warm off-white `#F7F7F5`
- Cards: pure white `#FFFFFF`, no borders, float on background
- No drop shadows — depth via background contrast only
- No heavy outlines — hairline separators `rgba(0,0,0,0.05)` only
- Typography: 2 weights only — 400 regular, 500 medium
- Border radius: 14px cards, 10px buttons, 8px pills
- Bottom bar: Home (left) + Add Document teal pill (center) + Manage (right)
- All section headers: 10px uppercase, `#BBB`, letter-spacing 0.8px

### Logo
"Redact" in navy + teal rounded rectangle replacing the "O"
App icon: navy background, document lines in white, teal block in bottom-right corner

---

## Screen Inventory

### Screen 1 — Home
- Logo topbar + profile icon
- Two tabs: Documents | Text snippets
- Category grid (2-col): icon, name, version count, doc count badge
- "Add category" tile — dashed border, last in grid
- Recent documents list: thumbnail, name, time, version count, multi-category pills
- Bottom bar: Home | Add Document (teal pill, center) | Manage

### Screen 2 — Upload Sheet (bottom sheet)
- Slides up over dimmed home screen
- Handle bar at top
- 4 options with color-coded icons:
  - Choose from gallery (teal)
  - Take a photo (blue)
  - Upload PDF (amber)
  - Type or paste text (purple)
- Cancel button

### Screen 3 — Redaction Screen
- Back button + Redacted/Original segment switcher in topbar
- **HUD bar** (dark navy): Backend (NPU chip + pulse dot), Latency, Tokens, Speed (t/s), Fields redacted
- Category toggle bubbles (horizontal scroll): colored dots + label + checkmark when active
- Document preview area (white card): filename header, redacted text with color-coded blocks
- "Tap any block to un-redact" hint
- Color legend row
- Save bar: version name input + Save version button

### Screen 4 — Document Detail
- Back + Share button (teal pill) + overflow menu
- Document title + meta (date, version count) + category pills
- Version stack: 3 stacked cards showing depth, swipe hint, active version preview, active category bubbles
- Pagination dots
- All versions list: numbered, name, category summary, individual share button per version
- "Create new version" dashed button

### Screen 5 — Category View
- Back button + overflow
- Category hero card (colored background, icon, title, doc/version count)
- Document list with "Also in: [category]" tag for multi-category docs
- Same bottom bar

### Screen 6 — Text Snippets
- Same topbar as Home, Text tab active
- Snippet cards: title, redacted preview (colored blocks), timestamp, redacted field count, category tag, share button
- Same bottom bar with "New snippet" center action

---

## Key UX Decisions

1. **Multi-category docs** — a document like "Driving License" appears in both "ID" and "Auto" categories. "Also in:" tag shown inline.
2. **Category → auto-redaction profile** — tagging a doc as "Medical" pre-selects HIPAA toggle profile.
3. **Tap to un-redact** — user can tap individual redacted blocks to restore that specific field.
4. **Version naming** — user-controlled names, not auto-generated. Defaults suggested but editable.
5. **Share format choice** — when sharing: PDF with visible redaction marks vs. clean text.
6. **Text snippets** — saved even though sharing same text is rare. Opt-in save prompt after redaction.
7. **HUD metrics** — always visible during redaction: backend (NPU/GPU/CPU), latency, tokens, speed, fields.
8. **Empty states** — friendly onboarding prompt for new users with no documents.
9. **Model missing state** — ModelSetupScreen shown with ADB push command if gemma4.litertlm not found.

---

## Important Technical Notes

1. **Backend cascade must wrap `Engine.initialize()`** — `Backend.NPU()` constructor succeeds even without dispatch library. Failure only surfaces at `Engine.initialize()`.
2. **`largeHeap=true` required** in AndroidManifest.xml — 2.4GB model + tensor buffers need this.
3. **Duplicate .so handling** — multiple QNN AARs ship same native libs. Use `packaging.jniLibs.pickFirsts` for `libQnnCpu.so`, `libQnnHtp.so`, etc.
4. **AOT cache** — LiteRT writes compiled cache to `context.cacheDir`. Delete if model file changes.
5. **Model filename** — app always looks for exactly `gemma4.litertlm` regardless of which variant is pushed.
6. **No INTERNET permission** — intentional. Firebase ML Kit telemetry uses cached background job.
7. **Parallel regex + LLM** — run concurrently with `coroutineScope { async {} }`, merge after LLM completes.
8. **SamplerConfig types** — temperature and topP must be `Double`, topK is `Int`. All three required.
9. **Nested scroll crash** — do NOT put `verticalScroll` inside `AnimatedContent` — causes infinite height constraints crash.
