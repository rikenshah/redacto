---
name: hipaa-identifiers
description: Defines the 18 mandatory HIPAA identifiers that must be 100% removed for Safe Harbor de-identification under 45 CFR § 164.514(b)(2). Includes names, geographic subdivisions, dates, phone/fax/email, SSN, MRN, insurance IDs, account numbers, license numbers, vehicle/device IDs, URLs, IP addresses, biometrics, photos, and unique identifiers. Provides detection patterns (regex and LLM validation) and redaction rules for each. Use when validating HIPAA compliance, implementing PHI detection, or building medical redaction logic.
---

# HIPAA Identifiers (H1-H18)

## Overview

The **18 mandated HIPAA identifiers** that must be removed for Safe Harbor de-identification.

**Priority**: ALL are CRITICAL (100% removal required)  
**Used By**: All medical documents  
**Regulation**: 45 CFR § 164.514(b)(2)

---

## Quick Reference

| ID | Identifier | Example | Redaction |
|----|-----------|---------|-----------|
| H1 | Names | Mary Johnson | [REDACTED NAME] |
| H2 | Geography (<state) | 123 Main St, SF | [REDACTED ADDRESS] or State only |
| H3 | Dates (except year) | 03/15/1948 | [REDACTED]/1948 |
| H4 | Telephone | (415) 555-0199 | [REDACTED PHONE] |
| H5 | Fax | 555-123-4567 | [REDACTED FAX] |
| H6 | Email | patient@email.com | [REDACTED EMAIL] |
| H7 | SSN | 123-45-6789 | [REDACTED SSN] |
| H8 | MRN | MRN: 4471829 | [REDACTED MRN] |
| H9 | Insurance ID | ABC123456789 | [REDACTED INSURANCE] |
| H10 | Account # | Acct: 987654 | [REDACTED ACCOUNT] |
| H11 | License # | DL: A1234567 | [REDACTED LICENSE] |
| H12 | Vehicle ID | ABC-1234 | [REDACTED VEHICLE] |
| H13 | Device Serial | Pacemaker SN: X123 | [REDACTED DEVICE] |
| H14 | Web URL | patientblog.com/mary | [REDACTED URL] |
| H15 | IP Address | 192.168.1.1 | [REDACTED IP] |
| H16 | Biometric | Fingerprint data | [REDACTED BIOMETRIC] |
| H17 | Face Photo | [Patient photo] | [BLUR FACE] |
| H18 | Other Unique ID | Genetic marker | [REDACTED ID] |

---

## Detection Patterns (Simplified)

### H1 - Names
- **Context**: Near "Patient:", "Name:", in headers
- **LLM Required**: YES (distinguish person vs. institution)

### H2 - Geography
- **Pattern**: Street addresses, cities
- **Exception**: State alone is OK

### H3 - Dates
- **Pattern**: MM/DD/YYYY, dates near "DOB:", "Admission:", etc.
- **Exception**: Year alone is OK

### H4-H6 - Contact Info
- **Patterns**: Phone (xxx-xxx-xxxx), Email (x@x.com)
- **LLM Required**: YES (distinguish personal vs. hospital main line)

### H7 - SSN
- **Pattern**: xxx-xx-xxxx or 9 digits near "SSN:"

### H8 - MRN
- **Context**: Near "MRN:", "Patient ID:", "Chart #:"

### H9 - Insurance
- **Context**: Near "Insurance:", "Member ID:", "Policy #:"

### H10 - Account
- **Context**: Near "Account:", "Billing #:"

### H13 - Device Serial
- **Context**: Near "Pacemaker", "Implant", "Serial Number:"

### H16 - Biometric
- **Detection**: Fingerprint images, voice recordings, retinal scans

### H17 - Photos
- **Detection**: Image analysis for face detection
- **Action**: Blur face or remove image

---

## Redaction Rules

**ALL identifiers**: 100% removal required (no exceptions)

**Partial OK for H2 (Geography)**:
- Keep state: "California" ✅
- Keep first 3 of ZIP if >20K population: "941**" ✅
- Everything else: REDACT

**Partial OK for H3 (Dates)**:
- Keep year: "1948" ✅
- Keep ages <90: "Age 78" ✅
- Ages ≥90: Aggregate to "≥90" ⚠️

**Everything else**: FULL REDACTION

---

## Real-World Example

**From your use case**:

**Input**:
```
Mrs. Chen (DOB 3/15/48, MRN 4471829) — left heel wound not 
improving, glucose 289, increased Lantus to 22u. She mentioned 
depression again — need psych referral. Daughter Lisa 
(408-555-1234) wants updates.
```

**Identifiers Found**:
- H1: Mrs. Chen, Lisa
- H3: 3/15/48
- H4: 408-555-1234
- H8: 4471829

**Output**:
```
Patient [REDACTED] — left heel wound not improving, glucose 
elevated, insulin dose adjusted per protocol. Patient reports 
mood concerns — requesting psych referral. Family contact 
requests updates — see care plan for details.
```

**Status**: ✅ All HIPAA identifiers removed

---

## Used By Document Types

- ✅ Patient medical records
- ✅ Prescriptions
- ✅ Lab results
- ✅ Clinical notes
- ✅ Discharge summaries
- ✅ Imaging reports
- ✅ All medical documents

---

## Status

**Built**: Simplified reference  
**Priority**: CRITICAL (main focus for ShieldText medical)
