# Financial Redaction Skills for LLM

## Purpose

These skills teach your LLM how to redact financial documents for regulatory compliance (GLBA, PCI DSS, etc.) while preserving necessary financial context.

---

## Structure

```
financial-redaction/
│
├── SKILL.md                            ← Master skill - LLM workflow
├── README.md                           ← This file
│
├── 1-documents/personal/               ← Document-specific redaction rules
│   ├── bank-statements/SKILL.md        ← Bank statements
│   └── tax-returns/SKILL.md            ← W-2, 1040, 1099 forms
│
├── 2-identifiers/personal/             ← What to detect
│   └── F1-F9-identity/                 ← Personal identity (name, SSN, DOB, etc.)
│
└── 3-compliance/GLBA/                  ← Validation rules
    ├── REGULATION.md                   ← GLBA overview
    ├── IDENTIFIERS.md                  ← 65 financial identifiers
    └── VALIDATION.md                   ← Compliance checking
```

---

## How It Works

### 4-Step LLM Process

**1. CLASSIFY** → Determine document type
- Bank statement? Tax return? Credit card statement?
- Load appropriate document skill

**2. DETECT** → Find identifiers
- F1-F9 (Identity) → Names, SSN, DOB, addresses, phone numbers, etc.
- F10-F24 (Accounts) → Account numbers, routing numbers (to be built)
- F51-F58 (Payment cards) → Credit card numbers, CVV (to be built)

**3. REDACT** → Transform text
- Replace F1-F105 with `[REDACTED]` placeholders
- Keep financial data needed for accounting/business

**4. VALIDATE** → Confirm compliance
- Check: No PII remains
- Result: ✅ GLBA COMPLIANT or list violations

---

## Example: Bank Statement

### Use Case
User shares bank statement with accountant but needs to protect personal information.

**BEFORE (PII EXPOSED)**:
```
Chase Bank Statement
Account Holder: Sarah Johnson
SSN: XXX-XX-6789
Account #: 1234567890
Routing #: 021000021
Address: 456 Oak St, Springfield, IL 62701
Phone: (555) 123-4567

Date        Description              Debit      Credit    Balance
01/15/2024  Direct Deposit - ACME    --         $3,500    $5,234
01/16/2024  ATM Withdrawal           $200       --        $5,034

❌ 6 personal identifiers exposed
```

**LLM ANALYSIS**:
```
REDACT (F1-F9 Identity):
- "Sarah Johnson" (F1: Name)
- "XXX-XX-6789" (F2: SSN)
- "456 Oak St, Springfield, IL 62701" (F4: Address)
- "(555) 123-4567" (F6: Phone)
- "1234567890" (F10: Account number)
- "021000021" (F11: Routing number)

PRESERVE (Financial Context):
- Bank name (Chase Bank)
- Transaction descriptions
- Amounts ($3,500, $200, etc.)
- Dates
- Balances
```

**AFTER (GLBA COMPLIANT)**:
```
Chase Bank Statement
Account Holder: [REDACTED NAME]
SSN: [REDACTED]
Account #: [REDACTED]
Routing #: [REDACTED]
Address: [REDACTED ADDRESS]
Phone: [REDACTED PHONE]

Date        Description              Debit      Credit    Balance
01/15/2024  Direct Deposit - ACME    --         $3,500    $5,234
01/16/2024  ATM Withdrawal           $200       --        $5,034

✅ All PII redacted
✅ Financial context preserved
✅ GLBA compliant
```

---

## What Gets Redacted vs Preserved

### 🔴 REDACT (F1-F105 PII)
Personal identifiers that can identify the account holder:

**F1-F9 (Identity)**
- Names
- Social Security Numbers (SSN)
- Date of Birth
- Addresses
- Phone numbers
- Email addresses
- Driver's License numbers
- Passport numbers

**F10-F24 (Account Identifiers)** - To be built
- Account numbers
- Routing numbers
- IBAN/SWIFT codes

**F51-F58 (Payment Cards)** - To be built
- Credit/debit card numbers (PAN)
- CVV codes
- Card expiration dates

### 🟢 PRESERVE (Financial Context)
Financial data needed for accounting, auditing, or business purposes:

- Bank/institution names
- Transaction descriptions
- Transaction amounts
- Transaction dates
- Account balances
- Interest rates
- Fee descriptions
- Statement periods
- Employer names (on W-2s)
- Income amounts

---

## Regulations Covered

### GLBA (Gramm-Leach-Bliley Act) ✅
- Personal financial data protection
- Applies to banks, credit unions, insurance companies, investment firms
- Covers "Nonpublic Personal Information" (NPI)
- **Status**: Complete compliance framework

### Future Regulations (To Be Built)
- **PCI DSS**: Payment card data security
- **FCRA**: Fair Credit Reporting Act (credit reports)
- **IRS**: Tax data protection
- **CCPA**: California consumer privacy

---

## Skill Reference

### Master Skill
- `SKILL.md` - Overall LLM workflow and process

### Document Types (Layer 1)
What to redact/preserve for each document type:
- `1-documents/personal/bank-statements/SKILL.md` ✅ - Bank statements
- `1-documents/personal/tax-returns/SKILL.md` ✅ - W-2, 1040, 1099

### Identifiers (Layer 2)
What to detect in the text:
- `2-identifiers/personal/F1-F9-identity/SKILL.md` ✅ - Personal identity

### Compliance (Layer 3)
Validation and regulatory reference:
- `3-compliance/GLBA/REGULATION.md` ✅ - GLBA overview
- `3-compliance/GLBA/IDENTIFIERS.md` ✅ - All 65 GLBA identifiers
- `3-compliance/GLBA/VALIDATION.md` ✅ - Compliance checking

---

## Why This Architecture?

**Layer 1 (Documents)**: Document-specific rules
- Each document type has unique redaction needs
- Example: Bank statements preserve transaction history, tax forms preserve income data

**Layer 2 (Identifiers)**: Reusable detection patterns
- Same identifiers used across all document types
- F1-F9 (identity) applies to bank statements, tax returns, credit reports, etc.

**Layer 3 (Compliance)**: Regulatory framework
- GLBA rules don't change per document
- Centralized validation logic

---

## Current Status

✅ **Foundation Complete**
- Master skill with LLM workflow
- F1-F9 identity identifiers (9 identifiers)
- GLBA compliance framework (65 identifiers covered)
- 2 document types (bank statements, tax returns)

🚧 **Next Phase**
- Build F10-F24 (account identifiers)
- Build F51-F58 (payment card identifiers)
- Add PCI DSS compliance
- Add credit card statement document type

---

## Summary

✅ **Personal finance foundation** - Bank statements and tax returns  
✅ **9 identity identifiers** - F1-F9 (name, SSN, DOB, address, phone, email, DL, passport)  
✅ **GLBA compliant** - Full regulatory framework  
✅ **Production ready** - Skills ready for LLM implementation

**These skills teach your LLM exactly what to look for and what to redact in financial documents.**
