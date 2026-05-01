---
name: patient-records
description: Redacts patient medical records for safe sharing with care teams, family, or second-opinion doctors ensuring HIPAA Safe Harbor compliance. Removes all 18 HIPAA identifiers (names, MRNs, dates, locations) while preserving clinical meaning for home health nurses, EMTs, social workers, and clinicians. Use when processing patient charts, medical records, clinical documentation, progress notes, or when the user mentions patient records, medical files, clinical notes, HIPAA compliance, or healthcare communication.
---

# Patient Medical Records Redaction

## Purpose

Redact patient medical records to safely share with family, second opinion doctors, or care teams while maintaining **HIPAA compliance**.

**Primary Use Case**: Home health nurses, EMTs, social workers communicating patient updates quickly without HIPAA violations.

---

## The Problem (From Your Use Case)

> "A home health nurse finishing a patient visit needs to update the care team now, in the 45 seconds before driving to her next appointment. The hospital's compliant messaging app requires VPN, loads slowly on rural cell networks. So she texts..."

**Risk**: HIPAA violation, up to $1.9M in fines

**ShieldText Solution**: Redact in <1 second on-device, text safely

---

## What Gets Redacted

### ALL 18 HIPAA Identifiers (100% Required)
1. ✅ Names (patient, family members)
2. ✅ Address (keep state only)
3. ✅ Dates (keep year only)
4. ✅ Phone numbers
5. ✅ Fax numbers
6. ✅ Email addresses
7. ✅ Social Security Numbers
8. ✅ Medical Record Numbers (MRN)
9. ✅ Insurance IDs
10. ✅ Account numbers
11. ✅ License numbers
12. ✅ Vehicle IDs
13. ✅ Device serial numbers
14. ✅ Web URLs
15. ✅ IP addresses
16. ✅ Biometric data
17. ✅ Face photos
18. ✅ Any other unique IDs

### Additional Medical Context
- Specific diagnoses (generalize if needed)
- Exact medication names/doses (generalize if needed)
- Exact vital signs (generalize: "elevated", "normal")

### What to Preserve
- Clinical meaning (condition, treatment, urgency)
- General body locations ("heel wound" OK, no identifying details)
- General medical concepts
- Necessary clinical context

---

## Required Identifiers

```yaml
critical:
  - H1-H18: All 18 HIPAA identifiers
```

---

## Real-World Example

**Scenario**: Home health nurse texts care team

**Before ShieldText** (HIPAA VIOLATION):
```
Mrs. Chen (DOB 3/15/48, MRN 4471829) — left heel wound 
not improving, glucose 289, increased Lantus to 22u. 
She mentioned depression again — need psych referral. 
Daughter Lisa (408-555-1234) wants updates.

Violations: 8 identifiers exposed
Risk: $1.9M+ in fines
Time to send: 30 seconds
```

**After ShieldText** (HIPAA COMPLIANT):
```
Patient [REDACTED] — left heel wound not improving, 
glucose elevated, insulin dose adjusted per protocol. 
Patient reports mood concerns — requesting psych referral. 
Family contact requests updates — see care plan for details.

Identifiers removed: 8
Status: ✅ HIPAA COMPLIANT
Time to redact + send: 45 seconds total (0.8s redaction)
```

---

## Processing Workflow

```
1. DETECT: Medical record (header, MRN, diagnosis keywords)
2. LOAD: H1-H18-hipaa-identifiers/
3. SCAN: Find all 18 identifier types
4. REDACT: Remove all identifiers
5. VALIDATE: Check all 18 removed (100%)
6. OUTPUT: De-identified record + compliance report
```

---

## Document Structure

**Typical Medical Record**:
```
Patient: Mary Elizabeth Johnson        ← H1
DOB: 03/15/1948                        ← H3
MRN: 4471829                           ← H8
Address: 123 Main St, SF, CA 94102     ← H2
Phone: (415) 555-0199                  ← H4
Insurance: ABC123456789                ← H9

Chief Complaint: Diabetes management
Vital Signs: BP 130/85, Glucose 289mg/dL
Medications: Lantus 22 units daily
Assessment: Type 2 diabetes, uncontrolled
Plan: Increase insulin, psych referral
```

**After Redaction**:
```
Patient: [REDACTED NAME]
DOB: [REDACTED]/1948
MRN: [REDACTED]
Address: California
Phone: [REDACTED]
Insurance: [REDACTED]

Chief Complaint: Diabetes management
Vital Signs: BP elevated, Glucose elevated
Medications: Insulin dose adjusted per protocol
Assessment: Type 2 diabetes, uncontrolled
Plan: Medication adjustment, psych referral
```

---

## Use Cases

### 1. Home Health Nurse → Care Team
**Redact**: All identifiers  
**Preserve**: Clinical updates, urgency  
**Time**: <1 second

### 2. EMT → Hospital ER
**Redact**: Witness/bystander names  
**Preserve**: Patient condition, vitals  
**Context**: Tactical emergency (from your use case)

### 3. Social Worker → Supervisor
**Redact**: Child name, family names, address  
**Preserve**: Case details, safety concerns  
**Context**: CPS field notes (from your use case)

### 4. Patient → Family Member
**Redact**: MRN, insurance, provider details  
**Preserve**: Diagnosis, treatment plan, prognosis

---

## Compliance

**HIPAA Safe Harbor**: All 18 identifiers must be 100% removed

**Validation**:
```yaml
required:
  H1_names: 100%
  H2_geography: 100% (except state)
  H3_dates: 100% (except year)
  H4_H18: 100% (all remaining)

status:
  if_all_removed: "✅ HIPAA COMPLIANT - De-identified"
  if_any_present: "❌ HIPAA NON-COMPLIANT - Still PHI"
```

---

## Expected Output

### User-Facing Report

```
✅ Medical Record De-identified Successfully

Document: Patient Medical Record
Processing Time: 0.82 seconds
Identifiers Removed: 8 of 8 found

HIPAA Safe Harbor Status:
  ✅ All 18 identifier types checked
  ✅ 8 identifiers found and removed
  ✅ HIPAA COMPLIANT - De-identified

Safe to share via: Text, email, messaging apps
```

---

## Target Users (From Your Use Cases)

1. **Home health nurses** (6.4M in US)
   - Quick patient updates between visits
   - Rural areas with slow connections

2. **EMTs/Paramedics**
   - Tactical emergency communications
   - Protect witness identities

3. **Social workers**
   - CPS field notes
   - Multi-agency coordination

4. **Field clinicians**
   - Disaster response
   - Offline-capable redaction

---

## Performance Targets

- **Processing Time**: <1 second
- **Accuracy**: 99%+ identifier detection
- **HIPAA Compliance**: 100% (binary: compliant or not)

---

## Status

**Built**: Simplified foundation  
**Priority**: HIGH (main focus for ShieldText)  
**Next**: Test with real medical scenarios

---

## Impact

**From your MISSION.md**:
> "173 million individuals affected by PHI breaches since 2009"  
> "Unauthorized/accidental disclosure is a leading cause"  
> "The people getting fined aren't hackers. They're exhausted clinicians"

**ShieldText Solution**: Turn every smartphone into a HIPAA compliance layer
