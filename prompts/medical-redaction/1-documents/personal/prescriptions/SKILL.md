---
name: prescriptions
description: Redacts prescription documents and medication lists for safe sharing with pharmacies, caregivers, or care coordinators ensuring HIPAA compliance. Removes patient names, DOB, MRN, addresses, phone numbers, prescriber details, and pharmacy information while preserving medication names, dosages, frequencies, and instructions. Use when processing prescriptions, medication lists, pharmacy labels, Rx documents, or when the user mentions prescription, medications, pharmacy, or drug lists.
---

# Prescription Redaction

## Purpose

Redact prescription documents and medication lists for safe sharing with pharmacies, family caregivers, or care coordinators while maintaining HIPAA compliance.

**Use Case**: Home health nurses photographing prescription bottles, sharing medication lists with care teams, coordinating pharmacy transfers.

---

## Document Types

### Physical Prescription Paper
- Doctor's handwritten Rx
- Printed prescription forms
- Hospital discharge prescriptions

### Medication Lists
- Current medications summary
- Hospital medication reconciliation forms
- Patient-provided med lists

### Pharmacy Labels
- Prescription bottle labels (photographed)
- Pharmacy printouts
- Refill notifications

---

## What Gets Redacted

### HIPAA Identifiers (18 - All Critical)

#### Direct Patient Identifiers
1. ✅ **Patient Name** → `[REDACTED NAME]` or `Patient [REDACTED]`
2. ✅ **Date of Birth** → `[REDACTED DOB]` or year only if needed
3. ✅ **Address** → `[REDACTED ADDRESS]`
4. ✅ **Phone Number** → `[REDACTED PHONE]`
5. ✅ **Email** → `[REDACTED EMAIL]`
6. ✅ **SSN** → `[REDACTED SSN]` (rare on prescriptions)
7. ✅ **MRN** → `[REDACTED MRN]`

#### Prescription-Specific Identifiers
8. ✅ **Prescription Number** → `[REDACTED RX#]`
   - Example: Rx #123456789
   
9. ✅ **Insurance ID/Group Number** → `[REDACTED INSURANCE]`

10. ✅ **Pharmacy Name & Address** → `[REDACTED PHARMACY]` or generic "Pharmacy"
    - Keep city/state if needed for coordination

11. ✅ **Pharmacy Phone/Fax** → `[REDACTED PHONE]`

12. ✅ **Prescriber Full Name** → `[REDACTED PRESCRIBER]` or "Prescribing physician"
    - Exception: Can keep specialty if needed ("Cardiologist prescribed...")

13. ✅ **Prescriber DEA Number** → `[REDACTED DEA]`

14. ✅ **Prescriber NPI** → `[REDACTED NPI]`

15. ✅ **Prescriber Address/Phone** → `[REDACTED]`

16. ✅ **Dates** → Redact specific dates, keep context
    - Prescription date: `[REDACTED DATE]` or just year/month
    - Fill date: `Filled [REDACTED]`
    - Expiration: `Expires [REDACTED]`

---

## What Gets PRESERVED

### Critical Clinical Information

✅ **Medication Name** (generic and brand)
- Example: "Metformin", "Lantus (insulin glargine)"

✅ **Dosage & Strength**
- Example: "500mg", "22 units"

✅ **Route of Administration**
- Example: "oral", "subcutaneous injection", "topical"

✅ **Frequency/Schedule**
- Example: "twice daily", "every 12 hours", "as needed"

✅ **Quantity**
- Example: "90 tablets", "3 vials"

✅ **Refills Remaining**
- Example: "2 refills left"

✅ **Special Instructions**
- Example: "Take with food", "Refrigerate", "Do not crush"

✅ **Warnings/Contraindications** (clinical relevance)
- Example: "May cause drowsiness", "Avoid alcohol"

✅ **Purpose** (if stated)
- Example: "For blood pressure", "Pain management"

---

## Example Redactions

### Example 1: Pharmacy Label

**BEFORE:**
```
RxCare Pharmacy
123 Main Street, Springfield, IL 62701
Phone: (217) 555-0199

Patient: Sarah Johnson
DOB: 04/15/1982
Rx #: 8847291

Metformin HCl 500mg Tablets
Take 1 tablet by mouth twice daily with meals
Qty: 90 tablets | Refills: 2

Prescribed by: Dr. Amanda Chen, MD
NPI: 1234567890
Filled: 03/20/2024 | Expires: 03/20/2025

Do not take with alcohol. May cause stomach upset.
```

**AFTER:**
```
[REDACTED PHARMACY]

Patient: [REDACTED NAME]
DOB: [REDACTED]
Rx #: [REDACTED]

Metformin HCl 500mg Tablets
Take 1 tablet by mouth twice daily with meals
Qty: 90 tablets | Refills: 2

Prescribed by: [REDACTED PRESCRIBER]
Filled: [REDACTED] | Expires: [REDACTED]

Do not take with alcohol. May cause stomach upset.
```

---

### Example 2: Medication List (Hospital Discharge)

**BEFORE:**
```
MEDICATION RECONCILIATION
Patient: Robert Martinez | MRN: 4471829 | DOB: 11/22/1956
Discharge Date: 04/18/2024
Attending: Dr. Lisa Patel, Cardiology

CURRENT MEDICATIONS:
1. Lisinopril 10mg - 1 tablet PO daily (for blood pressure)
2. Atorvastatin 40mg - 1 tablet PO at bedtime (cholesterol)
3. Aspirin 81mg - 1 tablet PO daily (blood thinner)
4. Metoprolol 25mg - 1 tablet PO twice daily (heart rate)

DISCONTINUED:
- Hydrochlorothiazide 25mg (switched to Lisinopril)

ALLERGIES: Penicillin (rash), Sulfa drugs (hives)

Follow-up: Call Dr. Patel's office (555-0123) within 7 days
```

**AFTER:**
```
MEDICATION RECONCILIATION
Patient: [REDACTED NAME] | MRN: [REDACTED] | DOB: [REDACTED]
Discharge Date: [REDACTED]
Attending: [REDACTED PRESCRIBER], Cardiology

CURRENT MEDICATIONS:
1. Lisinopril 10mg - 1 tablet PO daily (for blood pressure)
2. Atorvastatin 40mg - 1 tablet PO at bedtime (cholesterol)
3. Aspirin 81mg - 1 tablet PO daily (blood thinner)
4. Metoprolol 25mg - 1 tablet PO twice daily (heart rate)

DISCONTINUED:
- Hydrochlorothiazide 25mg (switched to Lisinopril)

ALLERGIES: Penicillin (rash), Sulfa drugs (hives)

Follow-up: Contact prescribing physician within 7 days
```

**Note**: Allergy information is PRESERVED because it's clinically critical for safe medication administration.

---

### Example 3: Handwritten Prescription (OCR)

**BEFORE (OCR'd):**
```
Sarah Johnson
DOB 4/15/82

Rx: Gabapentin 300mg
Sig: 1 cap PO TID
Disp: #90
Refills: 2

Dr. Chen
DEA: BC1234563
Date: 3/20/24
```

**AFTER:**
```
Patient: [REDACTED]
DOB: [REDACTED]

Rx: Gabapentin 300mg
Sig: 1 cap PO TID (three times daily)
Disp: #90
Refills: 2

Prescriber: [REDACTED]
Date: [REDACTED]
```

---

## Required Identifier Categories

Uses these identifier skills:

### From H1-H18 (HIPAA Identifiers)
- H1: Names (patient, prescriber)
- H2: Addresses (patient, pharmacy, prescriber office)
- H3: Dates (prescription date, fill date, DOB)
- H4: Phone numbers (pharmacy, prescriber)
- H5: Fax numbers
- H6: Email addresses
- H7: SSN (rare)
- H8: MRN
- H10: Account numbers (Rx number, insurance ID)
- H11: License numbers (DEA, NPI)

### From H19-H28 (Medical Context)
- H20: Medications and dosages (**PRESERVE**)
- H19: Conditions/diagnoses (if listed as indication) (**PRESERVE**)

---

## Processing Workflow

```
1. OCR EXTRACTION (if photo)
   └─> Extract text from prescription photo using Google ML Kit
   
2. DOCUMENT CLASSIFICATION
   └─> Identify as "prescription" document type
   
3. PHI DETECTION
   ├─> Find patient identifiers (name, DOB, address, phone)
   ├─> Find prescription-specific IDs (Rx#, insurance, DEA, NPI)
   ├─> Find pharmacy details
   ├─> Find prescriber details
   └─> Find dates
   
4. CLINICAL INFORMATION PRESERVATION
   ├─> Identify medication names
   ├─> Identify dosages and frequencies
   ├─> Identify special instructions
   └─> Mark as PRESERVE (do not redact)
   
5. REDACTION
   ├─> Replace all PHI with placeholders
   └─> Keep all medication details intact
   
6. VALIDATION
   ├─> Confirm no patient/prescriber names remain
   ├─> Confirm no Rx#, DEA, NPI remain
   ├─> Confirm medication details are intact
   └─> Return compliance status
```

---

## Compliance Requirements

### HIPAA Privacy Rule
- ✅ 100% removal of all 18 identifiers
- ✅ Medication information (non-identifying) can be shared

### State Laws
- Some states have additional Rx privacy laws
- Controlled substances may have stricter rules

### Use Case Alignment
- **Safe to share**: Redacted prescription with family caregiver, care coordinator, or second pharmacy
- **Clinical value preserved**: Medication name, dose, frequency for continuity of care

---

## Special Considerations

### Controlled Substances
- DEA number MUST be redacted
- Medication name/dose CAN be shared (it's clinical info, not PHI)

### Pediatric Prescriptions
- May include parent/guardian name → Redact as H1 (Names)
- Weight-based dosing → Preserve (clinical)

### Insulin Prescriptions
- Complex dosing schedules → Preserve all instructions
- Sliding scale charts → Preserve

### Allergy Information
- **ALWAYS PRESERVE** - Critical for patient safety
- "Allergies: Penicillin" is clinically essential, not PHI when patient is de-identified

---

## Android Implementation Notes

### OCR Challenges
- Handwritten prescriptions may have low OCR accuracy
- Pharmacy labels often use small fonts
- Recommendation: Show OCR confidence score, allow user correction

### Real-World Use Case
Home health nurse scenario:
1. Patient shows nurse their prescription bottles
2. Nurse takes photo of each label
3. ShieldText OCRs → Redacts → Outputs clean med list
4. Nurse texts med list to care coordinator
5. Time: ~30 seconds for 5 medications

---

## Status

**Phase**: 2 - Document Type Complete
- ✅ Prescription-specific PHI identified
- ✅ Clinical preservation rules defined
- ✅ Real-world examples provided
- ✅ Android workflow documented

**Dependencies**:
- H1-H18 HIPAA identifiers (existing)
- H20 Medications identifier (to be created)

**Next**: Lab Results skill
