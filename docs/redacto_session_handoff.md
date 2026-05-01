# Redacto — Session Handoff (2026-04-30)

This file is for the next Claude Code session that picks up Redacto work. It is
companion to `redacto_dev_prompt.md` (the original dev brief) and
`redacto_context.md` (the design + tech overview). Read those two first if
you have not yet — they describe *what we are building* and the constraints.
This file describes *where we are right now* and *what to do next*.

---

## Quick orientation

- Repo: `C:\Users\tnaik\StudioProjects\ShieldText`
- Branch: **`v2`** (push there, not `main`)
- Latest commit on `v2` at handoff time: `Redacto v2: gallery upload, save flow, doc detail, snippets, navy HUD`
- Package on device: `com.example.redacto` (applicationId), namespace is `com.example.starterhack` (do not rename — files import from this namespace)
- Test device: Samsung Galaxy S25 Ultra, ADB serial **`R3CXC0803WB`**, on Android 16 (Snapdragon 8 Elite, SM8750 / Hexagon V79)
- Build JDK: `C:\Program Files\Android\Android Studio\jbr`
- ADB: `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`

Memory file: `C:\Users\tnaik\.claude\projects\C--Users-tnaik-StudioProjects-ShieldText\memory\project_shieldtext.md` has device + model file facts.

---

## Phase status (against `redacto_dev_prompt.md`)

| Phase | Spec section | State | Notes |
|---|---|---|---|
| 1. Foundation & engine | Engine + Room + ViewModel + setup screen | ✅ done | `InferenceEngine` cascades NPU → GPU → CPU. Room schema: `Document`, `DocumentVersion`, `DocumentCategory`, `TextSnippet`. |
| 2. Home + nav | Nav graph, design system, components, Home, Category, Manage | ✅ done | Routes in `navigation/Routes.kt`. HomeScreen has Documents/Text tabs. |
| 3. Upload & camera | UploadSheet, gallery, camera, PDF, text | ⚠️ partial | Camera + gallery + text wired. **PDF deferred** (UploadSheet shows it as "Coming soon"). |
| 4. Redaction screen + version save | HUD, toggles, heatmap, save versions | ⚠️ partial | Navy HUD bar + toggle bubbles + heatmap done. **Live re-redact when toggling categories is NOT implemented** (bubbles are read-only indicators of what was redacted). **Tap-block-to-un-redact NOT implemented.** Save flow persists to Room. |
| 5. Document detail + version mgmt | Version stack swipe, share, create new version | ⚠️ partial | Numbered versions list + per-row share + rename + delete done. **Version-stack swipe gesture NOT implemented** (just a list). **"Create new version" is missing** — no button yet. |
| 6. Text snippets + polish | Snippet cards, empty states, perf, README, tests | ⚠️ partial | Snippet cards + share + delete + empty state done. **No tests updated for new save methods.** **README not updated.** ManageCategoriesScreen says "Custom categories — coming soon." |

Critical-path gaps if you are pushing toward demo readiness:
1. Live category toggling on the redaction screen (the bubbles look interactive but are not)
2. Tap-block-to-un-redact (also looks interactive, is not)
3. Create new version flow on `DocumentDetailScreen`
4. Categorization profile auto-selection when a document gets categorized as Medical etc. (per `redacto_context.md` §"Key UX Decisions" point 2)

---

## What works end-to-end right now

1. App launches → if model file at `/sdcard/Android/data/com.example.redacto/files/gemma4*.litertlm` is missing, ModelSetupScreen prompts the ADB push command. Otherwise → HomeScreen.
2. Backend chip in topbar HUD opens a dropdown to switch ModelVariant (NPU / GPU / CPU). Selection persists, engine restarts on switch. NPU path is known-good on this device after `LD_LIBRARY_PATH` + `ADSP_LIBRARY_PATH` setup in `InferenceEngine.configureNativeRuntime`.
3. Add document → Gallery → pick a photo → spinner → redacted image with HUD metrics → Save section (name + categories + Save version).
4. Add document → Take a photo → camera preview → capture → spinner → redacted image (same flow as gallery).
5. Add document → Type or paste text → input screen → Redact button → spinner → redacted text with heatmap → Save as document or Save as text snippet.
6. Save Document → lands on `DocumentDetailScreen` showing the new doc with Original + Fully redacted versions, share each one, rename, delete.
7. Home → Categories grid → tap any → CategoryScreen lists docs in that category. Multi-category docs appear in each.
8. Home → Text tab → snippets list with share + delete.

Pending requests (text or image picked while engine is still initializing) are now **queued** in the VM and fire automatically once init completes — they used to error out with "Engine not ready."

---

## Key files touched in this session

| File | What changed |
|---|---|
| `engine/InferenceEngine.kt` | (mostly stable, do not refactor) — backend cascade, native lib path setup |
| `ui/RedactionViewModel.kt` | Added `PendingTask` queue (drains after init); `redactImage`/`redactText` set Loading synchronously and queue if engine not ready; `selectModelVariant` triggers re-init |
| `ui/components/RedactionHudBar.kt` | **New** — dark navy HUD bar (backend chip + dot + 4 stats); backend chip is tappable for variant switching |
| `ui/components/HudBox.kt` | Backend chip is tappable for variant switching (Home topbar) |
| `ui/components/RedactoTopBar.kt` | Logo restyled: "Redact" + teal rounded block with white "o" inside |
| `ui/screens/UploadSheet.kt` | Vertical list with icon + title + description + chevron; PDF row is greyed "Coming soon" |
| `ui/screens/HomeScreen.kt` | Wired gallery picker; passes selectedVariant + onSelectVariant to HudBox |
| `ui/screens/ResultScreen.kt` | Major rewrite: handles Loading / Success / Error states; segment switcher in topbar; uses `RedactionHudBar`; toggle-bubble row (read-only); color legend; save section condenses to name + chips + "Save version" + optional "Save as snippet" |
| `ui/screens/DocumentDetailScreen.kt` | Built from stub: title + meta + category pills + active version preview + numbered version list with per-row share + rename + delete |
| `ui/screens/TextSnippetsScreen.kt` | Built from stub: snippet cards with heatmap preview + share + delete + empty state |
| `ui/viewmodel/DocumentViewModel.kt` | Added `saveDocument`, `saveSnippet`, `saveAdditionalVersion`, `renameDocument`, `versionsForDocument`, `categoriesForDocument`, `getDocumentById`. Image originals copied to `filesDir/originals/<docId>.png` |
| `data/repository/DocumentRepository.kt` | Added `updateDocument`, `getDocumentById` passthroughs |
| `navigation/NavGraph.kt` | RESULT route now passes `docViewModel` too |
| `gradle.properties` | Added `android.disallowKotlinSourceSets=false` so KSP-generated dirs work under AGP 9 |

---

## Design references

The user provided 6 reference PNGs in `~/Downloads/`:

- `redacto_01_home.png` — categories grid, Recent list, bottom bar
- `redacto_02_upload.png` — vertical upload sheet
- `redacto_03_redaction.png` — **the demo screen** — navy HUD, toggle bubbles, document preview with colored blocks, Save version button
- `redacto_04_doc_detail.png` — active version card with "Viewing" chip, swipe hint, ALL VERSIONS list, "Create new version" dashed button
- `redacto_05_category.png` — colored hero card with icon + title + counts
- `redacto_06_text_snippets.png` — Redacto-style topbar with Documents/Text tabs (Text active)

Current builds approximate `01`, `02`, `03` reasonably. **Not yet matched:** `04` (no version-stack card with "Viewing" chip + pagination dots + "Create new version" button), `05` (CategoryScreen has a basic colored header but not the elevated card style), `06` (TextSnippets has its own simple topbar, not the Redacto wordmark + tabs from Home).

---

## Build + deploy

```bash
JAVA_HOME="C:\Program Files\Android\Android Studio\jbr" ./gradlew :app:assembleDebug
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
"$ADB" -s R3CXC0803WB install -r app/build/outputs/apk/debug/app-debug.apk
"$ADB" -s R3CXC0803WB shell am force-stop com.example.redacto
"$ADB" -s R3CXC0803WB shell am start -n com.example.redacto/com.example.starterhack.MainActivity
```

`deploy.sh` exists but its `APP_PACKAGE` constant still says `com.example.starterhack` (legacy) — it will mis-target launch + model paths. Either fix that constant to `com.example.redacto` or use the manual commands above. (We did not fix the script in this session because the user asked to skip it.)

### When `INSTALL_FAILED_UPDATE_INCOMPATIBLE`

Signature mismatch — happens when Android Studio installed at one point and CLI at another. Fix by uninstalling fully, but **stash the model files first** so the 2.4 GB push doesn't have to repeat:

```bash
"$ADB" -s R3CXC0803WB shell mv /sdcard/Android/data/com.example.redacto/files/gemma4.litertlm /sdcard/Download/gemma4.litertlm.bak
"$ADB" -s R3CXC0803WB shell mv /sdcard/Android/data/com.example.redacto/files/gemma4_npu.litertlm /sdcard/Download/gemma4_npu.litertlm.bak
"$ADB" -s R3CXC0803WB uninstall com.example.redacto
"$ADB" -s R3CXC0803WB install -r app/build/outputs/apk/debug/app-debug.apk
"$ADB" -s R3CXC0803WB shell mv /sdcard/Download/gemma4.litertlm.bak /sdcard/Android/data/com.example.redacto/files/gemma4.litertlm
"$ADB" -s R3CXC0803WB shell mv /sdcard/Download/gemma4_npu.litertlm.bak /sdcard/Android/data/com.example.redacto/files/gemma4_npu.litertlm
```

`pm uninstall -k` (keep data) does **not** clear the prior signature — full uninstall is required.

---

## Known landmines (real ones we hit)

1. **`Engine.NPU()` constructs successfully even without dispatch lib** — failure surfaces only at `Engine.initialize()`. The cascade in `RedactionViewModel.checkModelAndInitialize` wraps `engine.initialize(...)` in `runCatching`, not the constructor. Don't move that `runCatching`.
2. **`largeHeap=true`** is required in AndroidManifest. Without it the 2.4 GB model load gets killed.
3. **AGP 9 + KSP**: KSP adds generated source dirs through the `kotlin.sourceSets` DSL which AGP 9 disallows by default. We set `android.disallowKotlinSourceSets=false` in `gradle.properties`. If you ever need a "real" fix, AGP wants those dirs added through `android.sourceSets`.
4. **`pm uninstall -k`** keeps signing certs around; it cannot resolve a signature mismatch. Full uninstall required (see above).
5. **Model files are 5.4 GB total** (2.4 GB generic + 2.9 GB NPU). They live under `/sdcard/Android/data/com.example.redacto/files/`. Don't accidentally `adb uninstall` without stashing them first.
6. **`gemma4_sm8750.litertlm` (the 2.9 GB NPU model) is in the repo root** — it is `.gitignore`d to prevent accidental push.

---

## Suggested next steps (priority order)

If you want to keep going on the demo polish:

1. **Wire toggle bubbles to live re-redaction.** Today they are read-only indicators. The spec wants tapping a bubble to re-run the LLM with a different category profile. Approach: pass a `selectedCategories` set to `redactText`/`redactImage`; build the prompt from that set instead of a single `RedactionMode`.
2. **Tap-block-to-un-redact.** Replace the current `RedactionHeatmap` with an annotated `ClickableText` that, on click of a placeholder, looks up the original token from the regex match in `RegexFallback.scan(originalText)` and substitutes it back in.
3. **DocumentDetail "Create new version"** — dashed button at the bottom of `DocumentDetailScreen`. Open a sheet or a screen that re-runs redaction on the source (text or image stored on the document) with a new version name and toggle state, and saves the result via `docViewModel.saveAdditionalVersion`.
4. **Version-stack swipe card** on DocumentDetail (per `redacto_04_doc_detail.png`) — Pager-based card stack with z-offset, "Viewing" chip on the active card, pagination dots.
5. **Category auto-profile.** When a document is tagged Medical, default to HIPAA prompt; Financial → financial profile, etc. The mapping is already in `SystemPrompts.toRedactionMode()`. Just need to surface it on the redaction screen.
6. **Update `deploy.sh`** to use `com.example.redacto` instead of `com.example.starterhack`.
7. **README**: rewrite for Redacto, list the team (Edge Artists: Bhavik, Jaydeep, Riken, Tirth), add ADB push instructions for both model variants.
8. **Tests**: `FakeLlmEngine` exists somewhere — extend it with the HUD metric fields and add a `RedactionViewModelTest` for the queueing behavior.

---

## How the user wants to work

- They will read transcripts, but expect terse status. State results, not deliberation.
- They use the device for testing (R3CXC0803WB). When they say "deploy", run the build + install + launch chain above.
- They want incremental, testable increments — not one giant changeset. Find natural stopping points and offer to pause for testing.
- They asked to defer PDF in this session ("just do images"). Don't re-introduce PDF unless asked.
- The `model/` directory is empty in git but contains the actual `.litertlm` files locally. Push them to the device with ADB; never check them into git.

---

## Useful greps

- "Where is the model file path defined?" → `RedactionViewModel.modelFile()` and `ModelVariant.fileName`
- "Where is the backend cascade?" → `RedactionViewModel.checkModelAndInitialize()`
- "Where do I add a new redaction prompt?" → `engine/SystemPrompts.kt`
- "Where does the heatmap colorize categories?" → `ui/components/RedactionHeatmap.kt`
- "Where do I add a new screen route?" → `navigation/NavGraph.kt` + `Routes` companion
