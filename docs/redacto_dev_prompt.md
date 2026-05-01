# Redacto — Android App Development Brief

## What You Are Building

You are building **Redacto** — a zero-trust, on-device privacy document vault for Android. The app allows users to upload documents (photos, PDFs, or typed text), automatically redact sensitive PII/PHI using an on-device LLM (Gemma 4 E2B via Google LiteRT-LM), save multiple named versions of each document, and share specific versions without exposing sensitive data.

**No internet permission. No cloud calls. All AI inference runs entirely on-device.**

The app targets the Samsung Galaxy S25 Ultra (Snapdragon 8 Elite) and was built for the Qualcomm × LiteRT Developer Hackathon.

---

## Project Context

This app is an evolution of an earlier prototype called "ShieldText". The core AI engine, redaction logic, and Android project structure already exist. You are rebuilding and extending it with a new feature set, new name (Redacto), and a completely redesigned UI.

### Existing codebase reference (ShieldText — github.com/rikenshah/ShieldText):
- `InferenceEngine.kt` — LiteRT-LM engine with NPU→GPU→CPU cascade
- `LlmEngine.kt` — interface (production + test injectable)
- `RegexFallback.kt` — parallel regex safety net
- `RedactionViewModel.kt` — MVVM state management
- `OcrProcessor.kt` — ML Kit OCR wrapper
- `SystemPrompts.kt` — per-mode prompt builder with few-shot examples
- `RedactionUiState.kt` — sealed state interface

You should reuse and extend this engine layer. The UI layer is being completely rebuilt.

---

## Package & Build Config

```
applicationId: com.example.redacto
compileSdk: 36
minSdk: 31
targetSdk: 36
abiFilters: arm64-v8a
largeHeap: true  ← REQUIRED — 2.4GB model needs this
No INTERNET permission in AndroidManifest
```

### Key dependencies:
```kotlin
// LiteRT-LM
implementation("com.google.ai.edge.litertlm:litertlm-android:latest.release")

// QNN NPU delegate
implementation("com.qualcomm.qti:qnn-litert-delegate:2.34.0")
implementation("com.qualcomm.qti:qnn-runtime:2.34.0")

// ML Kit OCR
implementation("com.google.mlkit:text-recognition:16.0.1")

// CameraX
implementation("androidx.camera:camera-camera2:1.4.1")
implementation("androidx.camera:camera-lifecycle:1.4.1")
implementation("androidx.camera:camera-view:1.4.1")

// Compose BOM
implementation(platform("androidx.compose:compose-bom:2026.02.01"))
implementation("androidx.navigation:navigation-compose:2.8.4")

// Room (local database)
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// Duplicate .so fix
packaging.jniLibs.pickFirsts += listOf(
    "**/libQnnCpu.so", "**/libQnnHtp.so",
    "**/libQnnHtpV75Stub.so", "**/libQnnSystem.so"
)
```

---

## Design System

### Colors
```kotlin
val NavyPrimary = Color(0xFF0D1B2A)
val TealAccent = Color(0xFF0F6E56)
val TealLight = Color(0xFF1D9E75)
val TealWash = Color(0xFFE8F8F2)
val BackgroundWarm = Color(0xFFF7F7F5)
val CardWhite = Color(0xFFFFFFFF)

// Category colors
val MedicalBlue = Color(0xFF185FA5)
val FinancialAmber = Color(0xFF854F0B)
val InsurancePurple = Color(0xFF534AB7)
val HomePink = Color(0xFF993556)
val TaxGreen = Color(0xFF3B6D11)
val IdGray = Color(0xFF5F5E5A)

// Redaction block colors (heatmap)
val RedactName = Color(0xFF0D1B2A)
val RedactDate = Color(0xFF185FA5)
val RedactSSN = Color(0xFFA32D2D)
val RedactFinancial = Color(0xFF854F0B)
val RedactContact = Color(0xFF534AB7)
val RedactLocation = Color(0xFF854F0B)
```

### Design principles
- Background: warm off-white `#F7F7F5`
- Cards: pure white, no borders, float on background via contrast alone
- No drop shadows, no heavy outlines
- Hairline separators only: `rgba(0,0,0,0.05)`
- Typography: weight 400 (body) and 500 (medium) only
- Border radius: 14dp cards, 10dp buttons, 8dp pills
- Inspired by: Linear, Apple Notes, Google Stack app

### Logo
"Redact" + teal rounded rectangle replacing the letter "O"

### Bottom Navigation Bar (all main screens)
- Left: Home icon + label
- Center: Teal pill button "Add document" (or "New snippet" on text tab)
- Right: Manage icon + label

---

## Data Models

```kotlin
@Entity
data class Document(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val originalFilePath: String,    // path to original file
    val fileType: FileType,          // IMAGE, PDF, TEXT
    val createdAt: Long,
    val updatedAt: Long
)

@Entity
data class DocumentVersion(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val documentId: String,          // FK to Document
    val name: String,                // user-defined e.g. "For Landlord"
    val redactedContent: String,     // redacted text or file path
    val activeCategories: String,    // JSON list of RedactionCategory enums
    val isOriginal: Boolean = false,
    val createdAt: Long
)

@Entity
data class DocumentCategory(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val documentId: String,          // FK to Document
    val category: Category           // enum
)

enum class Category {
    MEDICAL, FINANCIAL, TAX, HOME, INSURANCE, ID_DOCUMENTS,
    IMMIGRATION, AUTO, LEGAL, OTHER
}

enum class RedactionCategory {
    NAMES, DATES, SSN_ID, FINANCIAL, CONTACT, LOCATION
}

enum class FileType { IMAGE, PDF, TEXT }

data class TextSnippet(
    val id: String,
    val title: String,
    val originalText: String,
    val redactedText: String,
    val activeCategories: List<RedactionCategory>,
    val documentCategory: Category?,
    val createdAt: Long
)
```

---

## Screen Map

```
HomeScreen
  ├── [tab: Documents]
  │     ├── Category grid → CategoryScreen
  │     └── Recent docs list → DocumentDetailScreen
  ├── [tab: Text snippets] → TextSnippetsScreen
  ├── [bottom: Add document] → UploadSheet (bottom sheet)
  └── [bottom: Manage] → ManageCategoriesScreen

UploadSheet
  ├── Choose from gallery → RedactionScreen
  ├── Take a photo → CameraScreen → RedactionScreen
  ├── Upload PDF → RedactionScreen
  └── Type or paste text → TextInputScreen → RedactionScreen

RedactionScreen
  ├── Toggle category bubbles → live re-redact
  ├── Tap block to un-redact → field-level override
  └── Save version → DocumentDetailScreen

DocumentDetailScreen
  ├── Version stack (swipe) → version preview
  ├── Version list → share individual version
  └── Create new version → RedactionScreen

CategoryScreen
  └── Document list → DocumentDetailScreen

TextSnippetsScreen
  └── Snippet cards → share / view

ModelSetupScreen (shown if gemma4.litertlm missing)
```

---

## UI State

```kotlin
sealed interface RedactionUiState {
    object ModelMissing : RedactionUiState
    object Initializing : RedactionUiState
    object Idle : RedactionUiState
    object Loading : RedactionUiState
    data class Success(
        val original: String,
        val redacted: String,
        val backend: String,
        val latencyMs: Long,
        val tokenCount: Int,
        val tokensPerSecond: Float,
        val fieldsRedacted: Int
    ) : RedactionUiState
    data class Error(val message: String) : RedactionUiState
}
```

---

## HUD Metrics (shown during and after redaction)

The redaction screen must show a dark navy HUD bar with these 5 metrics:
1. **Backend** — "NPU" / "GPU" / "CPU" with pulsing green dot while active
2. **Latency** — time from prompt submit to full response (e.g. "1.2s")
3. **Tokens** — total tokens in response
4. **Speed** — tokens per second (e.g. "84 t/s")
5. **Fields** — number of PII fields redacted

These metrics are populated from `Success` state and displayed even after inference completes.

---

## Critical Technical Rules

1. **Backend cascade wraps `Engine.initialize()` not constructor** — `Backend.NPU()` construction succeeds even without dispatch library. Wrap `Engine(config).initialize()` in try/catch per backend.
2. **`largeHeap=true` in AndroidManifest** — mandatory. Without it the app will be killed during model load.
3. **SamplerConfig types** — `temperature: Double`, `topP: Double`, `topK: Int`. All three required, no defaults.
4. **No nested `verticalScroll` inside `AnimatedContent`** — causes `IllegalStateException` with infinite height constraints.
5. **AOT cache in `context.cacheDir`** — delete if model file changes between runs.
6. **Regex runs in parallel** — use `coroutineScope { val llm = async { }; val regex = RegexFallback.scan() }`, merge after await.
7. **Model filename is always `gemma4.litertlm`** regardless of variant pushed.
8. **No INTERNET permission** — do not add it. OCR, LiteRT, and QNN all work offline.

---

## Development Phases

---

### PHASE 1 — Foundation & Engine
**Goal:** App launches, loads model, and can redact text via the LLM engine. No new UI yet — just the engine working end to end with a minimal test screen.

**Deliverables:**
1. New Android project with package `com.example.redacto`
2. All dependencies configured in `build.gradle` including QNN, LiteRT-LM, Room, ML Kit, CameraX
3. `largeHeap=true`, no INTERNET permission
4. Port `LlmEngine.kt` interface from ShieldText
5. Port `InferenceEngine.kt` with NPU→GPU→CPU cascade
6. Port `RegexFallback.kt` with all patterns
7. Port `SystemPrompts.kt` — update for new Category enum
8. Room database setup: `Document`, `DocumentVersion`, `DocumentCategory`, `TextSnippet` entities + DAOs + Database class
9. `RedactionViewModel` with new `RedactionUiState` including HUD metrics fields
10. `ModelSetupScreen` — shown when `gemma4.litertlm` missing, displays ADB push command
11. Minimal single-screen test UI: text input → redact button → shows result + backend name

**Definition of done:** App installs, detects model file, initializes engine (NPU→GPU→CPU), redacts a hardcoded test string, shows which backend was used.

---

### PHASE 2 — Home Screen & Navigation
**Goal:** Full navigation skeleton with all screens stubbed. Home screen fully functional with categories and recent documents.

**Deliverables:**
1. Navigation graph with all routes: HOME, UPLOAD_SHEET, REDACTION, DOCUMENT_DETAIL, CATEGORY, TEXT_SNIPPETS, MODEL_SETUP, CAMERA, TEXT_INPUT, MANAGE_CATEGORIES
2. Design system: Color.kt, Theme.kt, Typography.kt with all brand colors
3. Reusable components:
   - `CategoryCard` — icon, name, doc count badge, version count
   - `DocumentRow` — thumbnail, name, time, version count, multi-category pills
   - `CategoryPill` — colored tag for category labels
   - `BottomBar` — Home | Add Document | Manage
   - `RedactoLogo` — wordmark with teal O block
4. `HomeScreen` fully implemented:
   - Logo topbar
   - Documents / Text tabs
   - Category grid (2-col) with all default categories + "Add category" dashed tile
   - Recent documents section with "Show all"
   - Bottom bar navigating correctly
5. `CategoryScreen` — category hero header + document list (stubbed data)
6. `ManageCategoriesScreen` — reorder, rename, add, delete categories
7. All screens use warm off-white background, white cards, no heavy borders

**Definition of done:** Full navigation works. Home screen shows categories and recent docs. Tapping a category opens category view. Bottom bar navigates correctly.

---

### PHASE 3 — Upload & Camera Flow
**Goal:** User can upload documents from all 4 sources. OCR works for photos. Documents are saved to the database.

**Deliverables:**
1. `UploadSheet` — bottom sheet with 4 options, dimmed background, handle bar, cancel
2. Gallery picker integration — `ActivityResultContracts.PickVisualMedia`
3. `CameraScreen` — CameraX preview + still capture → `ImageProxy.toBitmap()`
4. `OcrProcessor` — ML Kit Text Recognition v2 (port from ShieldText, adapt for new package)
5. PDF import — `ActivityResultContracts.OpenDocument` with `application/pdf` MIME type, extract text via `PdfRenderer`
6. `TextInputScreen` — full-screen text editor with clear/paste actions
7. All 4 paths feed into `RedactionScreen` with the extracted text + source file path
8. Document saved to Room with original file copied to `context.filesDir`
9. Category assignment prompt shown after upload — user picks one or more categories
10. File type detected and thumbnail generated per type

**Definition of done:** User can upload from gallery, camera, PDF, or typed text. Document appears in home screen recent list. Category is assigned. OCR extracts text from photos.

---

### PHASE 4 — Redaction Screen & Version Saving
**Goal:** Full redaction flow with live toggle bubbles, HUD metrics, color heatmap, tap to un-redact, and version saving.

**Deliverables:**
1. `RedactionScreen` fully implemented:
   - Topbar with back + Redacted/Original segment switcher
   - HUD bar (dark navy) with 5 metrics: Backend (NPU chip + pulse), Latency, Tokens, Speed, Fields
   - Horizontal scrollable category toggle bubbles — colored dot, label, checkmark when active
   - Document preview card with redacted text rendered as colored inline blocks
   - "Tap any block to un-redact" hint + tap handler to restore individual field
   - Color legend row
   - Version name input field + Save version button
2. `RedactionHeatmap` composable — annotated text with colored spans per PII category (reuse/extend ShieldText's implementation)
3. `ZeroTrustBadge` — shows active backend name (NPU / GPU / CPU)
4. Toggling a category bubble triggers re-redaction of the current document
5. When user saves: `DocumentVersion` created in Room with version name + active categories JSON
6. After save: navigate to `DocumentDetailScreen`
7. HUD metrics sourced from `Success` state — latencyMs, tokenCount, tokensPerSecond, fieldsRedacted calculated and stored
8. Inference runs on `Dispatchers.Default`, regex runs in parallel

**Definition of done:** Full redaction flow works end to end. Toggle bubbles change what's redacted. HUD shows real metrics from the model. Saving creates a version. Tap to un-redact restores individual fields.

---

### PHASE 5 — Document Detail & Version Management
**Goal:** Full document detail screen with version stack, swipe between versions, per-version sharing, and new version creation.

**Deliverables:**
1. `DocumentDetailScreen` fully implemented:
   - Topbar with back, Share button (teal pill), overflow menu
   - Document title, meta (date, version count), multi-category pills
   - Version stack — 3 stacked cards showing depth (z-index offsets), active version on top
   - Swipe gesture to switch between versions
   - Pagination dots below stack
   - Active version shows mini redacted preview + active category bubbles
   - All versions list below: numbered, name, category summary, share icon per row
   - "Create new version" dashed button at bottom
2. Swiping the stack switches the active version and updates the preview
3. Share flow:
   - Share button (top) shares currently active version
   - Share icon per version row shares that specific version
   - Share sheet: "Share as PDF" vs "Share as plain text" option
   - Clean text share via system share sheet
   - PDF share via `PdfDocument` API generating redacted PDF
4. "Create new version" opens `RedactionScreen` pre-filled with current version's toggle state
5. Overflow menu: rename document, delete document, add/remove categories

**Definition of done:** User can browse all versions by swiping the stack. Each version can be shared independently. New versions can be created from existing ones. Document can be renamed and deleted.

---

### PHASE 6 — Text Snippets & Polish
**Goal:** Text snippets tab fully working. Empty states. Performance optimizations. Hackathon demo readiness.

**Deliverables:**
1. `TextSnippetsScreen` fully implemented:
   - Snippet cards with: title, redacted preview (colored inline blocks), timestamp, field count, category tag, share button
   - "New snippet" bottom bar action → `TextInputScreen` → `RedactionScreen`
   - After redaction: "Save this snippet?" prompt with title input
   - Snippets stored in Room `TextSnippet` table
2. Empty states for all screens:
   - Home with no documents: illustration + "Add your first document" CTA
   - Category with no docs: friendly message + add button
   - Text snippets with none: brief explainer + new snippet CTA
3. `ModelSetupScreen` polish — clear instructions, ADB command in a copyable code block, retry button
4. Performance:
   - `LazyColumn` for all lists
   - `rememberSaveable` for transient UI state (input text, selected version index)
   - Thumbnail caching for document cards
   - Engine lifecycle tied to ViewModel `onCleared()`
5. Accessibility: content descriptions on all icon buttons
6. Error handling: snackbar for all error states, retry on engine failure
7. Loading states: skeleton shimmer on document lists while Room loads
8. Unit tests:
   - `FakeLlmEngine` updated with new HUD metric fields
   - `RedactionViewModelTest` updated for new state
   - `RegexFallbackTest` all patterns
9. README updated with: app description, team names/emails, setup instructions, ADB model push command, build instructions

**Definition of done:** Complete app. All flows work. Empty states handled. Model missing state shows setup screen. App is demo-ready for hackathon judges. README complete per submission requirements.

---

## Development Notes for Claude

### Working independently
- Always implement the full feature described — do not stub or leave TODOs unless explicitly told to
- Each phase must produce a buildable, runnable app
- Prefer Kotlin idiomatic code — use `sealed class`, `data class`, `StateFlow`, `collectAsStateWithLifecycle`
- All Composables should be stateless where possible — state hoisted to ViewModel
- Use `Dispatchers.IO` for database operations, `Dispatchers.Default` for inference

### File organization
```
com.example.redacto/
  MainActivity.kt
  engine/
    LlmEngine.kt
    InferenceEngine.kt
    OcrProcessor.kt
    RegexFallback.kt
    SystemPrompts.kt
    RedactionMode.kt
  data/
    AppDatabase.kt
    entities/  (Document.kt, DocumentVersion.kt, etc.)
    dao/       (DocumentDao.kt, VersionDao.kt, etc.)
    repository/ (DocumentRepository.kt)
  ui/
    theme/     (Color.kt, Theme.kt, Type.kt)
    components/ (CategoryCard.kt, DocumentRow.kt, BottomBar.kt, etc.)
    screens/   (HomeScreen.kt, RedactionScreen.kt, etc.)
    viewmodel/ (RedactionViewModel.kt, DocumentViewModel.kt)
  navigation/
    NavGraph.kt
    Routes.kt
```

### Do not
- Add INTERNET permission
- Use cloud APIs or Firebase for core features
- Use `GlobalScope`
- Put business logic in Composables
- Use `verticalScroll` inside `AnimatedContent`
- Hardcode the model filename — always reference a constant `MODEL_FILENAME = "gemma4.litertlm"`

### Model file location
```kotlin
val modelFile = File(context.getExternalFilesDir(null), "gemma4.litertlm")
```
Always check `modelFile.exists()` on app start. If false → navigate to `ModelSetupScreen`.
