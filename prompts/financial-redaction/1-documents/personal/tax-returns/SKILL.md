---
name: tax-returns
description: Redacts personal tax documents (W-2, 1040, 1099 forms) for safe sharing with accountants, tax preparers, financial advisors, or lenders. Removes SSN, EIN, names, addresses, and income amounts while preserving document structure and employer information. Ensures IRS privacy compliance and GLBA/FCRA alignment. Use when processing W-2 forms, 1040 tax returns, 1099 forms, wage statements, or when the user mentions tax documents, tax returns, W-2, 1099, IRS forms, or income statements.
---

# Tax Return Redaction (W-2, 1040, 1099)

## Purpose

Redact personal tax documents (W-2, 1040, 1099) for sharing with accountants, tax preparers, financial advisors, or lenders.

## Document Types

### W-2 (Wage and Tax Statement)
Most common - received annually from employers

### 1040 (Individual Income Tax Return)
Full tax return filed with IRS

### 1099 Forms
- 1099-MISC (Miscellaneous Income)
- 1099-NEC (Non-Employee Compensation)
- 1099-INT (Interest Income)

---

## What Gets Redacted

### Critical (Always)
- **SSN** (Employee): Box a on W-2, top of 1040
- **SSN** (Spouse): If married filing jointly
- **SSN** (Dependents): Children's SSNs

### High Priority
- **Name**: Employee/taxpayer name
- **Address**: Home address
- **Employer EIN**: Box b on W-2
- **All Income Amounts**: Boxes 1-20 on W-2, all 1040 line items
- **Bank Account Info**: Direct deposit (routing + account)

### Preserve
- Tax year
- Form type (W-2, 1040, etc.)
- General tax concepts

---

## Required Identifiers

```yaml
critical:
  - F2: SSN (Box a, 1040 top line)
  
high:
  - F1: Name
  - F4: Address
  - F32-F39: Income data (all boxes)
  - F37: Employer EIN
```

---

## W-2 Example

**Before Redaction**:
```
Box a: 123-45-6789 (Employee SSN)
Box b: 12-3456789 (Employer EIN)
Box e: John Michael Smith
Box f: 123 Main St, San Francisco, CA
Box 1: Wages $75,000
Box 2: Federal tax $15,000
```

**After Redaction**:
```
Box a: ***-**-6789
Box b: [REDACTED EIN]
Box e: [REDACTED NAME]
Box f: [REDACTED ADDRESS]
Box 1: [REDACTED INCOME]
Box 2: [REDACTED TAX]
```

---

## Compliance

**IRS**: Tax data protection  
**GLBA**: Personal financial information  
**CCPA**: Personal identifiers

**Validation**: Critical 100%, High 95%+

---

## Use Cases

1. **Sending to Accountant**: Redact SSN, keep income for tax prep
2. **Loan Application**: Redact SSN, keep income to prove earnings
3. **Background Check**: Redact income, keep employment dates

---

## Status

**Built**: Simplified version  
**Next**: Medical/HIPAA (main focus)
