---
name: financial-redaction
description: Redacts personal and corporate financial information ensuring GLBA, PCI DSS, SOX, and regulatory compliance. Detects and removes account numbers, SSNs, credit card numbers, routing numbers, transaction data, income details, and sensitive financial identifiers from bank statements, tax returns, credit reports, loan applications, and corporate documents. Use when processing financial documents, bank statements, W-2 forms, credit cards, or when the user mentions GLBA, PCI DSS, financial privacy, or financial redaction.
---

# Financial Redaction Skill

## Purpose

This skill teaches the LLM how to redact financial documents for regulatory compliance while preserving necessary financial context.

**Primary goal**: Remove all personally identifiable financial information (PII) but keep financial data needed for accounting, auditing, or business purposes.

## Supported Regulations

- **GLBA** (Gramm-Leach-Bliley Act): Personal financial data protection
- **PCI DSS** (Payment Card Industry): Payment card data security
- **SOX** (Sarbanes-Oxley): Corporate governance and fraud prevention
- **FCRA** (Fair Credit Reporting Act): Credit reporting privacy
- **IRS Regulations**: Tax data protection
- **CCPA/CPRA**: California consumer privacy

---

## LLM Workflow

When the LLM receives financial text, follow these steps:

### Step 1: Identify Document Type
Determine which type of financial document this is:
- **Bank Statement** → Use `1-documents/personal/bank-statements/`
- **Tax Return** (W-2, 1040, 1099) → Use `1-documents/personal/tax-returns/`
- **Credit Card Statement** → Use `1-documents/personal/credit-cards/` (to be built)
- **Credit Report** → Use `1-documents/personal/credit-reports/` (to be built)

### Step 2: Detect Identifiers
Find ALL identifiers in the text using:
- **F1-F9** (Personal identity) → Names, SSN, DOB, addresses, phone, email, DL, passport
- **F10-F24** (Account identifiers) → Account numbers, routing numbers, IBAN (to be built)
- **F51-F58** (Payment cards) → Credit card numbers, CVV, expiration dates (to be built)
- **F91-F105** (Transactions) → Transaction details, amounts, dates (to be built)

See `2-identifiers/` for detection patterns.

### Step 3: Redact PII, Preserve Financial Context
- Replace F1-F105 identifiers with `[REDACTED]` or placeholders
- Keep financial data relevant for accounting/business purposes
- Maintain document utility

### Step 4: Validate
Confirm no PII remains using rules in `3-compliance/GLBA/VALIDATION.md`

---

## Example: Bank Statement Redaction

### Use Case
User needs to share bank statement with accountant but wants to protect personal information.

**INPUT**:
```
Chase Bank Statement
Account Holder: Sarah Johnson
SSN: XXX-XX-6789
Account #: 1234567890
Routing #: 021000021
Address: 456 Oak Street, Springfield, IL 62701
Phone: (555) 123-4567

Date        Description              Debit      Credit    Balance
01/15/2024  Direct Deposit - ACME    --         $3,500    $5,234
01/16/2024  ATM Withdrawal           $200       --        $5,034
01/18/2024  Starbucks #4521          $6.45      --        $5,027.55
```

**LLM DETECTS**:
- F1 (Name): "Sarah Johnson" → REDACT
- F2 (SSN): "XXX-XX-6789" → REDACT
- F4 (Address): "456 Oak Street, Springfield, IL 62701" → REDACT
- F6 (Phone): "(555) 123-4567" → REDACT
- F10 (Account #): "1234567890" → REDACT
- F11 (Routing #): "021000021" → REDACT
- Financial Context: Bank name, transaction descriptions, amounts, balances → PRESERVE

**OUTPUT**:
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
01/18/2024  Starbucks #4521          $6.45      --        $5,027.55

✅ GLBA Compliant - All personal identifiers redacted
✅ Financial context preserved for accounting purposes
```

---

## Available Skills

### Document Types (what to redact/preserve per document)
- `1-documents/personal/bank-statements/SKILL.md` ✅
- `1-documents/personal/tax-returns/SKILL.md` ✅
- Additional document types (to be built)

### Identifiers (what to look for)
- `2-identifiers/personal/F1-F9-identity/SKILL.md` ✅ - Personal identity (name, SSN, DOB, address, phone, email, DL, passport)
- Additional identifier categories (to be built):
  - F10-F24: Account identifiers
  - F51-F58: Payment cards
  - F91-F105: Transactions

### Compliance (validation rules)
- `3-compliance/GLBA/REGULATION.md` ✅
- `3-compliance/GLBA/IDENTIFIERS.md` ✅
- `3-compliance/GLBA/VALIDATION.md` ✅
- Additional regulations (to be built):
  - PCI DSS (payment cards)
  - FCRA (credit reports)
  - IRS (tax documents)

---

## Status

**Current Phase**: Foundation - Personal Finance
- ✅ Master skill (this file)
- ✅ F1-F9 identity identifiers
- ✅ GLBA compliance framework
- ✅ Bank statements document skill
- ✅ Tax returns document skill

**Next Phase**: Expand identifier coverage
- Build F10-F24 (account identifiers)
- Build F51-F58 (payment cards)
- Build F91-F105 (transactions)
