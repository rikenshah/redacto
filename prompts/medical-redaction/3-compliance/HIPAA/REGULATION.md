# HIPAA (Health Insurance Portability and Accountability Act)

## Overview

**Full Name**: Health Insurance Portability and Accountability Act  
**Enacted**: August 21, 1996  
**Jurisdiction**: United States  
**Enforced By**: HHS Office for Civil Rights (OCR)  
**Citation**: 45 CFR Parts 160, 162, 164

### Purpose

HIPAA protects **Protected Health Information (PHI)** - individually identifiable health information held or transmitted by covered entities and business associates.

### Key Concept: Protected Health Information (PHI)

**PHI Definition** (45 CFR § 160.103):
> Individually identifiable health information transmitted or maintained in any form or medium that:
> - Relates to past, present, or future physical/mental health
> - Relates to provision of healthcare
> - Relates to payment for healthcare
> - Identifies the individual OR could be used to identify the individual

---

## The 18 HIPAA Identifiers

Must be removed to create "de-identified" data under Safe Harbor method (45 CFR § 164.514(b)(2)):

1. **Names** - Full name, last name, maiden name
2. **Geographic subdivisions smaller than state** - Street address, city, county, ZIP (first 3 digits OK if >20K people)
3. **Dates** - Birth, admission, discharge, death, service dates (EXCEPT year)
4. **Telephone numbers** - All phone numbers
5. **Fax numbers** - All fax numbers
6. **Email addresses** - All email addresses
7. **Social Security Numbers** - Full SSN
8. **Medical Record Numbers (MRN)** - Hospital/clinic patient ID
9. **Health Plan Beneficiary Numbers** - Insurance member IDs
10. **Account Numbers** - Hospital account, billing account
11. **Certificate/License Numbers** - Professional licenses if identifying
12. **Vehicle Identifiers** - License plates, VIN
13. **Device Identifiers & Serial Numbers** - Pacemaker SN, prosthetic device ID
14. **Web URLs** - Personal website URLs
15. **IP Addresses** - Internet Protocol addresses
16. **Biometric Identifiers** - Fingerprints, voice prints, retinal scans
17. **Full-Face Photographs** - Photos and comparable images
18. **Any Other Unique Identifier** - Anything that could identify the individual

---

## Requirements

### Privacy Rule (45 CFR Part 164, Subpart E)

**Permitted Uses**:
- Treatment, Payment, Healthcare Operations (TPO) - No authorization needed
- Public health activities
- Law enforcement (with restrictions)
- Research (with authorization or IRB waiver)

**Patient Rights**:
- Right to access own PHI
- Right to request amendments
- Right to accounting of disclosures
- Right to request restrictions

**Covered Entity Obligations**:
- Provide Notice of Privacy Practices
- Obtain authorization for non-TPO uses
- Minimum necessary standard (use only what's needed)
- Safeguard PHI

### Security Rule (45 CFR Part 164, Subpart C)

**Administrative Safeguards**:
- Security management process
- Assigned security responsibility
- Workforce training
- Access authorization

**Physical Safeguards**:
- Facility access controls
- Workstation security
- Device and media controls

**Technical Safeguards**:
- Access controls
- Audit controls
- Integrity controls
- Transmission security

### Breach Notification Rule (45 CFR §§ 164.400-414)

If PHI breach affects:
- **<500 individuals**: Notify HHS annually
- **≥500 individuals**: Notify HHS within 60 days + media notification

---

## Who Must Comply

### Covered Entities
- **Healthcare Providers**: Doctors, hospitals, clinics, home health agencies, pharmacies
- **Health Plans**: Insurance companies, HMOs, Medicare, Medicaid
- **Healthcare Clearinghouses**: Billing services, claims processors

### Business Associates
- Anyone who handles PHI on behalf of covered entities
- EHR vendors, medical billing companies, cloud storage providers, shredding services

### Individuals (Under ShieldText Context)
Healthcare workers using personal devices (BYOD) to communicate patient information

---

## Penalties

### Civil Penalties (Per Violation)

| Violation Level | Penalty Range | Who Pays |
|----------------|---------------|----------|
| **Tier 1**: Did not know | $100-$50,000 | Individual/Entity |
| **Tier 2**: Reasonable cause | $1,000-$50,000 | Individual/Entity |
| **Tier 3**: Willful neglect (corrected) | $10,000-$50,000 | Individual/Entity |
| **Tier 4**: Willful neglect (not corrected) | $50,000 | Individual/Entity |

**Annual Cap per Violation Category**: $1.9 million

### Criminal Penalties

| Offense | Fine | Prison |
|---------|------|--------|
| Obtaining PHI under false pretenses | Up to $50,000 | Up to 1 year |
| Obtaining PHI with intent to sell/transfer | Up to $100,000 | Up to 5 years |
| Obtaining PHI with intent to use for commercial advantage/personal gain/malicious harm | Up to $250,000 | Up to 10 years |

---

## Applicability Rules

### When HIPAA Applies in ShieldText

```yaml
hipaa_applies_if:
  document_contains:
    health_information: true
    identifiable: true
  AND:
    created_by:
      - healthcare_provider
      - health_plan
      - healthcare_clearinghouse
      - business_associate
      - healthcare_worker (using for work)
  
  document_types:
    - patient_medical_record
    - prescription
    - lab_result
    - clinical_note
    - discharge_summary
    - insurance_claim
    - billing_statement (medical)
```

### When HIPAA Does NOT Apply

```yaml
hipaa_not_applicable:
  - employment_records (not related to health)
  - education_records (covered by FERPA)
  - de-identified_data (all 18 identifiers removed)
  - deceased_50_years
  - personal_health_records (not from covered entity)
```

---

## Safe Harbor De-identification

To create de-identified data (45 CFR § 164.514(b)(2)):

**Remove ALL 18 identifiers** listed above, AND

**No actual knowledge** that remaining information could be used alone or in combination to identify the individual.

**Result**: Data is no longer PHI, HIPAA does not apply

---

## Real-World Impact

From your use case (medical-hipaa.md):

**Problem**:
> "Mrs. Chen (DOB 3/15/48, MRN 4471829) — left heel wound not improving, glucose 289, increased Lantus to 22u. She mentioned depression again — need psych referral. Daughter Lisa (408-555-1234) wants updates."

**HIPAA Identifiers Present**: 8
1. Name (Mrs. Chen)
2. Date (DOB 3/15/48)
3. MRN (4471829)
4. Diagnosis (heel wound)
5. Lab value (glucose 289)
6. Medication (Lantus 22u)
7. Mental health (depression)
8. Family name + phone (Lisa, 408-555-1234)

**Penalty Risk**: Each category violation = up to $1.9M/year  
**Total Exposure**: Potentially $15M+ if multiple categories violated

**ShieldText Solution**:
> "Patient [REDACTED] — left heel wound not improving, glucose elevated, insulin dose adjusted per protocol. Patient reports mood concerns — requesting psych referral. Family contact requests updates — see care plan for details."

**Result**: ✅ HIPAA COMPLIANT - All PHI redacted, clinical meaning preserved

---

## References

### Legal Citations
- **Primary Statute**: HIPAA, Public Law 104-191
- **Privacy Rule**: 45 CFR Part 164, Subpart E
- **Security Rule**: 45 CFR Part 164, Subpart C
- **Breach Notification**: 45 CFR §§ 164.400-414

### Official Resources
- **HHS OCR**: https://www.hhs.gov/hipaa
- **Privacy Rule Summary**: https://www.hhs.gov/hipaa/for-professionals/privacy/index.html
- **De-identification Guidance**: https://www.hhs.gov/hipaa/for-professionals/privacy/special-topics/de-identification/index.html

### Enforcement
- **OCR Enforcement**: https://www.hhs.gov/hipaa/for-professionals/compliance-enforcement/index.html
- **Breach Portal**: https://ocrportal.hhs.gov/ocr/breach/breach_report.jsf
- **Since 2009**: 173+ million individuals affected by reported breaches

---

## Document History

**Last Updated**: April 30, 2026  
**Version**: 1.0  
**Changes**: Initial documentation for ShieldText medical redaction
