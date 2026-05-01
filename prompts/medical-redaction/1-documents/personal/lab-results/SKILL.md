---
name: lab-results
description: Redacts laboratory test results for safe sharing with specialists, second-opinion doctors, or care teams ensuring HIPAA compliance. Removes patient names, DOB, MRN, addresses, ordering provider details, and lab identifiers while preserving test names, result values, reference ranges, and abnormal flags. Use when processing lab reports, blood work results, diagnostic test results, pathology reports, or when the user mentions lab results, blood tests, lab work, or diagnostic reports.
---

# Lab Results Redaction

## Purpose

Redact laboratory test results and diagnostic reports for safe sharing with specialists, second-opinion doctors, or care coordinators while maintaining HIPAA compliance.

**Use Case**: Home health nurse photographing lab printout to share glucose levels with endocrinologist, patient sharing blood work with new doctor for second opinion.

---

## Document Types

### Blood Work / Chemistry Panels
- Complete Blood Count (CBC)
- Comprehensive Metabolic Panel (CMP)
- Basic Metabolic Panel (BMP)
- Lipid Panel
- Liver Function Tests (LFTs)
- Thyroid Panel (TSH, T3, T4)

### Specialized Lab Tests
- HbA1c (diabetes monitoring)
- INR/PT (blood thinning monitoring)
- Vitamin levels (D, B12, etc.)
- Hormone levels
- Tumor markers (PSA, CA-125, etc.)

### Microbiology
- Culture results (blood, urine, wound)
- Sensitivity reports (antibiotic resistance)
- COVID-19 / Infectious disease tests

### Pathology Reports
- Biopsy results
- Cytology reports
- Surgical pathology

### Imaging Reports (Text)
- X-ray reports
- CT/MRI scan reports
- Ultrasound reports

---

## What Gets Redacted

### HIPAA Identifiers (18 - All Critical)

#### Patient Information
1. ✅ **Patient Name** → `[REDACTED NAME]` or `Patient [REDACTED]`
2. ✅ **Date of Birth** → `[REDACTED DOB]` or age/year only
3. ✅ **Address** → `[REDACTED ADDRESS]`
4. ✅ **Phone Number** → `[REDACTED PHONE]`
5. ✅ **Email** → `[REDACTED EMAIL]`
6. ✅ **SSN** → `[REDACTED SSN]`
7. ✅ **MRN** → `[REDACTED MRN]`

#### Lab-Specific Identifiers
8. ✅ **Accession Number** (Lab specimen ID) → `[REDACTED ACCESSION#]`
   - Example: Accession #: L2024-456789

9. ✅ **Specimen Number** → `[REDACTED SPECIMEN#]`

10. ✅ **Ordering Provider Name** → `[REDACTED PROVIDER]` or "Ordering physician"
    - Dr. Lisa Chen → `[REDACTED PROVIDER]`

11. ✅ **Provider NPI** → `[REDACTED NPI]`

12. ✅ **Lab/Facility Name & Address** → `[REDACTED LAB]` or generic "Laboratory"

13. ✅ **Lab Phone/Fax** → `[REDACTED PHONE]`

14. ✅ **Dates**
    - Collection date: `Collected [REDACTED]` or relative time "3 days ago"
    - Received date: `Received [REDACTED]`
    - Result date: `Resulted [REDACTED]`
    - Report date: `Reported [REDACTED]`
    - Exception: Keep if showing trend over time (see below)

---

## What Gets PRESERVED

### Critical Clinical Information

✅ **Test Names** (always preserve)
- Example: "Glucose, serum", "Hemoglobin A1c", "TSH"

✅ **Result Values** (always preserve)
- Example: "289 mg/dL", "7.2%", "150 U/L"

✅ **Units of Measurement**
- Example: "mg/dL", "mmol/L", "IU/mL", "%"

✅ **Reference Ranges** (normal ranges)
- Example: "Normal: 70-100 mg/dL"

✅ **Abnormal Flags**
- Example: "HIGH", "LOW", "CRITICAL", "*", "H", "L"

✅ **Interpretation/Comments** (clinical significance)
- Example: "Consistent with iron deficiency anemia"
- Example: "Urgent: Notify provider immediately"

✅ **Test Method** (if relevant)
- Example: "Method: Immunoassay", "ELISA"

✅ **Specimen Type**
- Example: "Serum", "Whole blood", "Urine"

✅ **Fasting Status** (clinical relevance)
- Example: "Fasting: Yes" or "Non-fasting"

---

## Date Handling (Special Rule for Labs)

### Option 1: Redact All Dates (Safest)
```
Collection Date: [REDACTED]
Result Date: [REDACTED]
```

### Option 2: Relative Dates (Preserve Trend Context)
```
Collection Date: 3 days ago
Previous test: 3 months prior
```

### Option 3: Date Ranges for Trends (HIPAA-compliant if >89 days apart)
Keep dates if showing long-term trends (e.g., HbA1c every 3 months)
```
HbA1c Trend:
- Q1 2024: 7.2%
- Q4 2023: 7.8%
- Q3 2023: 8.1%
```

**Recommendation**: Use Option 1 (redact all) unless user specifically needs trend data.

---

## Example Redactions

### Example 1: Blood Glucose Test

**BEFORE:**
```
LabCorp
123 Medical Plaza, Springfield, IL 62701
Phone: (217) 555-0199

PATIENT: Sarah Johnson
DOB: 04/15/1982 | Age: 42
MRN: 4471829
Accession #: L2024-089234

Ordering Provider: Dr. Amanda Chen, Endocrinology
NPI: 1234567890

Collection Date: 03/20/2024 08:15 AM
Received: 03/20/2024 09:30 AM
Reported: 03/20/2024 14:45 PM

TEST: Glucose, Fasting
Result: 289 mg/dL      [HIGH]
Reference Range: 70-100 mg/dL
Fasting: Yes

TEST: Hemoglobin A1c
Result: 7.2%           [HIGH]
Reference Range: <5.7% (normal)
                 5.7-6.4% (prediabetes)
                 ≥6.5% (diabetes)

INTERPRETATION:
Elevated glucose and HbA1c consistent with uncontrolled
diabetes. Recommend medication adjustment.
```

**AFTER:**
```
[REDACTED LAB]

PATIENT: [REDACTED NAME]
DOB: [REDACTED] | Age: [REDACTED]
MRN: [REDACTED]
Accession #: [REDACTED]

Ordering Provider: [REDACTED PROVIDER], Endocrinology

Collection Date: [REDACTED]
Received: [REDACTED]
Reported: [REDACTED]

TEST: Glucose, Fasting
Result: 289 mg/dL      [HIGH]
Reference Range: 70-100 mg/dL
Fasting: Yes

TEST: Hemoglobin A1c
Result: 7.2%           [HIGH]
Reference Range: <5.7% (normal)
                 5.7-6.4% (prediabetes)
                 ≥6.5% (diabetes)

INTERPRETATION:
Elevated glucose and HbA1c consistent with uncontrolled
diabetes. Recommend medication adjustment.
```

---

### Example 2: Complete Blood Count (CBC)

**BEFORE:**
```
Quest Diagnostics
Patient: Robert Martinez | MRN: 8847291
DOB: 11/22/1956
Collected: 04/18/2024 | Resulted: 04/18/2024

COMPLETE BLOOD COUNT (CBC)
Specimen: Whole blood (EDTA tube)

Test Name          Result    Flag    Reference Range
---------------------------------------------------
WBC               12.5       H       4.5-11.0 K/uL
RBC                4.2               4.5-5.9 M/uL
Hemoglobin         9.8       L       13.5-17.5 g/dL
Hematocrit        29.4       L       39-49%
MCV               70.0       L       80-100 fL
MCH               23.3       L       27-33 pg
MCHC              33.3               32-36 g/dL
Platelets          350               150-400 K/uL
Neutrophils       68%                40-70%
Lymphocytes       24%                20-40%
Monocytes          6%                2-10%
Eosinophils        2%                0-5%

INTERPRETATION:
Microcytic anemia with elevated WBC. Consider iron studies
and infection workup.
```

**AFTER:**
```
[REDACTED LAB]
Patient: [REDACTED NAME] | MRN: [REDACTED]
DOB: [REDACTED]
Collected: [REDACTED] | Resulted: [REDACTED]

COMPLETE BLOOD COUNT (CBC)
Specimen: Whole blood (EDTA tube)

Test Name          Result    Flag    Reference Range
---------------------------------------------------
WBC               12.5       H       4.5-11.0 K/uL
RBC                4.2               4.5-5.9 M/uL
Hemoglobin         9.8       L       13.5-17.5 g/dL
Hematocrit        29.4       L       39-49%
MCV               70.0       L       80-100 fL
MCH               23.3       L       27-33 pg
MCHC              33.3               32-36 g/dL
Platelets          350               150-400 K/uL
Neutrophils       68%                40-70%
Lymphocytes       24%                20-40%
Monocytes          6%                2-10%
Eosinophils        2%                0-5%

INTERPRETATION:
Microcytic anemia with elevated WBC. Consider iron studies
and infection workup.
```

---

### Example 3: Pathology Report (Biopsy)

**BEFORE:**
```
Springfield Pathology Associates
123 Hospital Drive, Springfield, IL 62701

SURGICAL PATHOLOGY REPORT

Patient: Jennifer Adams | MRN: 9912847
DOB: 06/30/1975 | Age: 48
Accession: S24-4729

Physician: Dr. Emily Roberts, General Surgery
Collected: 04/10/2024 | Reported: 04/12/2024

CLINICAL HISTORY:
48-year-old female with palpable left breast mass, 2cm,
upper outer quadrant. Family history of breast cancer.

SPECIMEN: Left breast, core needle biopsy

GROSS DESCRIPTION:
Three fragments of tan-pink tissue, 0.5-1.0cm each

MICROSCOPIC DESCRIPTION:
Sections show infiltrating ductal carcinoma, grade 2/3
(tubule formation 3/3, nuclear pleomorphism 2/3,
mitotic rate 2/3). Estrogen receptor: Positive (90%)
Progesterone receptor: Positive (70%)
HER2: Negative (IHC 1+)
Ki-67: 20%

DIAGNOSIS:
Infiltrating ductal carcinoma, grade 2
Left breast, core needle biopsy
```

**AFTER:**
```
[REDACTED LAB]

SURGICAL PATHOLOGY REPORT

Patient: [REDACTED NAME] | MRN: [REDACTED]
DOB: [REDACTED] | Age: [REDACTED]
Accession: [REDACTED]

Physician: [REDACTED PROVIDER], General Surgery
Collected: [REDACTED] | Reported: [REDACTED]

CLINICAL HISTORY:
Patient with palpable left breast mass, 2cm,
upper outer quadrant. Family history of breast cancer.

SPECIMEN: Left breast, core needle biopsy

GROSS DESCRIPTION:
Three fragments of tan-pink tissue, 0.5-1.0cm each

MICROSCOPIC DESCRIPTION:
Sections show infiltrating ductal carcinoma, grade 2/3
(tubule formation 3/3, nuclear pleomorphism 2/3,
mitotic rate 2/3). Estrogen receptor: Positive (90%)
Progesterone receptor: Positive (70%)
HER2: Negative (IHC 1+)
Ki-67: 20%

DIAGNOSIS:
Infiltrating ductal carcinoma, grade 2
Left breast, core needle biopsy
```

**Note**: Age and clinical history (body location, family history) are preserved because they provide essential clinical context. Patient is de-identified by name/DOB/MRN removal.

---

## Required Identifier Categories

Uses these identifier skills:

### From H1-H18 (HIPAA Identifiers)
- H1: Names (patient, provider)
- H2: Addresses (lab facility)
- H3: Dates (collection, result, report dates)
- H4: Phone numbers (lab)
- H5: Fax numbers
- H7: SSN (rare)
- H8: MRN
- H10: Account numbers (accession #, specimen #)
- H11: License numbers (NPI)

### From H19-H28 (Medical Context)
- H28: Lab result values (**PRESERVE**)
- H19: Diagnoses/conditions (if in interpretation) (**PRESERVE**)
- H22: Body locations (specimen site) (**PRESERVE**)

---

## Processing Workflow

```
1. OCR EXTRACTION (if photo/scanned)
   └─> Extract text from lab report using Google ML Kit
   └─> Challenge: Tables/columns require careful parsing
   
2. DOCUMENT CLASSIFICATION
   └─> Identify as "lab-result" document type
   └─> Sub-classify: blood work, pathology, imaging report
   
3. PHI DETECTION
   ├─> Find patient identifiers (name, DOB, MRN)
   ├─> Find provider identifiers (ordering physician, NPI)
   ├─> Find lab identifiers (accession #, specimen #)
   ├─> Find facility details (lab name, address, phone)
   └─> Find dates (collection, result)
   
4. CLINICAL INFORMATION PRESERVATION
   ├─> Identify test names
   ├─> Identify result values and units
   ├─> Identify reference ranges
   ├─> Identify abnormal flags
   ├─> Identify clinical interpretations
   └─> Mark ALL as PRESERVE
   
5. REDACTION
   ├─> Replace all PHI with placeholders
   └─> Keep all test results and clinical data intact
   
6. VALIDATION
   ├─> Confirm no patient/provider names remain
   ├─> Confirm no MRN, accession numbers remain
   ├─> Confirm all test results are intact
   ├─> Confirm reference ranges are intact
   └─> Return compliance status
```

---

## OCR Challenges for Lab Reports

### Table Formatting
- Lab results are often in tabular format
- OCR may break column alignment
- **Solution**: Train LLM to understand "Test | Result | Flag | Range" patterns

### Small Font Sizes
- Reference ranges often in smaller font
- Units may be superscript/subscript
- **Solution**: Use high-resolution photos, zoom on key sections

### Multiple Pages
- Comprehensive panels span multiple pages
- **Solution**: Process page-by-page or concatenate before redaction

---

## Compliance Requirements

### HIPAA Privacy Rule
- ✅ 100% removal of all 18 identifiers
- ✅ Lab result values (non-identifying) can be shared

### CLIA (Clinical Laboratory Improvement Amendments)
- Lab must be CLIA-certified to generate results
- Redaction doesn't affect CLIA compliance (it's about lab operations)

### State Laws
- Some states have additional genetic testing privacy laws
- HIV test results may have extra protections

---

## Special Considerations

### Critical/Panic Values
- **ALWAYS PRESERVE** alert flags ("CRITICAL", "Notify immediately")
- Example: "Potassium 6.8 mmol/L [CRITICAL - NOTIFY PROVIDER]"

### Genetic Testing Results
- May include family member information → Redact all family names
- Genetic markers (e.g., BRCA1) → Preserve (it's the result, not PHI)

### Tumor Markers
- Often monitored over time (trends)
- Consider preserving relative dates for longitudinal tracking

### Pending Results
- "Result pending" status → Preserve
- Follow-up instructions → Preserve

---

## Android Implementation Notes

### Use Case: Home Health Nurse
Scenario from your medical-hipaa.md:
```
"glucose 289, increased Lantus to 22u"
```

Workflow:
1. Nurse photographs patient's lab printout (glucose: 289 mg/dL)
2. ShieldText OCRs → Redacts patient name/MRN
3. Nurse texts: "Patient [REDACTED] - glucose 289 mg/dL [HIGH], adjusted insulin per protocol"
4. Care team receives clinically useful info, zero PHI
5. Time: <1 second for redaction

### UI Considerations
- Show abnormal flags prominently (color-code HIGH/LOW)
- Allow user to preserve/redact specific tests (customization)
- Display reference ranges inline for context

---

## Status

**Phase**: 2 - Document Type Complete
- ✅ Lab-specific PHI identified
- ✅ Clinical preservation rules defined
- ✅ Real-world examples (blood work, pathology)
- ✅ OCR considerations documented
- ✅ Android workflow defined

**Dependencies**:
- H1-H18 HIPAA identifiers (existing)
- H28 Lab results identifier (to be created)
- H22 Body locations identifier (to be created)

**Next**: Clinical/Progress Notes skill
