---
name: medical-context
description: Defines 10 medical context identifiers (H19-H28) that must be PRESERVED during redaction to maintain clinical meaning. Includes diagnoses, medications, vital signs, body locations, family medical history, mental health information, appointment details, insurance claims, provider specialties, and lab results. These are NOT redacted when the patient is de-identified. Use when implementing clinical information preservation, validating that medical meaning is retained, or building redaction logic that distinguishes PHI from clinical data.
---

# Medical Context Identifiers (H19-H28)

## Overview

The **10 medical context identifiers** that must be **PRESERVED** (not redacted) to maintain clinical meaning after patient de-identification.

**Critical Distinction**:
- H1-H18 = **PHI** (Protected Health Information) → **REDACT**
- H19-H28 = **Clinical Information** → **PRESERVE**

**Priority**: ALL are CRITICAL for clinical care continuity  
**Used By**: All medical documents  
**Regulation**: HIPAA permits sharing de-identified health information (45 CFR § 164.514)

---

## Quick Reference

| ID | Identifier | Status | Example | Handling |
|----|-----------|--------|---------|----------|
| H19 | Diagnoses/Conditions | PRESERVE | Type 2 diabetes, hypertension | Keep all |
| H20 | Medications & Dosages | PRESERVE | Metformin 500mg twice daily | Keep all |
| H21 | Vital Signs | PRESERVE | BP 140/90, HR 82, O2 sat 94% | Keep all |
| H22 | Body Locations | PRESERVE | Left heel, right lower quadrant | Keep all |
| H23 | Family Medical History | PRESERVE | Family history of breast cancer | Keep condition, redact family member names |
| H24 | Mental Health Information | PRESERVE | Reports depression, anxiety | Keep clinical info |
| H25 | Appointment Details | PRESERVE | Follow-up in 2 weeks | Keep relative times, redact specific dates |
| H26 | Insurance Claims Data | PRESERVE (PARTIAL) | Prior authorization needed | Keep claim type, redact claim numbers |
| H27 | Provider Specialties | PRESERVE | Cardiologist, endocrinologist | Keep specialty, redact provider names |
| H28 | Lab Result Values | PRESERVE | Glucose 289 mg/dL, HbA1c 7.2% | Keep all values, ranges, flags |

---

## H19: Diagnoses / Conditions

### Definition
Medical diagnoses, health conditions, disease states, and clinical findings.

### Why PRESERVE
This IS the reason for medical care. Removing diagnoses destroys clinical utility.

### Examples

**Chronic Conditions:**
- "Type 2 diabetes mellitus"
- "Congestive heart failure, NYHA class III"
- "Chronic kidney disease, stage 4"
- "COPD with acute exacerbation"

**Acute Conditions:**
- "Left lower lobe pneumonia"
- "Acute myocardial infarction"
- "Cellulitis of right leg"

**Symptoms/Findings:**
- "Left heel wound, stage 3"
- "Shortness of breath on exertion"
- "Abdominal pain, right lower quadrant"

### Redaction Rules

✅ **PRESERVE**: The diagnosis itself
```
"Patient has uncontrolled type 2 diabetes" → Keep entire phrase
```

✅ **REDACT**: Patient-identifying context around it
```
BEFORE: "Mrs. Chen (MRN 123456) has type 2 diabetes"
AFTER: "Patient [REDACTED] (MRN [REDACTED]) has type 2 diabetes"
```

✅ **PRESERVE**: Multiple diagnoses (comorbidities)
```
"Patient has diabetes, hypertension, and hyperlipidemia" → Keep all three
```

### LLM Detection Prompt
```
Identify all diagnoses, medical conditions, symptoms, and clinical findings.
These should be PRESERVED, not redacted.

Examples to detect:
- Named diseases: "diabetes", "pneumonia", "cancer"
- Symptom descriptions: "chest pain", "fever", "nausea"
- Clinical findings: "wound", "rash", "swelling"
- ICD-10 codes: "E11.9 (Type 2 diabetes)"

Return as JSON array:
[
  {"type": "Diagnosis", "value": "Type 2 diabetes", "preserve": true},
  {"type": "Symptom", "value": "left heel wound", "preserve": true}
]
```

---

## H20: Medications & Dosages

### Definition
Medication names (brand and generic), dosages, frequencies, routes of administration, and medication-related instructions.

### Why PRESERVE
Essential for medication reconciliation, preventing drug interactions, and ensuring continuity of care.

### Examples

**Medication Names:**
- Generic: "Metformin", "Lisinopril", "Atorvastatin"
- Brand: "Lantus", "Norvasc", "Lipitor"

**Dosage Information:**
- "500mg twice daily"
- "22 units subcutaneous at bedtime"
- "10mg PO daily"

**Medication Changes:**
- "Increased Lantus from 18u to 22u"
- "Discontinued Hydrochlorothiazide"
- "Started on Lisinopril 10mg"

**Complex Regimens:**
- "Insulin sliding scale: 4u for BG 150-200, 6u for 201-250, 8u for >250"
- "Warfarin dose adjusted based on INR"

### Redaction Rules

✅ **PRESERVE**: All medication details
```
"Metformin 500mg PO BID" → Keep entire phrase
"Lantus 22 units SC qHS" → Keep entire phrase
```

✅ **PRESERVE**: Medication changes (critical for safety)
```
"Increased Lantus to 22u" → Keep
"Stopped Lisinopril due to cough" → Keep
```

❌ **REDACT**: Prescription numbers (those are identifiers)
```
"Rx #123456" → "[REDACTED RX#]"
```

✅ **PRESERVE**: Instructions and warnings
```
"Take with food"
"Do not crush"
"Refrigerate after opening"
"May cause drowsiness"
```

### LLM Detection Prompt
```
Identify all medications, dosages, frequencies, routes, and instructions.
These should be PRESERVED, not redacted.

Examples to detect:
- Drug names: Look for pharmaceutical terms
- Dosages: Numbers + units (mg, mL, units, tablets)
- Frequencies: "twice daily", "BID", "q12h", "as needed"
- Routes: "PO", "IV", "subcutaneous", "topical"
- Changes: "increased", "decreased", "started", "stopped"

Distinguish medication names from patient names:
- "Lantus" = medication (PRESERVE)
- "Lisa" = person name (REDACT if family member)

Return as JSON array:
[
  {"type": "Medication", "value": "Metformin 500mg", "preserve": true},
  {"type": "MedicationChange", "value": "Increased Lantus to 22u", "preserve": true}
]
```

---

## H21: Vital Signs

### Definition
Physiological measurements: blood pressure, heart rate, temperature, respiratory rate, oxygen saturation, pain level, weight, height.

### Why PRESERVE
Core clinical data for assessment, trending, and treatment decisions.

### Examples

**Standard Vital Signs:**
- "BP 140/90 mmHg"
- "HR 82 bpm"
- "Temp 98.6°F (37°C)"
- "RR 18 breaths/min"
- "O2 sat 94% on room air"

**Pain Assessment:**
- "Pain 7/10"
- "Reports increasing pain in left knee"

**Body Measurements:**
- "Weight 180 lbs (81.6 kg)"
- "Height 5'8\" (172 cm)"
- "BMI 27.4"

**Trending:**
- "BP improved from 160/95 to 140/90 since last visit"

### Redaction Rules

✅ **PRESERVE**: All vital sign values and units
```
"BP 140/90, HR 82, Temp 98.6°F" → Keep all
```

✅ **PRESERVE**: Contextual qualifiers
```
"O2 sat 94% on room air" → Keep
"Glucose 289 mg/dL [HIGH]" → Keep (the HIGH flag is clinical)
```

✅ **PRESERVE**: Trends
```
"Weight down 10 lbs since last month" → Keep
```

### LLM Detection Prompt
```
Identify all vital signs and physiological measurements.
These should be PRESERVED, not redacted.

Vital signs to detect:
- Blood pressure (BP): Systolic/diastolic, units
- Heart rate (HR, pulse): Beats per minute
- Temperature (Temp, T): Fahrenheit or Celsius
- Respiratory rate (RR): Breaths per minute
- Oxygen saturation (O2 sat, SpO2): Percentage
- Pain level: Numeric scale (e.g., 7/10)
- Weight, height, BMI

Return as JSON array:
[
  {"type": "VitalSign", "value": "BP 140/90 mmHg", "preserve": true},
  {"type": "VitalSign", "value": "Pain 7/10", "preserve": true}
]
```

---

## H22: Body Locations / Anatomical References

### Definition
Body parts, anatomical locations, laterality (left/right), anatomical regions.

### Why PRESERVE
Essential for localizing conditions, wounds, pain, and surgical sites.

### Examples

**Specific Body Parts:**
- "Left heel"
- "Right lower quadrant"
- "C5-C6 vertebrae"
- "Left upper outer breast quadrant"

**Anatomical Regions:**
- "Abdomen"
- "Lower extremities"
- "Chest"

**Directional Terms:**
- "Anterior", "Posterior", "Medial", "Lateral"
- "Proximal", "Distal"

**Surgical Sites:**
- "Left total knee replacement"
- "Right inguinal hernia repair site"

### Redaction Rules

✅ **PRESERVE**: All body location references
```
"Left heel wound 3cm x 2cm" → Keep
"Right lower quadrant tenderness" → Keep
"Breath sounds decreased in right base" → Keep
```

✅ **DISTINGUISH from Geographic Locations** (which ARE PHI)
```
"Left heel" → Body part (PRESERVE)
"Springfield, IL" → City (REDACT)
"Room 3" → Geographic location in facility (REDACT)
```

### LLM Detection Prompt
```
Identify all body locations and anatomical references.
These should be PRESERVED, not redacted.

Body locations to detect:
- Specific body parts: "left heel", "right knee", "abdomen"
- Laterality: "left", "right", "bilateral"
- Anatomical regions: "upper quadrant", "lower extremity"
- Vertebral levels: "C5-C6", "L4-L5"

CRITICAL: Distinguish body locations from geographic locations:
- "Left heel" = body location (PRESERVE)
- "Left wing of hospital" = geographic (REDACT)
- "Springfield" = city (REDACT)
- "Right lower quadrant" = abdomen (PRESERVE)

Return as JSON array:
[
  {"type": "BodyLocation", "value": "left heel", "preserve": true},
  {"type": "BodyLocation", "value": "right lower quadrant", "preserve": true}
]
```

---

## H23: Family Medical History

### Definition
Medical conditions in family members (genetic risk factors, hereditary conditions).

### Why PRESERVE
Important for risk assessment, screening decisions, and genetic counseling.

### Examples

**Family History:**
- "Mother had breast cancer at age 45"
- "Father died of heart attack at 60"
- "Sister has type 1 diabetes"
- "Family history of colon cancer"

### Redaction Rules

✅ **PRESERVE**: The medical condition and relationship type
```
"Family history of breast cancer" → Keep
"Mother had heart disease" → Keep "Mother" (relationship) and "heart disease" (condition)
```

❌ **REDACT**: Family member's NAME if given
```
BEFORE: "Mother Susan had breast cancer"
AFTER: "Mother [REDACTED NAME] had breast cancer"
OR
AFTER: "Family history of breast cancer (maternal)"
```

✅ **BEST PRACTICE**: Generalize to preserve privacy while keeping clinical value
```
BEFORE: "Sister Lisa has type 1 diabetes"
AFTER: "Family history of type 1 diabetes (sibling)"
```

### LLM Detection Prompt
```
Identify family medical history information.
PRESERVE the relationship and condition.
REDACT specific family member names.

Examples:
- "Mother had breast cancer" → PRESERVE relationship ("Mother") and condition ("breast cancer")
- "Sister Lisa has diabetes" → REDACT name ("Lisa"), PRESERVE relationship and condition

Return as JSON array:
[
  {"type": "FamilyHistory", "relationship": "Mother", "condition": "breast cancer", 
   "preserve": true, "redact_name": true},
  {"type": "FamilyHistory", "value": "Family history of diabetes", "preserve": true}
]
```

---

## H24: Mental Health Information

### Definition
Psychiatric diagnoses, psychological symptoms, substance use, behavioral health notes, mental status exam findings.

### Why PRESERVE
Essential clinical information for comprehensive care. Mental health = health.

### Examples

**Psychiatric Diagnoses:**
- "Major depressive disorder"
- "Generalized anxiety disorder"
- "PTSD"
- "Bipolar disorder, type II"

**Symptoms:**
- "Reports depression"
- "Patient mentioned depression again"
- "Suicidal ideation"
- "Anxiety worsening"

**Substance Use:**
- "Alcohol use disorder"
- "Reports drinking 6 beers prior to call"
- "History of opioid use disorder, currently in recovery"

**Mental Status Exam:**
- "Alert and oriented x3"
- "Affect flat"
- "Thought process linear"

### Redaction Rules

✅ **PRESERVE**: All mental health clinical information
```
"Patient reports depression and anxiety" → Keep
"Suicidal ideation with plan" → Keep (critical for safety)
"History of substance abuse" → Keep
```

✅ **PRESERVE**: Substance use (it's a medical condition)
```
"Reports drinking 6 beers" → Keep
"Positive urine drug screen for THC" → Keep
```

❌ **REDACT**: Specific substance abuse treatment facility names/locations
```
"Attends AA meetings at St. Mary's Church" → "Attends AA meetings at [REDACTED LOCATION]"
```

**IMPORTANT**: Mental health information IS PHI when linked to an identified patient. Once patient is de-identified (name, DOB, MRN removed), the mental health condition becomes non-identifying clinical information that should be preserved for care continuity.

### Special Regulation: 42 CFR Part 2
Substance abuse treatment records have EXTRA federal protections beyond HIPAA. ShieldText redacts PHI to de-identify, but users should be aware that substance abuse records may require explicit consent even when de-identified in some cases.

### LLM Detection Prompt
```
Identify all mental health and behavioral health information.
These should be PRESERVED, not redacted (once patient is de-identified).

Mental health information to detect:
- Psychiatric diagnoses: "depression", "anxiety", "PTSD", "bipolar"
- Psychological symptoms: "suicidal ideation", "panic attacks"
- Substance use: "alcohol use", "drug use", specific substances
- Mental status exam: "alert", "oriented", "affect", "thought process"
- Therapy/treatment: "CBT", "psychiatric medication"

Return as JSON array:
[
  {"type": "MentalHealth", "value": "reports depression", "preserve": true},
  {"type": "SubstanceUse", "value": "alcohol use disorder", "preserve": true}
]
```

---

## H25: Appointment Details

### Definition
Scheduling information, follow-up plans, appointment frequency.

### Why PRESERVE
Necessary for care coordination and treatment plan adherence.

### Examples

**Follow-up Plans:**
- "Follow-up in 2 weeks"
- "Return to clinic if symptoms worsen"
- "Next PT session in 3 days"

**Appointment Frequency:**
- "Continue PT 2x/week for 4 weeks"
- "Monthly medication check"

### Redaction Rules

✅ **PRESERVE**: Relative timing and frequency
```
"Follow-up in 2 weeks" → Keep
"Monthly visits" → Keep
```

❌ **REDACT**: Specific appointment dates/times
```
"Appointment on 04/15/2024 at 2:00 PM" → "Appointment on [REDACTED DATE]"
OR
"Next appointment in 2 weeks" (convert to relative)
```

✅ **PRESERVE**: Appointment type and purpose
```
"Schedule follow-up with endocrinologist for glucose management" → Keep
```

### LLM Detection Prompt
```
Identify appointment details.
PRESERVE relative timing and purpose.
REDACT specific dates/times.

Examples:
- "Follow-up in 2 weeks" → PRESERVE
- "Appointment on 04/15 at 2pm" → REDACT specific date/time
- "Monthly PT sessions" → PRESERVE

Return as JSON array:
[
  {"type": "AppointmentDetail", "value": "Follow-up in 2 weeks", "preserve": true},
  {"type": "AppointmentDate", "value": "04/15/2024", "preserve": false, "redact": true}
]
```

---

## H26: Insurance Claims Data (Partial Preservation)

### Definition
Insurance-related information: coverage type, prior authorizations, billing codes, claim status.

### Why PRESERVE (PARTIAL)
Relevant for care coordination (e.g., knowing if prior auth is needed).

### Examples

**Preserve (Clinical Relevance):**
- "Prior authorization required"
- "Medicare coverage"
- "Requires pre-certification"

**Redact (Identifiers):**
- "Insurance ID: ABC123456" → "[REDACTED INSURANCE ID]"
- "Claim #789456" → "[REDACTED CLAIM#]"
- "Policy holder: [Patient name]" → "[REDACTED]"

### Redaction Rules

✅ **PRESERVE**: Insurance type and authorization requirements
```
"Prior authorization needed for MRI" → Keep
"Medicare Part B covers this service" → Keep
```

❌ **REDACT**: Insurance IDs, claim numbers, policy numbers
```
"Insurance ID: ABC123" → "[REDACTED INSURANCE ID]"
```

### LLM Detection Prompt
```
Identify insurance/claims information.
PRESERVE types and requirements.
REDACT ID numbers.

Examples:
- "Prior authorization required" → PRESERVE
- "Insurance ID: ABC123" → REDACT
- "Medicare coverage" → PRESERVE
- "Claim #456789" → REDACT

Return as JSON array:
[
  {"type": "InsuranceType", "value": "Medicare", "preserve": true},
  {"type": "InsuranceID", "value": "ABC123", "preserve": false, "redact": true}
]
```

---

## H27: Provider Specialties (Not Names)

### Definition
Medical specialties, provider types, roles in care team.

### Why PRESERVE
Important for care coordination and referral management.

### Examples

**Preserve (Specialty):**
- "Endocrinologist"
- "Cardiologist"
- "Physical therapist"
- "Consulting psychiatrist"

**Redact (Provider Name):**
- "Dr. Lisa Chen" → "[REDACTED PROVIDER]"
- "Referred to Dr. Smith" → "Referred to [REDACTED PROVIDER]"

### Redaction Rules

✅ **PRESERVE**: Specialty and role
```
"Consulted endocrinology for glucose management" → Keep
"Physical therapist recommended exercises" → Keep
```

❌ **REDACT**: Provider's personal name
```
BEFORE: "Dr. Lisa Chen, Endocrinology"
AFTER: "[REDACTED PROVIDER], Endocrinology"
OR
AFTER: "Endocrinologist"
```

✅ **BEST PRACTICE**: Use specialty instead of name
```
"Referred to Dr. Johnson" → "Referred to cardiologist"
"Called Dr. Smith's office" → "Contacted prescribing provider"
```

### LLM Detection Prompt
```
Identify provider information.
PRESERVE specialty/role.
REDACT personal names.

Examples:
- "Dr. Lisa Chen, Endocrinology" → REDACT name ("Lisa Chen"), PRESERVE specialty ("Endocrinology")
- "Cardiologist recommended" → PRESERVE
- "Referred to psychiatrist" → PRESERVE

Return as JSON array:
[
  {"type": "ProviderSpecialty", "value": "Endocrinology", "preserve": true},
  {"type": "ProviderName", "value": "Dr. Lisa Chen", "preserve": false, "redact": true}
]
```

---

## H28: Lab Result Values

### Definition
Laboratory test results, values, units, reference ranges, abnormal flags.

### Why PRESERVE
Core diagnostic and monitoring data. Essential for clinical decisions.

### Examples

**Lab Values:**
- "Glucose 289 mg/dL"
- "Hemoglobin A1c 7.2%"
- "WBC 12.5 K/uL"
- "Creatinine 1.8 mg/dL"

**Reference Ranges:**
- "Normal: 70-100 mg/dL"
- "Reference range: 4.5-11.0 K/uL"

**Abnormal Flags:**
- "[HIGH]", "[LOW]", "[CRITICAL]"
- "H", "L", "*"

**Interpretations:**
- "Elevated glucose consistent with uncontrolled diabetes"
- "Microcytic anemia"

### Redaction Rules

✅ **PRESERVE**: All lab values, units, ranges, flags
```
"Glucose 289 mg/dL [HIGH], reference 70-100" → Keep all
```

✅ **PRESERVE**: Interpretations
```
"Results consistent with iron deficiency anemia" → Keep
```

❌ **REDACT**: Lab accession numbers, specimen IDs
```
"Accession #L2024-089234" → "[REDACTED ACCESSION#]"
```

### LLM Detection Prompt
```
Identify laboratory result values.
These should be PRESERVED, not redacted.

Lab results to detect:
- Test names: "Glucose", "Hemoglobin", "Creatinine"
- Numeric values + units: "289 mg/dL", "7.2%", "12.5 K/uL"
- Reference ranges: "70-100 mg/dL", "Normal: <5.7%"
- Abnormal flags: "[HIGH]", "[LOW]", "H", "L", "*"
- Interpretations: Clinical statements about results

DISTINGUISH from:
- Accession numbers: "L2024-089234" (REDACT - this is an identifier)
- Lab facility name: "LabCorp" (REDACT)

Return as JSON array:
[
  {"type": "LabResult", "test": "Glucose", "value": "289 mg/dL", 
   "flag": "HIGH", "preserve": true},
  {"type": "ReferenceRange", "value": "70-100 mg/dL", "preserve": true}
]
```

---

## Summary Table: H1-H18 vs H19-H28

| Category | IDs | Status | Examples |
|----------|-----|--------|----------|
| **HIPAA Identifiers (PHI)** | H1-H18 | **REDACT** | Names, DOB, MRN, SSN, Addresses, Phones, Dates |
| **Medical Context** | H19-H28 | **PRESERVE** | Diagnoses, Medications, Vitals, Lab Results |

---

## Android Implementation

### Data Model
```kotlin
data class MedicalContextItem(
    val type: MedicalContextType,
    val value: String,
    val shouldPreserve: Boolean = true  // Always true for H19-H28
)

enum class MedicalContextType {
    DIAGNOSIS,          // H19
    MEDICATION,         // H20
    VITAL_SIGN,         // H21
    BODY_LOCATION,      // H22
    FAMILY_HISTORY,     // H23
    MENTAL_HEALTH,      // H24
    APPOINTMENT_DETAIL, // H25
    INSURANCE_INFO,     // H26
    PROVIDER_SPECIALTY, // H27
    LAB_RESULT          // H28
}
```

### LLM Prompt for Detection
```kotlin
fun detectMedicalContext(text: String): List<MedicalContextItem> {
    val prompt = """
    Identify all clinical information in this text that should be PRESERVED 
    (not redacted) for medical care continuity:
    
    Text: "$text"
    
    Detect:
    - Diagnoses/conditions
    - Medications and dosages
    - Vital signs
    - Body locations
    - Family medical history
    - Mental health information
    - Appointment details
    - Provider specialties
    - Lab results
    
    Return as JSON array with type and value.
    """.trimIndent()
    
    val output = llmInference.run(prompt)
    return parseJSON(output)
}
```

### UI: Show Preserved Items
```kotlin
// In redaction result screen, highlight preserved clinical info in GREEN
fun displayRedactedText(redacted: String, preserved: List<MedicalContextItem>) {
    textView.text = redacted
    
    preserved.forEach { item ->
        val startIndex = redacted.indexOf(item.value)
        if (startIndex != -1) {
            textView.setSpan(
                BackgroundColorSpan(Color.GREEN.withAlpha(0.3)),
                startIndex,
                startIndex + item.value.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
}
```

---

## Status

**Phase**: 2 - Identifier Category Complete
- ✅ All 10 medical context identifiers defined (H19-H28)
- ✅ Preservation rules documented
- ✅ LLM detection prompts provided
- ✅ Android implementation guidance included

**Dependencies**: None (this is a foundational skill)

**Used By**: All medical document skills (patient-records, prescriptions, lab-results, clinical-notes)

**Next**: Update master medical-redaction SKILL.md with completion status
