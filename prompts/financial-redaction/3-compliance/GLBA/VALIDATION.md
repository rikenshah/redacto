# GLBA - Validation & Audit

## Purpose

This document defines how to validate GLBA compliance after redaction and generate audit reports.

---

## Compliance Validation Rules

### Critical Validation (100% Required)

All CRITICAL identifiers MUST be redacted. Even one exposure = NON-COMPLIANT.

```yaml
critical_identifiers:
  F2_SSN:
    redaction_required: 100%
    acceptable_formats:
      - "[REDACTED SSN]"
      - "***-**-1234"  # last 4 digits only
    unacceptable:
      - "123-45-6789"  # full SSN visible
      - "123-45-****"  # first 5 visible
    failure_message: "CRITICAL - SSN exposure violates GLBA § 6802 (NPI disclosure)"
  
  F10_BankAccount:
    redaction_required: 100%
    acceptable_formats:
      - "[REDACTED ACCOUNT]"
      - "********1234"  # last 4 digits only
    unacceptable:
      - "1234567890"  # full account visible
    failure_message: "CRITICAL - Account number exposure violates GLBA § 6802"
  
  F11_RoutingNumber:
    redaction_required: 100%
    acceptable_formats:
      - "[REDACTED ROUTING]"
      - "****0021"  # last 4 optional
    unacceptable:
      - "021000021"  # full routing visible
    failure_message: "CRITICAL - Routing number exposure violates GLBA § 6802"
  
  F12_IBAN:
    redaction_required: 100%
    acceptable_formats:
      - "[REDACTED IBAN]"
    unacceptable:
      - "GB82WEST12345698765432"  # full IBAN visible
    failure_message: "CRITICAL - IBAN exposure violates GLBA § 6802"
  
  F13_SWIFT:
    redaction_required: 100%
    acceptable_formats:
      - "[REDACTED SWIFT]"
    unacceptable:
      - "CHASUS33"  # full SWIFT visible
    failure_message: "CRITICAL - SWIFT code exposure violates GLBA § 6802"
  
  F16_BrokerageAccount:
    redaction_required: 100%
    acceptable_formats:
      - "[REDACTED ACCOUNT]"
      - "****5678"  # last 4 only
    unacceptable:
      - "X12345678"  # full account visible
    failure_message: "CRITICAL - Investment account exposure violates GLBA § 6802"
  
  F17_401k_Account:
    redaction_required: 100%
    acceptable_formats:
      - "[REDACTED ACCOUNT]"
      - "****3456"
    unacceptable:
      - "401K-123456"  # full account visible
    failure_message: "CRITICAL - 401(k) account exposure violates GLBA § 6802"
  
  F18_IRA_Account:
    redaction_required: 100%
    acceptable_formats:
      - "[REDACTED ACCOUNT]"
      - "****7654"
    unacceptable:
      - "IRA987654"  # full account visible
    failure_message: "CRITICAL - IRA account exposure violates GLBA § 6802"
  
  F22_CryptoWallet:
    redaction_required: 100%
    acceptable_formats:
      - "[REDACTED WALLET]"
    unacceptable:
      - "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"  # full wallet visible
    failure_message: "CRITICAL - Crypto wallet exposure violates GLBA § 6802"
  
  F23_LoanAccount:
    redaction_required: 100%
    acceptable_formats:
      - "[REDACTED ACCOUNT]"
      - "****6789"
    unacceptable:
      - "LOAN-123456789"  # full account visible
    failure_message: "CRITICAL - Loan account exposure violates GLBA § 6802"
  
  F24_MortgageAccount:
    redaction_required: 100%
    acceptable_formats:
      - "[REDACTED ACCOUNT]"
      - "****0123"
    unacceptable:
      - "MTG-XX-12345"  # full account visible
    failure_message: "CRITICAL - Mortgage account exposure violates GLBA § 6802"
  
  F45_OnlineBankingUsername:
    redaction_required: 100%
    acceptable_formats:
      - "[REDACTED USERNAME]"
    unacceptable:
      - "user@email.com"  # username visible
    failure_message: "CRITICAL - Login credential exposure violates GLBA Safeguards Rule"
  
  F46_Password:
    redaction_required: 100%
    acceptable_formats:
      - "[REDACTED PASSWORD]"
      - "●●●●●●●●"
    unacceptable:
      - "P@ssw0rd!"  # password visible
    failure_message: "CRITICAL - Password exposure violates GLBA Safeguards Rule"
```

### High Priority Validation (≥95% Required)

High priority identifiers should be redacted in 95%+ of instances.

```yaml
high_priority_thresholds:
  F1_F9_identity:
    redaction_required: 95%
    rationale: "Personal identity linked to financial data = NPI"
    acceptable_misses: "Generic names (company names), publicly available data"
  
  F14_F15_account_details:
    redaction_required: 95%
    rationale: "Account details reveal financial status"
    acceptable_misses: "Aggregate data, public disclosures"
  
  F25_F44_financial_data:
    redaction_required: 95%
    rationale: "Credit, income, insurance data are sensitive NPI"
    acceptable_misses: "General financial concepts (not personal)"
  
  F47_F50_security:
    redaction_required: 95%
    rationale: "Security info enables account access"
    acceptable_misses: "None - always redact"
  
  F51_F54_payment_cards:
    redaction_required: 95%
    rationale: "Card data is NPI (also PCI DSS)"
    acceptable_misses: "None for PAN - always redact"
  
  F59_F65_credit_reports:
    redaction_required: 95%
    rationale: "Credit information is explicitly NPI"
    acceptable_misses: "Aggregate credit statistics"
```

### Medium Priority Validation (≥80% Required)

Medium priority identifiers are context-dependent.

```yaml
medium_priority_thresholds:
  F19_F21_investment_values:
    redaction_required: 80%
    rationale: "Context-dependent: redact if linked to individual"
    acceptable_misses: "Aggregate data, market statistics"
  
  F55_F58_payment_card_security:
    redaction_required: 100%  # Actually NEVER STORE (PCI DSS)
    rationale: "PCI DSS prohibits storage"
    acceptable_misses: "None - NEVER STORE CVV/PIN/mag stripe"
  
  F91_F99_transactions:
    redaction_required: 80%
    rationale: "Transaction data is NPI if identifiable"
    acceptable_misses: "Generic transaction types, not linked to person"
```

---

## Overall Compliance Threshold

```yaml
compliance_status:
  COMPLIANT:
    conditions:
      critical_redacted: 100%  # No exceptions
      high_redacted: ">=95%"
      medium_redacted: ">=80%"
    status_code: "✅ GLBA COMPLIANT"
    message: "All NPI properly redacted per 15 CFR 313"
    color: green
    can_share: true
  
  PARTIAL_COMPLIANCE:
    conditions:
      critical_redacted: 100%  # Still required
      high_redacted: "80-94%"
      medium_redacted: ">=60%"
    status_code: "⚠️ GLBA PARTIAL COMPLIANCE"
    message: "Review detected identifiers - some NPI may be exposed"
    color: yellow
    can_share: false
    action_required: "Review high-priority identifiers manually"
  
  NON_COMPLIANT:
    conditions:
      critical_redacted: "<100%"
      OR:
        high_redacted: "<80%"
    status_code: "❌ GLBA NON-COMPLIANT"
    message: "CRITICAL - NPI exposure violates GLBA. Do not share document."
    color: red
    can_share: false
    action_required: "Fix critical exposures before sharing"
    legal_risk: "High - potential GLBA violation (up to $500K fine)"
```

---

## Audit Checklist

### Pre-Redaction Assessment

```markdown
Document Pre-Processing Checklist:

□ Document Type Identified
  - [ ] Bank statement
  - [ ] Tax return
  - [ ] Credit card statement
  - [ ] Loan application
  - [ ] Investment statement
  - [ ] Other: __________

□ Financial Institution Confirmed
  - [ ] Bank name/logo present
  - [ ] Account numbers visible
  - [ ] Financial data present

□ Personal Identifiers Detected
  - [ ] Name present
  - [ ] SSN present
  - [ ] Address present
  - [ ] Other personal data: __________

□ GLBA Applicability Determination
  - [ ] Contains NPI (personal + financial data)
  - [ ] GLBA applies ✅
  - [ ] GLBA does not apply (explain): __________
```

### Post-Redaction Validation

```markdown
Post-Redaction Compliance Checklist:

═══════════════════════════════════════════════════
CRITICAL IDENTIFIERS (100% Required)
═══════════════════════════════════════════════════

□ F2 (SSN)
  Found: ___ instances | Redacted: ___ | % = ___
  Status: [ ] ✅ Pass (100%) [ ] ❌ FAIL

□ F10 (Bank Account)
  Found: ___ instances | Redacted: ___ | % = ___
  Status: [ ] ✅ Pass (100%) [ ] ❌ FAIL

□ F11 (Routing Number)
  Found: ___ instances | Redacted: ___ | % = ___
  Status: [ ] ✅ Pass (100%) [ ] ❌ FAIL

□ F12 (IBAN)
  Found: ___ instances | Redacted: ___ | % = ___
  Status: [ ] ✅ Pass (100%) [ ] ❌ FAIL

□ F13 (SWIFT Code)
  Found: ___ instances | Redacted: ___ | % = ___
  Status: [ ] ✅ Pass (100%) [ ] ❌ FAIL

□ F16-F18 (Investment Accounts)
  Found: ___ instances | Redacted: ___ | % = ___
  Status: [ ] ✅ Pass (100%) [ ] ❌ FAIL

□ F22 (Crypto Wallet)
  Found: ___ instances | Redacted: ___ | % = ___
  Status: [ ] ✅ Pass (100%) [ ] ❌ FAIL

□ F23-F24 (Loan/Mortgage Accounts)
  Found: ___ instances | Redacted: ___ | % = ___
  Status: [ ] ✅ Pass (100%) [ ] ❌ FAIL

□ F45-F46 (Login Credentials)
  Found: ___ instances | Redacted: ___ | % = ___
  Status: [ ] ✅ Pass (100%) [ ] ❌ FAIL

**CRITICAL TOTAL**: ___/___  = ___%
Required: 100% | Status: [ ] ✅ PASS [ ] ❌ FAIL

═══════════════════════════════════════════════════
HIGH PRIORITY IDENTIFIERS (≥95% Required)
═══════════════════════════════════════════════════

□ F1 (Name)
  Found: ___ instances | Redacted: ___ | % = ___
  
□ F3-F9 (Personal Identity)
  Found: ___ instances | Redacted: ___ | % = ___

□ F14-F15 (Account Details)
  Found: ___ instances | Redacted: ___ | % = ___

□ F25-F31 (Credit/Loans)
  Found: ___ instances | Redacted: ___ | % = ___

□ F32-F39 (Income/Employment)
  Found: ___ instances | Redacted: ___ | % = ___

□ F40-F44 (Insurance)
  Found: ___ instances | Redacted: ___ | % = ___

□ F47-F50 (Security/Auth)
  Found: ___ instances | Redacted: ___ | % = ___

□ F51-F54 (Payment Cards)
  Found: ___ instances | Redacted: ___ | % = ___

□ F59-F65 (Credit Reports)
  Found: ___ instances | Redacted: ___ | % = ___

**HIGH PRIORITY TOTAL**: ___/___ = ___%
Required: ≥95% | Status: [ ] ✅ PASS [ ] ⚠️ PARTIAL [ ] ❌ FAIL

═══════════════════════════════════════════════════
MEDIUM PRIORITY IDENTIFIERS (≥80% Required)
═══════════════════════════════════════════════════

□ F19-F21 (Investment Values)
  Found: ___ instances | Redacted: ___ | % = ___

□ F91-F99 (Transactions)
  Found: ___ instances | Redacted: ___ | % = ___

**MEDIUM PRIORITY TOTAL**: ___/___ = ___%
Required: ≥80% | Status: [ ] ✅ PASS [ ] ⚠️ PARTIAL [ ] ❌ FAIL

═══════════════════════════════════════════════════
OVERALL COMPLIANCE
═══════════════════════════════════════════════════

Total Identifiers Found: _______
Total Identifiers Redacted: _______
Overall Redaction Rate: _______%

Compliance Status:
[ ] ✅ COMPLIANT (Critical 100%, High ≥95%, Medium ≥80%)
[ ] ⚠️ PARTIAL (Critical 100%, High 80-94%)
[ ] ❌ NON-COMPLIANT (Critical <100% OR High <80%)

Approved for Sharing: [ ] YES [ ] NO

Reviewer: _________________ Date: _______
```

---

## Compliance Report Template

```markdown
════════════════════════════════════════════════════════
GLBA COMPLIANCE REPORT
════════════════════════════════════════════════════════

Document: {{document_type}}
Date: {{timestamp}}
Processor: ShieldText {{version}}
Session ID: {{session_id}}

────────────────────────────────────────────────────────
DOCUMENT ANALYSIS
────────────────────────────────────────────────────────

Document Type: {{document_type}}
File Size: {{file_size}}
Pages: {{page_count}}
Processing Time: {{processing_time_ms}}ms

GLBA Applicability: {{glba_applicable}}
{{#if glba_applicable}}
Reason: Contains NPI (personal identifiers + financial data)
{{else}}
Reason: {{not_applicable_reason}}
{{/if}}

────────────────────────────────────────────────────────
IDENTIFIERS DETECTED
────────────────────────────────────────────────────────

Critical Identifiers: {{critical_count}} types, {{critical_instances}} instances
High Priority: {{high_count}} types, {{high_instances}} instances
Medium Priority: {{medium_count}} types, {{medium_instances}} instances

Total: {{total_types}} types, {{total_instances}} instances

Breakdown by Category:
  • Personal Identity (F1-F9): {{identity_instances}} instances
  • Account Numbers (F10-F24): {{account_instances}} instances
  • Credit/Loans (F25-F31): {{credit_instances}} instances
  • Income/Employment (F32-F39): {{income_instances}} instances
  • Insurance (F40-F44): {{insurance_instances}} instances
  • Authentication (F45-F50): {{auth_instances}} instances
  • Payment Cards (F51-F58): {{card_instances}} instances
  • Credit Reports (F59-F65): {{credit_report_instances}} instances
  • Transactions (F91-F99): {{transaction_instances}} instances

────────────────────────────────────────────────────────
REDACTION SUMMARY
────────────────────────────────────────────────────────

Critical Identifiers:
  Found: {{critical_found}}
  Redacted: {{critical_redacted}}
  Rate: {{critical_percent}}%
  Status: {{#if critical_100}}✅ PASS{{else}}❌ FAIL{{/if}}

High Priority Identifiers:
  Found: {{high_found}}
  Redacted: {{high_redacted}}
  Rate: {{high_percent}}%
  Status: {{#if high_95}}✅ PASS{{else if high_80}}⚠️ PARTIAL{{else}}❌ FAIL{{/if}}

Medium Priority Identifiers:
  Found: {{medium_found}}
  Redacted: {{medium_redacted}}
  Rate: {{medium_percent}}%
  Status: {{#if medium_80}}✅ PASS{{else if medium_60}}⚠️ PARTIAL{{else}}❌ FAIL{{/if}}

────────────────────────────────────────────────────────
COMPLIANCE STATUS
────────────────────────────────────────────────────────

{{status_icon}} GLBA (Gramm-Leach-Bliley Act)
   Status: {{compliance_status}}
   {{compliance_message}}
   
   Requirements Met:
   {{#if critical_100}}✅{{else}}❌{{/if}} Critical NPI protected (100% required)
   {{#if high_95}}✅{{else if high_80}}⚠️{{else}}❌{{/if}} High-priority NPI secured (≥95% required)
   {{#if medium_80}}✅{{else if medium_60}}⚠️{{else}}❌{{/if}} Medium-priority data protected (≥80% required)
   
   Coverage: 15 CFR 313 (Privacy Rule), 16 CFR 314 (Safeguards Rule)
   Enforced by: FTC, Federal Banking Agencies
   
   {{#if non_compliant}}
   ⚠️ ACTION REQUIRED:
   {{action_required_message}}
   Legal Risk: {{legal_risk}}
   {{/if}}

────────────────────────────────────────────────────────
SHARING RECOMMENDATION
────────────────────────────────────────────────────────

{{#if compliant}}
✅ SAFE TO SHARE
This document has been properly redacted and complies with GLBA requirements.
All NPI (Nonpublic Personal Information) has been secured.
{{else}}
❌ DO NOT SHARE
This document contains exposed NPI and does not meet GLBA requirements.
Sharing this document may violate GLBA and result in penalties.

Next Steps:
1. Review identified exposures
2. Re-run redaction
3. Verify 100% critical identifier redaction
{{/if}}

────────────────────────────────────────────────────────
PROCESSING DETAILS
────────────────────────────────────────────────────────

Device: {{device_info}}
Model: {{model_name}} ({{model_type}})
Processing: On-device (zero cloud transmission)
Version: ShieldText v{{version}}
Session ID: {{session_id}}
Timestamp: {{timestamp}}

Data Security:
✅ All processing performed on-device
✅ No data transmitted to cloud
✅ Original document not retained
✅ Redacted output only

────────────────────────────────────────────────────────
AUDIT TRAIL
────────────────────────────────────────────────────────

{{#each redaction_log}}
[{{timestamp}}] {{action}}: {{identifier_type}} ({{identifier_id}})
  Location: {{location}}
  Original: {{original_value}}
  Redacted: {{redacted_value}}
{{/each}}

────────────────────────────────────────────────────────
CERTIFICATION
────────────────────────────────────────────────────────

This document has been processed using ShieldText's on-device redaction
engine and {{#if compliant}}complies{{else}}does not comply{{/if}} with 
GLBA data protection requirements.

The original document was not transmitted to any third-party service or
cloud infrastructure per GLBA Safeguards Rule (16 CFR 314).

For audit purposes, this report can be saved alongside the redacted document.

Report Generated: {{report_timestamp}}

════════════════════════════════════════════════════════
```

---

## Testing Scenarios

### Test Case 1: Bank Statement (Should PASS)

**Input**:
```
Account holder: John Smith
Account Number: 1234567890
Routing Number: 021000021
Address: 123 Main St, San Francisco, CA 94102
Balance: $5,432.12
```

**Expected Output**:
```
Account holder: [REDACTED NAME]
Account Number: ********7890
Routing Number: ****0021
Address: [REDACTED ADDRESS]
Balance: [REDACTED BALANCE]
```

**Expected Compliance**:
```
Critical: 2/2 (100%) ✅
High: 2/2 (100%) ✅
Medium: 1/1 (100%) ✅
Status: ✅ GLBA COMPLIANT
```

---

### Test Case 2: Missing SSN Redaction (Should FAIL)

**Input**:
```
Name: John Smith
SSN: 123-45-6789
Account: 1234567890
```

**Output (WRONG)**:
```
Name: [REDACTED NAME]
SSN: 123-45-6789  ← NOT REDACTED!
Account: ********7890
```

**Expected Compliance**:
```
Critical: 1/2 (50%) ❌ FAIL
High: 1/1 (100%) ✅
Status: ❌ GLBA NON-COMPLIANT
Error: "CRITICAL - SSN exposure violates GLBA § 6802"
Action: Do not share - fix SSN redaction
```

---

### Test Case 3: Partial High-Priority Redaction (Should be PARTIAL)

**Input**:
10 personal identifiers (F1-F9, F14)

**Output**:
9 redacted, 1 missed (90% redaction rate)

**Expected Compliance**:
```
Critical: N/A (none present)
High: 9/10 (90%) ⚠️ PARTIAL (need ≥95%)
Status: ⚠️ GLBA PARTIAL COMPLIANCE
Action: Review missed high-priority identifier
```

---

## Integration with Other Regulations

When validating GLBA, also check:

**PCI DSS**: If payment card data present (F51-F58)
- More strict than GLBA for card data
- Use PCI DSS validation rules for F51-F58

**CCPA**: For California residents
- GLBA data is exempt from CCPA, but apply both for safety
- Use stricter standard

**SOX**: For corporate documents
- GLBA applies to employee personal financial data
- SOX applies to corporate governance
- Validate both independently

---

## References

- **Privacy Rule**: 15 CFR Part 313
- **Safeguards Rule**: 16 CFR Part 314
- **NPI Definition**: 15 CFR § 313.3(n)
- **FTC Enforcement**: https://www.ftc.gov/enforcement

---

## Document History

**Last Updated**: April 30, 2026  
**Version**: 1.0  
**Changes**: Initial validation rules for ShieldText GLBA compliance
