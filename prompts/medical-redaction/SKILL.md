---
name: medical-redaction
description: Redacts Protected Health Information (PHI) from medical documents to ensure HIPAA Safe Harbor compliance. Detects and removes all 18 HIPAA identifiers (names, dates, MRNs, SSNs, addresses, phone numbers, device IDs, biometrics, photos) while preserving clinical meaning. Use when processing patient records, clinical notes, prescriptions, lab results, discharge summaries, or when the user mentions HIPAA, PHI, medical privacy, healthcare redaction, patient data, or de-identification.
---

# Medical Redaction Skill

## Purpose

This skill teaches the LLM how to redact medical documents for **HIPAA compliance** while preserving clinical meaning.

**Primary goal**: Remove all PHI (Protected Health Information) but keep medical information needed for patient care.

## The Problem We Solve

From your use case:
> "Every day, healthcare workers violate HIPAA — not out of negligence, but necessity. A home health nurse needs to update the care team NOW, but the compliant messaging app is slow. So she texts patient info and risks $1.9M in fines."

**ShieldText's Solution**: On-device redaction that happens in <1 second, allowing healthcare workers to communicate quickly while staying HIPAA compliant.

---

## HIPAA Overview

**HIPAA** = Health Insurance Portability and Accountability Act (1996)

**What it Protects**: Protected Health Information (PHI)

**The 18 HIPAA Identifiers** that must be redacted:
1. Names
2. Geographic subdivisions smaller than state
3. Dates (except year)
4. Telephone numbers
5. Fax numbers
6. Email addresses
7. Social Security Numbers
8. Medical Record Numbers
9. Health plan beneficiary numbers
10. Account numbers
11. Certificate/license numbers
12. Vehicle identifiers
13. Device identifiers & serial numbers
14. Web URLs
15. IP addresses
16. Biometric identifiers
17. Full-face photographs
18. Any other unique identifying number/characteristic

---

## Entry Points

### Personal Medical Documents
- "Redact my patient record" → `1-documents/personal/patient-records/`
- "Redact my prescription" → `1-documents/personal/prescriptions/`
- "Redact my lab results" → `1-documents/personal/lab-results/`
- "Redact my doctor's note" → `1-documents/personal/clinical-notes/`

### Healthcare Provider Documents
- "Redact patient chart" → `1-documents/provider/patient-charts/`
- "Redact discharge summary" → `1-documents/provider/discharge-summaries/`
- "Redact progress notes" → `1-documents/provider/progress-notes/`

---

## Supported Regulations

- **HIPAA**: Primary focus (Privacy Rule, Security Rule)
- **State Privacy Laws**: Additional protections
- **42 CFR Part 2**: Substance abuse treatment records (extra protection)

---

## Identifier Categories

### Medical (H1-H28)
- `H1-H18`: The 18 HIPAA identifiers
- `H19-H28`: Additional medical context identifiers
  - H19: Diagnoses/Conditions
  - H20: Medications/Dosages
  - H21: Vital Signs
  - H22: Body Locations
  - H23: Family Medical History
  - H24: Mental Health Notes
  - H25: Appointment Details
  - H26: Insurance Claims
  - H27: Provider Names (when linked to patient)
  - H28: Lab Results

---

## Workflow Example: Home Health Nurse

**Before ShieldText**:
```
Text message (HIPAA VIOLATION):
"Mrs. Chen (DOB 3/15/48, MRN 4471829) — left heel wound 
not improving, glucose 289, increased Lantus to 22u. 
She mentioned depression again — need psych referral. 
Daughter Lisa (408-555-1234) wants updates."

Risk: 8 HIPAA identifiers exposed
Penalty: Up to $1.9M per violation category
```

**After ShieldText**:
```
Text message (HIPAA COMPLIANT):
"Patient [REDACTED] — left heel wound not improving, 
glucose elevated, insulin dose adjusted per protocol. 
Patient reports mood concerns — requesting psych referral. 
Family contact requests updates — see care plan for details."

Result: All PHI redacted, clinical meaning preserved
Time: 0.8 seconds on-device
Status: ✅ HIPAA COMPLIANT
```

---

## Architecture

Same 3-layer hybrid as financial:

**Layer 1: Document Skills** (Patient records, prescriptions, etc.)  
**Layer 2: Identifier Skills** (H1-H28 PHI identifiers)  
**Layer 3: Compliance** (HIPAA Privacy Rule validation)

---

## Target Users

### Primary (from your use cases):
- **Home health nurses** (6.4M in US)
- **EMTs/Paramedics** (tactical emergency)
- **Social workers** (CPS investigators)
- **Field clinicians** (rural areas, disaster response)

### Secondary:
- Patients (sharing records with family, second opinions)
- Medical students (case studies)
- Researchers (de-identification for studies)

---

## LLM Workflow

When the LLM receives medical text, follow these steps:

### Step 1: Identify Document Type
Determine which type of medical document this is:
- **Patient Record** → Use `1-documents/personal/patient-records/`
- **Prescription** → Use `1-documents/personal/prescriptions/`
- **Lab Result** → Use `1-documents/personal/lab-results/`
- **Clinical Note** → Use `1-documents/personal/clinical-notes/` (most common)

### Step 2: Detect Identifiers
Find ALL identifiers in the text using:
- **H1-H18** (HIPAA identifiers) → These MUST be redacted
- **H19-H28** (Medical context) → These MUST be preserved

See `2-identifiers/` for detection patterns.

### Step 3: Redact PHI, Preserve Clinical Info
- Replace H1-H18 identifiers with `[REDACTED]` or placeholders
- Keep H19-H28 medical information intact
- Maintain clinical meaning

### Step 4: Validate
Confirm no PHI remains using rules in `3-compliance/HIPAA/VALIDATION.md`

---

## Example: Clinical Note Redaction

**INPUT**:
```
Mrs. Chen (DOB 3/15/48, MRN 4471829) — left heel wound not improving, 
glucose 289, increased Lantus to 22u. She mentioned depression again — 
need psych referral. Daughter Lisa (408-555-1234) wants updates.
```

**LLM DETECTS**:
- H1 (Name): "Mrs. Chen" → REDACT
- H3 (Date): "3/15/48" → REDACT
- H8 (MRN): "4471829" → REDACT
- H1 (Name): "Lisa" (family member when linked to patient) → REDACT
- H4 (Phone): "408-555-1234" → REDACT
- H22 (Body Location): "left heel wound" → PRESERVE
- H28 (Lab Value): "glucose 289" → PRESERVE (can simplify to "elevated")
- H20 (Medication): "Lantus 22u" → PRESERVE (can simplify to "insulin adjusted")
- H24 (Mental Health): "depression" → PRESERVE (becomes "mood concerns")

**OUTPUT**:
```
Patient [REDACTED] — left heel wound not improving, glucose elevated, 
insulin dose adjusted per protocol. Patient reports mood concerns — 
requesting psych referral. Family contact requests updates.
```

**RESULT**: ✅ HIPAA Compliant - All PHI removed, clinical meaning preserved

---

## Available Skills

### Document Types (what to redact/preserve per document)
- `1-documents/personal/patient-records/SKILL.md`
- `1-documents/personal/prescriptions/SKILL.md`
- `1-documents/personal/lab-results/SKILL.md`
- `1-documents/personal/clinical-notes/SKILL.md`

### Identifiers (what to look for)
- `2-identifiers/H1-H18-hipaa-identifiers/SKILL.md` - PHI to redact
- `2-identifiers/H19-H28-medical-context/SKILL.md` - Clinical info to preserve

### Compliance (validation rules)
- `3-compliance/HIPAA/REGULATION.md`
- `3-compliance/HIPAA/IDENTIFIERS.md`
- `3-compliance/HIPAA/VALIDATION.md`
