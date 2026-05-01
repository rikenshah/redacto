# GLBA - Identifiers Covered

## Summary

GLBA protects **"Nonpublic Personal Information" (NPI)** which includes personally identifiable financial information collected by or about consumers in connection with financial products or services.

**Total identifiers covered**: 50+ from personal finance taxonomy (F1-F50 and selected others)

---

## NPI Categories

Per 15 CFR § 313.3(n), NPI includes:

1. **Information Consumer Provides**: Data given to obtain financial product/service
2. **Transaction Information**: Data resulting from transactions  
3. **Other Financial Information**: Data obtained in connection with providing service

---

## Critical Identifiers (Must Always Redact - 100%)

These identifiers provide direct access to financial accounts or are highly sensitive under GLBA.

| ID | Identifier | Why Critical | GLBA Citation | Example |
|----|-----------|--------------|---------------|---------|
| **F2** | Social Security Number | Direct identifier, enables identity theft | §313.3(n)(1) | 123-45-6789 |
| **F10** | Bank Account Number | Direct account access | §313.3(n)(2) | Acct: 1234567890 |
| **F11** | Routing Number | Enables unauthorized transfers | §313.3(n)(2) | RTN: 021000021 |
| **F12** | IBAN | International account access | §313.3(n)(2) | GB82WEST12345... |
| **F13** | SWIFT Code | International banking identifier | §313.3(n)(2) | CHASUS33 |
| **F16** | Brokerage Account # | Direct investment account access | §313.3(n)(2) | X12345678 |
| **F17** | 401(k) Account # | Retirement account access | §313.3(n)(2) | 401K-123456 |
| **F18** | IRA Account # | Retirement account access | §313.3(n)(2) | IRA987654 |
| **F22** | Crypto Wallet Address | Direct cryptocurrency access | §313.3(n)(2) | 1A1zP1e... |
| **F23** | Loan Account # | Loan account access | §313.3(n)(2) | LOAN-123456 |
| **F24** | Mortgage Loan # | Mortgage account access | §313.3(n)(2) | MTG-XX-12345 |
| **F45** | Online Banking Username | Account access credential | §313.3(n)(2) | user@email.com |
| **F46** | Password/PIN | Account access credential | §313.3(n)(2) | ●●●●●●●● |

**Redaction Rule**: ALWAYS redact completely or show last 4 digits only (where appropriate).

**GLBA Violation if Exposed**: Yes - unauthorized disclosure of NPI

---

## High Priority Identifiers (≥95% Redaction)

These identifiers are NPI when linked to financial accounts or services.

### Personal Identity (F1-F9)

When combined with financial data, personal identifiers become NPI.

| ID | Identifier | GLBA Status | Redaction Strategy |
|----|-----------|-------------|-------------------|
| **F1** | Full Name | NPI when + financial data | [REDACTED NAME] |
| **F3** | Date of Birth | NPI when + financial data | [REDACTED DOB] |
| **F4** | Home Address | NPI on financial documents | [REDACTED ADDRESS] |
| **F5** | Mailing Address | NPI on financial documents | [REDACTED ADDRESS] |
| **F6** | Phone Number | NPI when + financial data | ***-***-0199 |
| **F7** | Email Address | NPI when + financial data | ***@domain.com |
| **F8** | Driver's License # | NPI on loan applications | [REDACTED DL] |
| **F9** | Passport Number | NPI on intl banking docs | [REDACTED PASSPORT] |

**Context**: These become NPI under GLBA when appearing on financial documents (bank statements, loan applications, etc.)

### Account Details (F14-F15)

| ID | Identifier | GLBA Status | Redaction Strategy |
|----|-----------|-------------|-------------------|
| **F14** | Account Balance | NPI - transaction result | [REDACTED BALANCE] |
| **F15** | Account Opening Date | NPI - account information | [REDACTED DATE] |

### Credit & Loans (F25-F31)

| ID | Identifier | GLBA Status | Redaction Strategy |
|----|-----------|-------------|-------------------|
| **F25** | Credit Score | NPI - credit information | [REDACTED SCORE] |
| **F26** | Credit Limit | NPI - credit information | [REDACTED LIMIT] |
| **F27** | Outstanding Balance | NPI - account information | [REDACTED BALANCE] |
| **F28** | Interest Rate (personal) | NPI - account terms | [REDACTED RATE] |
| **F29** | Payment History | NPI - transaction information | [REDACTED HISTORY] |
| **F30** | Auto Loan # | NPI - account number | ****3456 |
| **F31** | Student Loan # | NPI - account number | ****6789 |

### Income & Employment (F32-F39)

When on financial applications or records.

| ID | Identifier | GLBA Status | Redaction Strategy |
|----|-----------|-------------|-------------------|
| **F32** | Current Employer | NPI on loan applications | [REDACTED EMPLOYER] |
| **F33** | Salary/Wage | NPI - financial information | [REDACTED SALARY] |
| **F34** | Bonus Amount | NPI - financial information | [REDACTED BONUS] |
| **F35** | W-2 Box 1 (Wages) | NPI - income information | [REDACTED] |
| **F36** | W-2 Box 2 (Tax) | NPI - tax information | [REDACTED] |
| **F37** | W-2 Employer EIN | Business identifier | [REDACTED EIN] |
| **F38** | 1099 Income | NPI - income information | [REDACTED INCOME] |
| **F39** | Direct Deposit Account | NPI - account number | [REDACTED ACCOUNT] |

### Insurance (F40-F44)

Insurance information is explicitly covered by GLBA.

| ID | Identifier | GLBA Status | Redaction Strategy |
|----|-----------|-------------|-------------------|
| **F40** | Policy Number | NPI - account identifier | ****6789 |
| **F41** | Beneficiary Name | NPI - policy information | [REDACTED BENEFICIARY] |
| **F42** | Coverage Amount | NPI - policy terms | [REDACTED COVERAGE] |
| **F43** | Premium Amount | NPI - financial information | [REDACTED PREMIUM] |
| **F44** | Claim Number | NPI - transaction identifier | [REDACTED CLAIM] |

### Authentication & Security (F47-F50)

| ID | Identifier | GLBA Status | Redaction Strategy |
|----|-----------|-------------|-------------------|
| **F47** | Security Questions | NPI - access control | [REDACTED SECURITY INFO] |
| **F48** | 2FA Codes | NPI - access control | [REDACTED CODE] |
| **F49** | Recovery Phone | NPI - account recovery | [REDACTED PHONE] |
| **F50** | Backup Email | NPI - account recovery | [REDACTED EMAIL] |

---

## Medium Priority Identifiers (≥80% Redaction)

Context-dependent: Redact if personally identifiable or linked to individual accounts.

### Investment Data (F19-F21)

| ID | Identifier | When It's NPI | Redaction Strategy |
|----|-----------|---------------|-------------------|
| **F19** | Portfolio Value | When linked to individual | [REDACTED VALUE] |
| **F20** | Specific Holdings | When linked to individual | [REDACTED HOLDINGS] |
| **F21** | Dividend Income | When linked to individual | [REDACTED INCOME] |

**Preserve if**: Aggregate data, not linked to specific person

### Transaction Data (F91-F99)

| ID | Identifier | When It's NPI | Redaction Strategy |
|----|-----------|---------------|-------------------|
| **F91** | Transaction ID | When linked to person | [REDACTED TXN ID] |
| **F92** | Check Number | Personal checks | [REDACTED CHECK #] |
| **F93** | Wire Transfer # | When linked to person | [REDACTED WIRE #] |
| **F94** | ACH Trace # | When linked to person | [REDACTED ACH #] |
| **F95** | Invoice # (with amount) | When linked to person | [REDACTED INVOICE] |
| **F96** | Receipt # | Personal receipts | [REDACTED RECEIPT] |
| **F97** | Transaction Amount | When identifiable | [REDACTED AMOUNT] |
| **F98** | Transaction Date | When identifiable | [REDACTED DATE] |
| **F99** | Merchant Name | Behavioral pattern | Context-dependent |

**Preserve if**: Generic business transaction data, not personally identifiable

---

## Payment Card Data (F51-F58)

**Note**: Also covered by PCI DSS (stricter rules apply)

| ID | Identifier | GLBA + PCI DSS Status | Redaction Strategy |
|----|-----------|----------------------|-------------------|
| **F51** | Credit Card PAN | NPI + CHD (Cardholder Data) | **** **** **** 1234 |
| **F52** | Cardholder Name | NPI + CHD | [REDACTED CARDHOLDER] |
| **F53** | Expiration Date | NPI + CHD | [REDACTED] or **/** |
| **F54** | Service Code | NPI + CHD | [REDACTED] |
| **F55** | CVV/CVC | **NEVER STORE** (PCI DSS) | [REDACTED CVV] |
| **F56** | Magnetic Stripe | **NEVER STORE** (PCI DSS) | [REDACTED] |
| **F57** | Chip Data | **NEVER STORE** (PCI DSS) | [REDACTED] |
| **F58** | PIN | **NEVER STORE** (PCI DSS) | [REDACTED PIN] |

---

## Credit Report Data (F59-F65)

Credit information is explicitly NPI under GLBA.

| ID | Identifier | GLBA Status | Redaction Strategy |
|----|-----------|-------------|-------------------|
| **F59** | Credit Report # | NPI - report identifier | [REDACTED REPORT #] |
| **F60** | Credit Score (FICO) | NPI - credit information | [REDACTED SCORE] |
| **F61** | VantageScore | NPI - credit information | [REDACTED SCORE] |
| **F62** | Tradeline Details | NPI - credit history | [REDACTED TRADELINE] |
| **F63** | Hard Inquiries | NPI - credit activity | [REDACTED INQUIRY] |
| **F64** | Collections Account | NPI - credit history | [REDACTED COLLECTION] |
| **F65** | Public Records | NPI - credit history | [REDACTED PUBLIC RECORD] |

---

## Document-Specific Coverage

### Bank Statement

**Critical NPI**:
- F1 (Name), F4 (Address), F10 (Account #), F11 (Routing #)

**High Priority NPI**:
- F6 (Phone), F7 (Email), F14 (Balance)

**Medium Priority NPI**:
- F91-F97 (Transaction details)

**Why**: Bank statement contains account holder info + account data + transaction history = all NPI

### Tax Return (W-2, 1040)

**Critical NPI**:
- F2 (SSN - multiple instances)

**High Priority NPI**:
- F1 (Name), F4 (Address), F32-F39 (Income data)

**Why**: Tax returns contain comprehensive personal financial information = NPI

### Credit Card Statement

**Critical NPI**:
- F51 (PAN), F10 (Payment account if linked)

**High Priority NPI**:
- F1 (Name), F4-F5 (Addresses), F14 (Balance), F27 (Outstanding balance)

**Medium Priority NPI**:
- F97 (Transaction amounts), F99 (Merchant names if pattern)

**Why**: Contains cardholder data + account information + spending patterns = NPI

### Loan Application

**Critical NPI**:
- F2 (SSN), F10-F11 (Bank accounts), F23-F24 (Existing loans)

**High Priority NPI**:
- F1-F9 (Full personal profile), F25-F31 (Credit history), F32-F33 (Income), F68-F75 (Assets)

**Why**: Comprehensive financial application = extensive NPI collection

### Investment Statement (401k, Brokerage)

**Critical NPI**:
- F16-F18 (Investment account numbers), F22 (Crypto if applicable)

**High Priority NPI**:
- F1 (Name), F4 (Address), F19-F21 (Holdings, values)

**Medium Priority NPI**:
- F41 (Beneficiaries)

**Why**: Investment accounts + holdings + values = NPI

---

## Full Coverage Matrix

```
GLBA Coverage Summary:
├─ CRITICAL (13): F2, F10-F13, F16-F18, F22-F24, F45-F46
├─ HIGH (37): F1, F3-F9, F14-F15, F25-F44, F47-F50, F51-F54, F59-F65
├─ MEDIUM (15): F19-F21, F55-F58, F91-F99
└─ TOTAL: 65 identifiers covered by GLBA
```

---

## What is NOT NPI (Preserve These)

### Public Information
- Publicly available phone numbers (business listings)
- Published information (news articles, government records)
- Company names (unless sole proprietor)

### Aggregate Data
- "Average account balance: $5,000" (not individual)
- "Total customers: 10,000" (not identifiable)

### Generic Company Information
- Bank name and branch address
- Customer service phone numbers (1-800-...)
- Generic company emails (support@company.com)

---

## Compliance Validation

See `VALIDATION.md` for detailed validation rules and thresholds.

**Quick Summary**:
- **CRITICAL** identifiers: 100% redaction required
- **HIGH** priority: ≥95% redaction required
- **MEDIUM** priority: ≥80% redaction required

**Status**:
- ✅ COMPLIANT: All thresholds met
- ⚠️ PARTIAL: Critical 100%, High 80-94%
- ❌ NON-COMPLIANT: Any critical identifier exposed OR high <80%

---

## References

- **NPI Definition**: 15 CFR § 313.3(n)
- **Privacy Rule**: 15 CFR Part 313
- **Safeguards Rule**: 16 CFR Part 314
- **FTC Guidance**: "How to Comply with the Privacy Rule"

---

## Document History

**Last Updated**: April 30, 2026  
**Version**: 1.0  
**Changes**: Initial identifier mapping for ShieldText
