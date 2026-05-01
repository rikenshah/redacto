# HIPAA - Protected Health Information (PHI) Identifiers

## Summary

HIPAA's Safe Harbor method (45 CFR § 164.514(b)(2)) requires removal of **18 specific identifiers** to de-identify health information.

**Total identifiers**: 18 mandated + 10 contextual medical identifiers = **28 total (H1-H28)**

---

## The 18 Mandated HIPAA Identifiers

### H1 - Names

**Scope**: All names directly identifying the individual
- Full name, first name, last name, middle name
- Maiden name
- Nicknames if identifying
- Aliases

**Examples**:
```
✅ REDACT:
"Patient: Mary Elizabeth Johnson"
"Mrs. Chen"
"John 'Johnny' Smith"

❌ PRESERVE:
Generic role names not linked to individual ("the patient", "the nurse")
```

**Redaction**: `[REDACTED NAME]` or `[NAME]`

---

### H2 - Geographic Subdivisions Smaller Than State

**Scope**: Address components except state
- Street address
- City
- County  
- Precinct
- ZIP code (EXCEPTION: First 3 digits OK if area has >20,000 people)

**Examples**:
```
✅ REDACT:
"123 Main Street, Apt 4B"
"San Francisco, CA 94102" → "CA" (keep state only)
"Los Angeles County"

⚠️ PARTIAL OK:
"941**" (first 3 of ZIP if >20K population)
"California" (state is OK)

❌ PRESERVE:
"United States"
"West Coast"
```

**Redaction**: `[REDACTED ADDRESS]` or keep state: `State: CA`

---

### H3 - Dates (Except Year)

**Scope**: All dates related to the individual EXCEPT year

**Dates to Redact**:
- Date of birth (except year)
- Date of admission
- Date of discharge
- Date of death (except year)
- Date of service
- Appointment dates
- Prescription dates

**Examples**:
```
✅ REDACT:
"DOB: 03/15/1948" → "DOB: [REDACTED]/1948" (keep year)
"Admitted: 12/25/2025" → "Admitted: [REDACTED]/2025"
"Appointment: April 15, 2026" → "Appointment: [REDACTED], 2026"

✅ PRESERVE:
"Year: 1948" (year alone is OK)
"2025" (year alone)
"Patient is 78 years old" (age >89 must aggregate to ≥90)

⚠️ SPECIAL: Ages >89 → "≥90"
```

**Redaction**: `[REDACTED DATE]/2025` (keep year)

---

### H4 - Telephone Numbers

**Scope**: ALL telephone numbers
- Home, work, mobile, cell
- Pager numbers
- Any phone number that could reach the individual

**Examples**:
```
✅ REDACT:
"Phone: (415) 555-0199"
"Mobile: 555-123-4567"
"Emergency contact: 408-555-0101"

❌ PRESERVE:
Hospital main line (not personal)
Pharmacy customer service line
```

**Redaction**: `[REDACTED PHONE]` or `***-***-****`

---

### H5 - Fax Numbers

**Scope**: All fax numbers
- Home fax
- Work fax
- Medical office fax (if personal)

**Redaction**: `[REDACTED FAX]`

---

### H6 - Email Addresses

**Scope**: All personal email addresses
- Personal email accounts
- Work email if identifying

**Examples**:
```
✅ REDACT:
"mary.johnson@gmail.com"
"patient123@yahoo.com"

❌ PRESERVE:
"info@hospital.org" (generic institution)
"appointments@clinic.com" (generic service)
```

**Redaction**: `[REDACTED EMAIL]`

---

### H7 - Social Security Numbers

**Scope**: All SSNs
- Full SSN: 123-45-6789
- Last 4 digits are still PHI if linkable

**Redaction**: `[REDACTED SSN]` (full redaction, no last 4)

---

### H8 - Medical Record Numbers (MRN)

**Scope**: Patient identification numbers assigned by healthcare facilities
- Hospital MRN
- Clinic patient ID
- Practice management system ID

**Examples**:
```
"MRN: 4471829"
"Patient ID: P-123456"
"Chart #: 789012"
```

**Redaction**: `[REDACTED MRN]`

---

### H9 - Health Plan Beneficiary Numbers

**Scope**: Insurance/health plan member IDs
- Insurance member number
- Medicare number
- Medicaid ID
- Group policy number

**Examples**:
```
"Insurance ID: ABC123456789"
"Medicare #: 1EG4-TE5-MK73"
"Member ID: XYZ987654"
```

**Redaction**: `[REDACTED INSURANCE ID]`

---

### H10 - Account Numbers

**Scope**: Financial account numbers related to healthcare
- Hospital billing account
- Medical account number
- Payment plan account

**Redaction**: `[REDACTED ACCOUNT]`

---

### H11 - Certificate/License Numbers

**Scope**: Professional licenses IF they identify the individual
- Driver's license (on patient intake forms)
- Professional licenses (if identifying patient, rare)

**Context**: Usually applies to patient records, not provider licenses

**Redaction**: `[REDACTED LICENSE]`

---

### H12 - Vehicle Identifiers

**Scope**: Vehicle identification
- License plate numbers
- VIN (Vehicle Identification Number)

**Context**: May appear on accident reports, intake forms

**Redaction**: `[REDACTED VEHICLE ID]`

---

### H13 - Device Identifiers & Serial Numbers

**Scope**: Medical device IDs assigned to individual
- Pacemaker serial number
- Prosthetic device ID
- Implant serial number
- Hearing aid serial number
- Insulin pump ID

**Examples**:
```
"Pacemaker SN: ABC123456"
"Prosthetic limb ID: PL-987654"
```

**Redaction**: `[REDACTED DEVICE ID]`

---

### H14 - Web URLs

**Scope**: Personal website URLs
- Personal health blogs
- Personal websites mentioning health

**Redaction**: `[REDACTED URL]`

---

### H15 - IP Addresses

**Scope**: Internet Protocol addresses
- IPv4: 192.168.1.1
- IPv6: 2001:0db8:85a3...

**Context**: May appear in telemedicine logs, patient portal access logs

**Redaction**: `[REDACTED IP]`

---

### H16 - Biometric Identifiers

**Scope**: Biometric data used for identification
- Fingerprints
- Voice prints
- Retinal scans
- Iris scans
- Facial recognition data

**Redaction**: `[REDACTED BIOMETRIC]` or blur/remove biometric data

---

### H17 - Full-Face Photographs

**Scope**: Photos showing full face
- Patient photos
- Identification photos
- Comparable images

**Exception**: Photos of body parts (hands, legs, etc.) without face are OK

**Redaction**: Blur face or `[PHOTO REDACTED]`

---

### H18 - Any Other Unique Identifier

**Scope**: Catch-all for any other unique identifying characteristic
- Unusual identifying characteristics
- Rare combinations of attributes
- Genetic markers (if identifying)

**Redaction**: Context-dependent

---

## Additional Medical Context Identifiers (H19-H28)

Beyond the 18 mandated identifiers, these require LLM reasoning:

### H19 - Diagnoses/Conditions

**Scope**: Medical conditions linked to patient
- Specific diagnoses
- Symptoms
- Disease names

**Why PHI**: Diagnosis + any identifier = PHI

**Examples**:
```
"Mrs. Chen has diabetes" ← PHI (name + diagnosis)
"Patient has heel wound" ← OK if name redacted
"Compound fracture" ← OK if de-identified
```

**Redaction**: If linked to identifiers, redact diagnosis OR identifiers

---

### H20 - Medications/Dosages

**Scope**: Specific medications and dosages linked to patient
- Drug names
- Dosages
- Administration instructions

**Examples**:
```
"Increased Lantus to 22u" ← PHI if patient identifiable
"Insulin dose adjusted" ← OK if de-identified
```

**Redaction**: Redact specific drug names/doses if identifying

---

### H21 - Vital Signs

**Scope**: Specific vital sign values
- Blood pressure, heart rate, temperature, weight, glucose

**Examples**:
```
"Glucose 289" ← PHI if linked to patient
"Glucose elevated" ← OK if de-identified
```

**Redaction**: Generalize ("elevated", "normal", "abnormal")

---

### H22 - Body Locations

**Scope**: Specific anatomical locations if identifying
- "Left heel wound" vs. "Heel wound"

**Redaction**: Generalize if overly specific

---

### H23 - Family Medical History

**Scope**: Relatives' health information
- Family member names + conditions
- Inherited conditions

**Redaction**: Redact family member names

---

### H24 - Mental Health Notes

**Scope**: Psychotherapy notes (extra protection under 45 CFR § 164.508(a)(2))
- Counseling session notes
- Mental health diagnoses
- Psychiatric treatment

**Redaction**: Requires extra care - usually full redaction

---

### H25 - Appointment Details

**Scope**: Specific appointment information
- Dates (redact per H3)
- Provider names
- Locations

**Redaction**: Redact dates, provider names if identifying

---

### H26 - Insurance Claims

**Scope**: Billing/claim information
- Claim numbers
- Insurance details
- Payment amounts

**Redaction**: Redact account numbers, claim numbers

---

### H27 - Provider Names (When Linked to Patient)

**Scope**: Doctor/nurse names in patient context
- "Dr. Smith is her oncologist" ← PHI if patient identifiable

**Redaction**: Redact or generalize ("her oncologist")

---

### H28 - Lab Results

**Scope**: Specific lab values
- Test results
- Pathology reports

**Redaction**: Generalize ("abnormal", "within normal limits")

---

## Document-Specific Coverage

### Patient Medical Record
**Required**: H1-H18 (all 18 mandated), H19-H28 (medical context)

### Prescription
**Required**: H1 (name), H3 (date), H4 (phone), H8 (MRN), H20 (medication details)

### Lab Results
**Required**: H1 (name), H3 (dates), H8 (MRN), H28 (specific values)

### Clinical Notes
**Required**: H1 (name), H3 (dates), H8 (MRN), H19-H28 (all clinical details)

---

## Compliance Validation

**Safe Harbor Method**: ALL 18 identifiers (H1-H18) must be removed

**Status**:
- ✅ COMPLIANT: All 18 removed + no actual knowledge of re-identification
- ❌ NON-COMPLIANT: Even 1 identifier present = PHI

---

## References

- **De-identification Rule**: 45 CFR § 164.514(b)(2)
- **HHS Guidance**: https://www.hhs.gov/hipaa/for-professionals/privacy/special-topics/de-identification/index.html

---

## Document History

**Last Updated**: April 30, 2026  
**Version**: 1.0
