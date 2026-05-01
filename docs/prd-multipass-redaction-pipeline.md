# Redacto — Multi-Pass Redaction Pipeline PRD

**Version:** 1.1
**Created:** 2026-05-01
**Updated:** 2026-05-01
**Authors:** Jaydeep Shah (Edge Artists), Claude Code
**Status:** Implemented (see `docs/engineering-decisions.md` for deviations)
**Scope:** All 7 categories implemented

> **NOTE:** This PRD was the original design. Several decisions changed during
> implementation. The authoritative record of what was actually built and why
> is `docs/engineering-decisions.md`. Key deviation: Step 3 uses LLM instead
> of deterministic replacement (see Decision D4).

---

## 1. Problem Statement

The current redaction pipeline uses a single LLM call with a flat system prompt. This approach:
- Asks the LLM to simultaneously understand document type, detect PII/PHI, replace it with placeholders, and validate correctness — all in one pass
- Provides no verification that redaction is complete
- Uses generic identifier lists regardless of document type
- Has no mechanism to catch missed identifiers
- Produces no confidence signal for the user

The result is inconsistent redaction quality, especially for complex documents like clinical notes where contextual reasoning is required (e.g., "her daughter Lisa" is PHI but "depression" is clinical data that must be preserved).

---

## 2. Design Principles

### 2.1 Two Category Systems (explicitly decoupled)

**UI Categories** — User-facing organizational labels for filing and retrieval.
- Medical, Financial, Tax, Home/Lease, Insurance, ID Documents, Immigration, Auto, Legal, Other
- A document can belong to multiple UI categories
- Selected by the user at save time
- No influence on how redaction is performed

**Redaction Categories** — Content-driven compliance frameworks that govern how redaction happens.
- Medical (informed by HIPAA and general medical privacy)
- Financial (informed by GLBA and general financial privacy)
- Legal (informed by contractual and attorney-client privacy)
- Tactical (law enforcement source/witness protection)
- Journalism (source protection)
- Field Service (customer privacy)
- Named in human-friendly terms, not compliance jargon
- Detected automatically from document content in Step 1
- Multiple redaction categories can apply to a single document

**Decision:** No mapping exists between UI categories and redaction categories. The pipeline analyzes the content and selects the appropriate redaction category independently of where the user files the document.

### 2.2 Accuracy Over Latency

- Users prefer thorough redaction over fast results
- Inference runs on local NPU at no marginal cost
- Multi-pass LLM calls are acceptable
- There is no hard latency budget; quality is the priority

### 2.3 Deterministic Where Possible

- If a step can be done without an LLM (string replacement, pattern matching), do it without an LLM
- LLM calls are reserved for tasks requiring contextual reasoning (classification, detection, validation)

---

## 3. Pipeline Architecture

### 3.1 Overview

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   STEP 1     │    │   STEP 2     │    │   STEP 3     │    │   STEP 4     │
│  CLASSIFY    │───>│   DETECT     │───>│   REDACT     │───>│  VALIDATE    │
│  (LLM call)  │    │  (LLM call)  │    │ (no LLM)     │    │  (LLM call)  │
│              │    │              │    │ deterministic │    │              │
│ New convo    │    │ New convo    │    │ in-memory     │    │ New convo    │
└──────────────┘    └──────────────┘    └──────────────┘    └──────┬───────┘
                                                                   │
                                                            PASS? ─┤
                                                            │      │
                                                           YES    NO
                                                            │      │
                                                            ▼      ▼
                                                         OUTPUT   Loop back
                                                                  to Step 2
                                                                  (max 3 rounds)
```

### 3.2 Step 1 — CLASSIFY

**Purpose:** Determine document type and applicable redaction category.

**Input:** Raw text (from OCR, typed input, or PDF extraction).

**LLM conversation:** New conversation, dedicated classification prompt.

**Prompt structure:**
```
You are a document classifier for a privacy redaction system.

Analyze the following text and determine:
1. DOCUMENT TYPE — what kind of document is this?
2. REDACTION CATEGORY — which privacy category applies?

Available redaction categories:
- Medical: patient records, clinical notes, lab results, prescriptions,
  any document containing health information
- Financial: bank statements, tax returns, W-2 forms, investment
  statements, any document containing financial account information
- Legal: contracts, leases, purchase agreements, legal correspondence
- Tactical: police reports, incident reports, first responder notes
- Journalism: source notes, interview transcripts, investigative notes
- Field Service: service orders, work orders, customer site notes
- General: anything not fitting the above categories

Respond in exactly this format:
DOCUMENT_TYPE: <type>
CATEGORY: <category>

INPUT:
{text}
```

**Output format (line-based, not JSON):**
```
DOCUMENT_TYPE: clinical note
CATEGORY: Medical
```

**Decision:** Step 1 does NOT list sub-identifiers. The detected category determines which Step 2 skill to load, and that skill already contains the full identifier list.

**Error handling:**
- If the LLM returns an unrecognized category, default to "General" which uses a broad PII detection skill
- If the LLM returns multiple categories (e.g., "Medical, Financial"), use the first as primary and run Step 2 with the primary category's identifiers
- If Step 1 fails entirely (LLM error), fall back to the current single-pass prompt as a degraded mode

### 3.3 Step 2 — DETECT

**Purpose:** Find all PII/PHI instances in the text with their categories.

**Input:** Raw text + document type + category from Step 1.

**LLM conversation:** New conversation. The system prompt is loaded dynamically based on the category from Step 1.

**Prompt loading:**
- Medical → loads H1-H18 identifier definitions (what to redact) + H19-H28 definitions (what to preserve)
- Financial → loads F1-F9 identifier definitions + document-specific rules
- Other categories → loads their respective identifier skills

**Prompt structure (Medical example):**
```
You are a PII/PHI detection engine for medical documents.

Analyze the following {document_type} and identify every instance
of protected health information.

IDENTIFIERS TO DETECT (REDACT these):
H1 - Names: patient names, family member names, provider names
H2 - Geographic: addresses, cities, ZIP codes (state name is OK)
H3 - Dates: all dates except year (keep year, redact month/day)
H4 - Phone numbers
H5 - Fax numbers
H6 - Email addresses
H7 - SSN
H8 - Medical record numbers
H9 - Health plan numbers
H10 - Account numbers
H11 - License/certificate numbers
H12 - Vehicle identifiers
H13 - Device identifiers
H14 - URLs
H15 - IP addresses
H16 - Biometric identifiers
H17 - Photographs (note: text only, flag if referenced)
H18 - Any other unique identifying number

IDENTIFIERS TO PRESERVE (NEVER redact these):
- Diagnoses and conditions (e.g., "Type 2 diabetes", "CHF")
- Medications and dosages (e.g., "Metformin 500mg BID")
- Vital signs (e.g., "BP 140/90", "HR 82")
- Body locations (e.g., "left heel", "right lower quadrant")
- Lab result values, units, reference ranges, flags
- Clinical procedures and instructions
- General body systems and symptoms

SPECIAL RULES:
- Ages under 90: keep the age. Ages 90+: replace with ">=90"
- Family members mentioned by name ARE identifiers (H1)
- Provider names ARE identifiers (H1), but specialty is preserved
- First 3 digits of ZIP are OK if population >20,000

For each identifier found, output one line in this format:
IDENTIFIER_CATEGORY: exact text found

Example:
NAME: Jane Smith
NAME: Dr. Michael Torres
DATE: 04/12/1978
DATE: 03/15/2024
PHONE: (312) 555-0148
SSN: 123-45-6789
MRN: 847-293-1
LOCATION: St. Mary's Hospital in Chicago, IL
NAME: Lisa (patient's daughter)

INPUT:
{text}
```

**Output format (line-based):**
```
NAME: Jane Smith
NAME: Dr. Michael Torres
NAME: Lisa
DATE: 04/12/1978
DATE: 03/15/2024
PHONE: (312) 555-0148
SSN: 123-45-6789
MRN: 847-293-1
LOCATION: St. Mary's Hospital in Chicago, IL
```

**Parsing:** Split output by newlines. Each line is `CATEGORY: value`. Parse into a list of `Detection(category: String, text: String)` objects.

**Error handling:**
- If the LLM includes explanatory text alongside detections, filter to only lines matching the `CATEGORY: value` pattern
- If the LLM returns zero detections, proceed to Step 3 (the document may genuinely contain no PII)
- If the LLM response is malformed, retry once with a simplified prompt

### 3.4 Step 3 — REDACT

**Purpose:** Replace detected PII/PHI with structured placeholders.

**Input:** Raw text + detection list from Step 2.

**LLM call:** NONE. This is a deterministic in-memory string replacement.

**Algorithm:**
```kotlin
fun redact(originalText: String, detections: List<Detection>): String {
    // Sort by text length descending to avoid substring conflicts
    // e.g., "St. Mary's Hospital in Chicago, IL" before "Chicago"
    val sorted = detections.sortedByDescending { it.text.length }

    // Track placeholder counters per category
    val counters = mutableMapOf<String, Int>()
    var result = originalText

    for (detection in sorted) {
        val count = counters.getOrDefault(detection.category, 0) + 1
        counters[detection.category] = count
        val placeholder = "[${detection.category}_$count]"
        result = result.replace(detection.text, placeholder)
    }

    return result
}
```

**Decision:** No LLM is used in this step. The LLM has already identified what to redact in Step 2. String replacement is faster, deterministic, and cannot introduce errors (no hallucinated text changes).

**Edge cases:**
- If a detected text appears multiple times, all instances are replaced with the same placeholder
- If a detected text is a substring of another detection, the longer detection is processed first (sorting handles this)
- If a detected text is not found in the original (LLM hallucinated it), skip silently

### 3.5 Step 4 — VALIDATE

**Purpose:** Independent audit of the redacted output for any leaked identifiers.

**Input:** Redacted text from Step 3 + the identifier checklist for the category.

**LLM conversation:** New conversation (independent auditor — not biased by the detection conversation).

**Prompt structure:**
```
You are a privacy compliance auditor. Your job is to check whether
a redacted document still contains any personally identifiable
information that should have been removed.

The document was redacted under MEDICAL privacy rules. Placeholders
like [NAME_1], [DATE_2] etc. are correctly redacted — ignore those.

Check for ANY of the following that might have been MISSED:
- Person names (patients, family members, providers)
- Specific dates (month/day — year alone is OK)
- Phone/fax numbers
- Email addresses
- Social Security Numbers
- Medical record numbers
- Health plan numbers
- Account numbers
- Street addresses, city names, ZIP codes (state name is OK)
- License or certificate numbers
- Any other unique identifying number or code

If everything looks clean, respond with exactly:
RESULT: PASS

If you find leaked identifiers, respond with:
RESULT: FAIL
Then list each leaked item:
CATEGORY: exact leaked text

INPUT:
{redacted_text}
```

**Output parsing:**
- Look for `RESULT: PASS` → pipeline complete, output the redacted text
- Look for `RESULT: FAIL` → parse the missed items, feed them back as additional detections

**Retry loop:**
1. Parse missed items from validation output
2. Add them to the detection list from Step 2
3. Re-run Step 3 (deterministic replacement) with the augmented list
4. Re-run Step 4 (new conversation) on the updated output
5. Maximum 3 total rounds (1 initial + 2 retries)
6. After 3 rounds, output whatever we have — even partial redaction is better than none

**Error handling:**
- If the LLM response doesn't contain `RESULT: PASS` or `RESULT: FAIL`, treat as PASS (conservative: the text was likely already clean and the LLM just didn't follow format)
- If validation keeps failing after 3 rounds on the same items, output the result and flag it to the user

---

## 4. Conversation Management

| Step | Conversation | Rationale |
|---|---|---|
| Step 1 (Classify) | New | Clean slate for classification |
| Step 2 (Detect) | New | Category-specific prompt, no carry-over from classification |
| Step 3 (Redact) | None | Deterministic code, no LLM |
| Step 4 (Validate) | New | Independent auditor must not be biased by detection context |
| Retry Step 2 | New | Fresh detection with additional context about missed items |
| Retry Step 4 | New | Fresh validation of updated output |

**Decision:** Every LLM call gets a fresh conversation. No conversation chaining. This prevents context pollution between steps and ensures each step operates independently.

---

## 5. Cache Management

**App cache (`context.cacheDir`):** Clear between documents to remove stale conversation artifacts. Do NOT clear between pipeline steps within the same document.

**LiteRT AOT cache:** Keep intact. Only clear when the model file itself is swapped (e.g., switching from `gemma4.litertlm` to `gemma4_npu.litertlm`).

**Conversation objects:** Close each conversation after its step completes. The `conversation.close()` call releases native memory.

---

## 6. Prompt Source of Truth

The `prompts/` directory in the repository is the source of truth for identifier definitions and compliance rules.

| Category | Identifier Source | Compliance Source |
|---|---|---|
| Medical | `prompts/medical-redaction/2-identifiers/H1-H18-hipaa-identifiers/SKILL.md` + `H19-H28-medical-context/SKILL.md` | `prompts/medical-redaction/3-compliance/HIPAA/` |
| Financial | `prompts/financial-redaction/2-identifiers/personal/F1-F9-identity/SKILL.md` | `prompts/financial-redaction/3-compliance/GLBA/` |
| Legal | `prompts/legal-redaction/SKILL.md` (planned) | Not yet defined |

**Decision:** Prompts are embedded as Kotlin string constants in the codebase (not read from files at runtime). The SKILL.md files serve as the design reference; the Kotlin constants are the implementation. When a SKILL.md is updated, the corresponding Kotlin constant must be updated to match.

---

## 7. Output Format

### 7.1 Placeholder Convention

Format: `[CATEGORY_N]` where:
- `CATEGORY` is the identifier type in UPPER_CASE (e.g., NAME, DATE, SSN, MRN, PHONE, LOCATION)
- `N` is an incrementing integer per category (e.g., NAME_1, NAME_2)

This convention is unchanged from the current implementation. The interactive heatmap and tap-to-un-redact features rely on this format.

### 7.2 Pipeline Metadata (returned alongside redacted text)

```kotlin
data class PipelineResult(
    val redactedText: String,
    val documentType: String,
    val redactionCategory: String,
    val detections: List<Detection>,
    val validationPassed: Boolean,
    val roundsUsed: Int,           // 1-3
    val totalLlmCalls: Int,        // for HUD display
    val totalLatencyMs: Long,
)

data class Detection(
    val category: String,          // e.g., "NAME", "SSN"
    val originalText: String,      // e.g., "Jane Smith"
    val placeholder: String,       // e.g., "[NAME_1]"
)
```

The `detections` list replaces the current `PlaceholderMapper.buildMap()` approach — we now have the exact mapping from detection, not a heuristic reconstruction.

### 7.3 Confidence Score (planned, not immediate)

Future feature: a per-document confidence score based on:
- Validation pass/fail status
- Number of retry rounds needed (fewer = higher confidence)
- Ratio of detected identifiers to expected identifiers for the document type
- Whether any validation items were unfixable after 3 rounds

Display: green (PASS, 1 round), yellow (PASS after retries), red (FAIL after 3 rounds).

---

## 8. Error Handling

| Scenario | Handling |
|---|---|
| Step 1 LLM call fails | Fall back to current single-pass prompt (degraded mode) |
| Step 1 returns unrecognized category | Default to "General" with broad PII detection |
| Step 2 LLM call fails | Fall back to single-pass prompt |
| Step 2 returns zero detections | Proceed normally — document may have no PII |
| Step 2 output is malformed | Retry once with simplified prompt; if still fails, fall back |
| Step 3 detection text not found in original | Skip that detection silently |
| Step 4 LLM call fails | Treat as PASS (we already have Step 2 + 3 output) |
| Step 4 keeps failing after 3 rounds | Output current result, flag to user |
| Context window exceeded | Chunk the input text and process segments independently |
| Engine not ready (still initializing) | Queue the pipeline task (existing behavior) |

**Degraded mode:** The current single-pass `SystemPrompts.build()` call. This ensures the app always produces output even if the multi-pass pipeline encounters errors. The user sees a result; it's just less thoroughly validated.

---

## 9. Implementation Plan

### Phase 1: Medical Category (current scope)

1. Create `RedactionPipeline` class orchestrating the 4 steps
2. Create `PipelinePrompts` object with Step 1, 2, 4 prompt templates for Medical
3. Create `DetectionParser` to parse Step 2 line-based output
4. Create `DeterministicRedactor` for Step 3 string replacement
5. Create `ValidationParser` to parse Step 4 output and extract missed items
6. Update `RedactionViewModel` to use `RedactionPipeline` instead of direct `engine.redact()`
7. Update `RedactionUiState.Success` to carry `PipelineResult` metadata
8. Test with medical documents from `image-data/medical/`

### Phase 2: Financial Category

9. Add Financial Step 2 prompt (F1-F9 identifiers)
10. Add Financial Step 4 validation prompt
11. Test with financial documents from `image-data/financial/`

### Phase 3: Remaining Categories

12. Add prompts for Legal, Tactical, Journalism, Field Service, General
13. Complete the identifier skills in `prompts/` for each

### Phase 4: Confidence Score

14. Implement scoring logic based on validation results
15. Add confidence badge to the result screen HUD

---

## 10. Testing Strategy

### 10.1 Unit Tests

- `DetectionParserTest` — parse well-formed, malformed, and empty LLM outputs
- `DeterministicRedactorTest` — substring conflicts, duplicate texts, missing texts, empty input
- `ValidationParserTest` — PASS/FAIL parsing, mixed format responses
- `PipelinePromptTest` — correct prompt loaded per category

### 10.2 Integration Tests (on-device)

Using documents from `image-data/`:

| Document Set | Category | Expected Identifiers | Test Focus |
|---|---|---|---|
| `medical/patient-records/` | Medical | Names, dates, MRN, SSN, addresses | Full H1-H18 coverage |
| `medical/lab-results/` | Medical | Names, dates, MRN, lab facility | Preserve lab values + flags |
| `medical/handwritten prescriptions/` | Medical | Names, dates, Rx#, DEA# | OCR quality + detection |
| `financial/bank-statement/` | Financial | Names, account#, routing#, SSN | Full F1-F9 coverage |
| `financial/W-2/` | Financial | Names, SSN, EIN, addresses, income | Tax-specific identifiers |

### 10.3 Regression Tests

- Run the current single-pass prompt on the same documents
- Compare: does multi-pass catch identifiers the single-pass missed?
- Compare: does multi-pass preserve clinical/financial data that single-pass incorrectly redacted?

### 10.4 Edge Cases to Test

- Document with zero PII (should pass through unchanged)
- Document with PII in every sentence
- Very short input (1-2 sentences)
- Mixed category document (medical note mentioning financial info)
- Names that are also common words ("Grace", "Will", "Mark")
- Dates that are also years ("born in 2000" — year OK, but "born on January 5, 2000" — redact month/day)
- Addresses where the state name should be preserved but city/street should not

---

## 11. Decisions Log

| # | Decision | Rationale | Date |
|---|---|---|---|
| D1 | UI categories and redaction categories are fully decoupled | User filing ≠ compliance framework; content drives redaction | 2026-05-01 |
| D2 | Multiple compliance frameworks can apply to one document | Real documents cross boundaries (e.g., medical + financial) | 2026-05-01 |
| D3 | Human-friendly category names, not compliance jargon | Users don't know HIPAA/GLBA; they know "Medical"/"Financial" | 2026-05-01 |
| D4 | Accuracy over latency; no hard latency budget | Local NPU has zero marginal cost; users want thorough redaction | 2026-05-01 |
| D5 | Step 3 (Redact) uses no LLM — deterministic replacement | Faster, more reliable, no hallucination risk | 2026-05-01 |
| D6 | Steps 1 and 2 are separate (not combined) | Classification determines which skill to load for detection | 2026-05-01 |
| D7 | Every LLM call gets a fresh conversation | Prevents context pollution; each step is an independent auditor | 2026-05-01 |
| D8 | Line-based output format (not JSON) for LLM responses | More token-efficient; simpler for small model to produce reliably | 2026-05-01 |
| D9 | Max 3 validation rounds (1 initial + 2 retries) | Diminishing returns after 3 rounds; prevents infinite loops | 2026-05-01 |
| D10 | Fall back to single-pass prompt on pipeline failure | App must always produce output; degraded mode is acceptable | 2026-05-01 |
| D11 | Clear app cache between documents, keep AOT cache | Conversation artifacts should not leak; AOT cache speeds startup | 2026-05-01 |
| D12 | Confidence score is planned but not in initial scope | Pipeline must work reliably first; scoring adds UX value later | 2026-05-01 |
| D13 | Prompts embedded as Kotlin constants, SKILL.md is design reference | Runtime file reading adds complexity; constants are faster | 2026-05-01 |

---

## Appendix A: Current vs. Proposed Data Flow

### Current (single-pass)

```
User input
  → SystemPrompts.build(mode, text)     // one flat prompt
  → engine.redact()                      // one LLM call
  → redacted text                        // done
```

### Proposed (multi-pass)

```
User input
  → RedactionPipeline.process(text)
    → Step 1: engine.redact(classifyPrompt)     // LLM call 1
    → parse category
    → Step 2: engine.redact(detectPrompt)       // LLM call 2
    → parse detections
    → Step 3: DeterministicRedactor.redact()    // no LLM
    → Step 4: engine.redact(validatePrompt)     // LLM call 3
    → if FAIL and rounds < 3:
        → augment detections with missed items
        → goto Step 3
        → goto Step 4                           // LLM call 4-5
    → PipelineResult
  → RedactionUiState.Success
```

## Appendix B: Context Window Budget (Gemma 4 E2B)

Max tokens: ~4000 (configured in EngineConfig.maxNumTokens)

| Step | System prompt | Input text | Output | Total budget |
|---|---|---|---|---|
| Step 1 | ~200 tokens | up to ~3000 | ~20 tokens | ~3220 |
| Step 2 | ~800 tokens (identifier list) | up to ~2500 | ~500 tokens (detections) | ~3800 |
| Step 4 | ~400 tokens | up to ~3000 (redacted text) | ~200 tokens | ~3600 |

If input text exceeds ~2500 tokens, chunk into segments of ~2000 tokens with ~200 token overlap. Process each chunk through Steps 2-3 independently, then validate the full reassembled output in Step 4.
