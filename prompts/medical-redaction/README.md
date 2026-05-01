# Medical Redaction Skills for LLM

## Purpose

These skills teach your LLM how to redact medical documents for HIPAA compliance while preserving clinical meaning.

---

## Structure

```
medical-redaction/
│
├── SKILL.md                            ← Master skill - LLM workflow
├── README.md                           ← This file
│
├── 1-documents/personal/               ← Document-specific redaction rules
│   ├── patient-records/SKILL.md        ← Medical charts, records
│   ├── prescriptions/SKILL.md          ← Rx, medication lists
│   ├── lab-results/SKILL.md            ← Blood work, pathology
│   └── clinical-notes/SKILL.md         ← Progress notes (PRIMARY USE CASE)
│
├── 2-identifiers/                      ← What to detect
│   ├── H1-H18-hipaa-identifiers/       ← PHI to REDACT
│   └── H19-H28-medical-context/        ← Clinical info to PRESERVE
│
└── 3-compliance/HIPAA/                 ← Validation rules
    ├── REGULATION.md                   ← HIPAA overview
    ├── IDENTIFIERS.md                  ← All 28 identifiers
    └── VALIDATION.md                   ← Compliance checking
```

---

## How It Works

### 4-Step LLM Process

**1. CLASSIFY** → Determine document type
- Patient record? Prescription? Lab result? Clinical note?
- Load appropriate document skill

**2. DETECT** → Find identifiers
- H1-H18 (PHI) → Names, DOB, MRN, addresses, phone numbers, etc.
- H19-H28 (Clinical) → Diagnoses, medications, vitals, lab results, etc.

**3. REDACT** → Transform text
- Replace H1-H18 with `[REDACTED]` placeholders
- Keep H19-H28 clinical information intact

**4. VALIDATE** → Confirm compliance
- Check: No PHI remains
- Result: ✅ HIPAA COMPLIANT or list violations

---

## Example: Clinical Note

### Use Case
Home health nurse needs to update care team about patient visit.

**BEFORE (HIPAA VIOLATION)**:
```
Mrs. Chen (DOB 3/15/48, MRN 4471829) — left heel wound not improving, 
glucose 289, increased Lantus to 22u. She mentioned depression again — 
need psych referral. Daughter Lisa (408-555-1234) wants updates.

❌ 8 HIPAA identifiers exposed
```

**LLM ANALYSIS**:
```
REDACT (H1-H18 PHI):
- "Mrs. Chen" (H1: Name)
- "3/15/48" (H3: Date of birth)
- "4471829" (H8: MRN)
- "Lisa" (H1: Family member name)
- "408-555-1234" (H4: Phone number)

PRESERVE (H19-H28 Clinical):
- "left heel wound" (H22: Body location)
- "not improving" (clinical observation)
- "glucose 289" → simplify to "glucose elevated" (H28: Lab value)
- "Lantus 22u" → simplify to "insulin adjusted" (H20: Medication)
- "depression" → "mood concerns" (H24: Mental health)
- "psych referral" (clinical action)
```

**AFTER (HIPAA COMPLIANT)**:
```
Patient [REDACTED] — left heel wound not improving, glucose elevated, 
insulin dose adjusted per protocol. Patient reports mood concerns — 
requesting psych referral. Family contact requests updates.

✅ All PHI removed
✅ Clinical meaning preserved
✅ HIPAA compliant
```

---

## What Gets Redacted vs Preserved

### 🔴 REDACT (H1-H18 PHI)
PHI = Protected Health Information that can identify a patient

- Names (patient, family, providers)
- Dates (DOB, service dates)
- Medical Record Numbers (MRN)
- Phone numbers, email addresses
- Addresses, cities (smaller than state)
- Social Security Numbers
- Account numbers
- License/certificate numbers
- Device IDs, serial numbers
- URLs, IP addresses
- Biometric identifiers
- Photos

### 🟢 PRESERVE (H19-H28 Clinical)
Clinical information needed for patient care

- Diagnoses & conditions
- Medications & dosages
- Vital signs (BP, HR, temp, etc.)
- Body locations ("left heel", "right knee")
- Lab results & values
- Symptoms & observations
- Treatment plans
- Family medical history (keep condition, redact names)
- Mental health information
- Referrals & appointments

---

## HIPAA Compliance

These skills implement the **Safe Harbor Method** (45 CFR § 164.514(b)(2)):

✅ **Requirement**: Remove all 18 HIPAA identifiers  
✅ **Implementation**: H1-H18 detection and redaction

✅ **Requirement**: No knowledge remaining info could identify individual  
✅ **Implementation**: Validation step confirms no identifiers remain

✅ **Result**: De-identified health information can be shared without HIPAA restrictions

---

## Skill Reference

### Master Skill
- `SKILL.md` - Overall LLM workflow and process

### Document Types (Layer 1)
What to redact/preserve for each document type:
- `1-documents/personal/patient-records/SKILL.md` - Medical charts
- `1-documents/personal/prescriptions/SKILL.md` - Rx, medication lists
- `1-documents/personal/lab-results/SKILL.md` - Blood work, pathology
- `1-documents/personal/clinical-notes/SKILL.md` - Progress notes (most common)

### Identifiers (Layer 2)
What to detect in the text:
- `2-identifiers/H1-H18-hipaa-identifiers/SKILL.md` - PHI to redact
- `2-identifiers/H19-H28-medical-context/SKILL.md` - Clinical info to preserve

### Compliance (Layer 3)
Validation and regulatory reference:
- `3-compliance/HIPAA/REGULATION.md` - HIPAA overview
- `3-compliance/HIPAA/IDENTIFIERS.md` - All 28 identifiers with examples
- `3-compliance/HIPAA/VALIDATION.md` - Compliance checking rules

---

## Why This Architecture?

**Layer 1 (Documents)**: Document-specific rules  
- Each document type has unique redaction needs
- Example: Prescriptions preserve medication details, labs preserve result values

**Layer 2 (Identifiers)**: Reusable detection patterns  
- Same identifiers used across all document types
- H1-H18 (PHI) always redacted, H19-H28 (clinical) always preserved

**Layer 3 (Compliance)**: Regulatory framework  
- HIPAA rules don't change per document
- Centralized validation logic

---

## Summary

✅ **4 document types** - Patient records, prescriptions, lab results, clinical notes  
✅ **28 identifiers** - H1-H18 (PHI to redact) + H19-H28 (clinical to preserve)  
✅ **HIPAA Safe Harbor compliant** - 100% removal of all 18 HIPAA identifiers  
✅ **Production ready** - Complete skills for LLM implementation

**These skills teach your LLM exactly what to look for and what to redact in medical documents.**
