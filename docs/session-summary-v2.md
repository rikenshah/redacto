# Redacto v2 — Session Summary

**Date:** 2026-04-30 to 2026-05-01
**Branch:** v2
**Team:** Edge Artists (Jaydeep, Riken, Bhavik, Tirth)

---

## What was built in this session

### App Setup & Infrastructure
- Cloned ShieldText repo, set up GitHub credentials (repo-local)
- Fetched v2 branch, copied model files to device (gemma4.litertlm, gemma4_npu.litertlm, gemma4_ft.litertlm)
- Resolved INSTALL_FAILED_UPDATE_INCOMPATIBLE (signature mismatch from package rename)

### Phase 3 — Upload & Camera (completed)
- PDF upload via PdfRenderer + ML Kit OCR (later removed — untested, see Dropped Features)
- Text snippets tab replaced Phase 5 placeholder with real snippet cards
- Dynamic bottom bar: "+ New snippet" on Text tab, "+ Add document" on Documents tab
- Text-mode save now only offers "Save as text snippet" (not document)
- After snippet save, navigates directly to Text tab via `?tab=1` query param

### Phase 4 — Redaction Screen (completed)
- Interactive toggle bubbles: tapping a category reveals/hides redacted fields
- Tap-to-un-redact: individual placeholders toggleable via ClickableText
- PlaceholderMapper aligns redacted placeholders with original text

### Phase 5 — Document Detail (completed)
- HorizontalPager-based version stack with swipe and "Viewing" chip
- Pagination dots below pager
- "+ Create new version" button
- Share button on all version rows (including Original)

### Phase 6 — Polish (completed)
- README rewritten for Redacto v2 with team credits
- Category tiles: 4-column grid, grayed out when empty, no numbers
- FAB replaces full-width bottom bar
- Image orientation fix (camera rotation applied)
- FileProvider for sharing images with thumbnail preview
- Version rows show "Original image" / "Redacted image" instead of file paths
- Categories stored and shown in version detail

### Multi-Pass Redaction Pipeline (major feature)
- 4-step pipeline: Classify → Detect → Redact → Validate
- All 7 redaction categories with dedicated prompts (Medical, Financial, Legal, Tactical, Journalism, Field Service, General)
- Pipeline progress indicator on loading screen ("Step 2/4: Detecting Medical identifiers…")
- Indexed-element image redaction (OCR elements indexed, LLM returns indices to redact)
- Retry loop with max 3 validation rounds
- Fallback to single-pass on pipeline failure

### Metrics Overhaul
- Added: TTFT, decode tok/s, peak RSS memory
- Removed: battery current draw, energy (unreliable)
- Every metric has documented measurement method and confidence level

### Benchmarking System
- ADB-triggered via BroadcastReceiver
- Text-based benchmark using 85-entry JSONL dataset (from ShieldText-main)
- Difficulty filter (easy/medium/hard)
- Backend switching (GPU/NPU) with engine-ready wait
- Per-step metrics: latency, TTFT, decode tok/s, token count
- Summary by mode and difficulty
- 12 demo images pushed to device gallery (DCIM/RedactoDemo)

### Sample Text Dropdown
- TextInputScreen: ExposedDropdownMenu with 5 RedactionMode samples
- Pre-fills text field with category-specific sample data

---

## Dropped Features (with reasons)

| Feature | Reason |
|---|---|
| PDF upload | Not tested end-to-end; removed from UploadSheet to avoid demo risk |
| Battery/energy metrics | Software measurement is unreliable (system-wide, 1Hz sampling, no per-process isolation) |
| RegexFallback safety net | User explicitly said "we don't want regex fallback" — LLM-only redaction |
| Color-coded redaction boxes on images | User requested plain black boxes |
| Home/Manage bottom bar buttons | Non-functional; replaced with FAB |
| Confidence score | Planned for future; pipeline must stabilize first |
| Benchmark UI screen | Not built yet; ADB-triggered runner covers the need (spec written, see benchmark-ui-spec.md) |

---

## Key Bug Fixes

| Bug | Root Cause | Fix |
|---|---|---|
| Camera shows during redaction | No navigation to ResultScreen after capture | Navigate immediately, show spinner |
| Image orientation rotated 90° | ImageProxy.toBitmap() ignores rotation | Apply imageInfo.rotationDegrees via Matrix |
| Text snippet not visible after save | Navigated to Documents tab (index 0) | Navigate to `home?tab=1` |
| Share always sends original | Top Share button used first version | Track pager page index, share viewed version |
| HUD fields vs visual boxes mismatch | fieldsRedacted counted from text regex, boxes drawn from detections | Use pipeline's redactedElements directly |
| Benchmark OOM killed | Created second InferenceEngine (doubled 2.4GB) | Reuse app's engine via callback |
| Validation reports placeholders as leaks | LLM flagged [NAME_1] as PII | Parser filter + prompt rewrite |
| Image over-redaction | Word-diff heuristic flagged reformatted text | Replaced with indexed-element pipeline |

---

## Documents written/updated

| Document | Purpose |
|---|---|
| `docs/engineering-decisions.md` | 23 decisions with rationale (authoritative) |
| `docs/prd-multipass-redaction-pipeline.md` | Original PRD, marked v1.1 with deviation notes |
| `docs/todo-multipass-implementation.md` | Implementation checklist with status summary |
| `docs/benchmark-results.md` | GPU vs NPU performance data |
| `docs/benchmark-ui-spec.md` | Spec for in-app benchmark screen |
| `docs/session-summary-v2.md` | This document |

---

## Related documents (pre-existing, not modified)

| Document | What it covers |
|---|---|
| `docs/npu-enablement.md` | 6 NPU failure modes and fixes |
| `docs/native-libs.md` | 9 .so files reference |
| `docs/backend-cascade.md` | NPU → GPU → CPU cascade logic |
| `docs/diagnostics.md` | Error decoder and troubleshooting |
| `docs/runbook.md` | Operational recipes |
| `docs/redacto_session_handoff.md` | Previous session context |
| `docs/redacto_dev_prompt.md` | Original development brief |
| `docs/redacto_context.md` | Design and tech overview |
