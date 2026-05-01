---
name: financial-identity
description: Defines 9 personal identity identifiers (F1-F9) for financial document redaction including full name, SSN, date of birth, addresses, phone, email, driver's license, and passport. Provides regex detection patterns, LLM validation prompts, and redaction strategies for GLBA, CCPA, IRS, FCRA, and SOX compliance. Use when implementing identity detection in financial documents, validating personal identifiers, or building redaction logic for bank statements, tax returns, credit reports, or loan applications.
---

# Identity Identifiers (F1-F9)

## Overview

Personal identity information that can identify an individual in financial contexts.

**Total Identifiers**: 9 (F1-F9)  
**Used By**: GLBA, CCPA, IRS, FCRA, SOX (executive personal data)  
**Priority**: CRITICAL to HIGH

## Covered Identifiers

| ID | Identifier | Format Examples | Priority | Redaction Strategy |
|----|-----------|-----------------|----------|-------------------|
| **F1** | Full Legal Name | John Michael Smith, Jane Q. Doe | HIGH | [REDACTED NAME] |
| **F2** | Social Security Number | 123-45-6789, 987654321 | CRITICAL | ***-**-1234 (last 4) |
| **F3** | Date of Birth | 12/15/1985, DOB: 1985-12-15 | HIGH | [REDACTED DOB] |
| **F4** | Home Address | 123 Main St, Apt 4B, City, ST 12345 | HIGH | [REDACTED ADDRESS] |
| **F5** | Mailing Address | PO Box 456, City, ST 12345 | HIGH | [REDACTED ADDRESS] |
| **F6** | Phone Number | 555-0199, (408) 555-0101 | MEDIUM | ***-***-0199 |
| **F7** | Email Address | john.smith@email.com | MEDIUM | ***@email.com |
| **F8** | Driver's License # | DL: A1234567, CA D1234567 | HIGH | [REDACTED DL] |
| **F9** | Passport Number | 123456789, PP: X12345678 | HIGH | [REDACTED PASSPORT] |

---

## Detection Patterns

### F1 - Full Legal Name

**Detection Methods**:
1. **Context-based** (preferred - requires LLM):
   - Near labels: "Name:", "Account holder:", "Customer:", "Cardholder:"
   - In structured forms (first line of address block)
   - After "Pay to the order of:"
   
2. **Pattern-based** (fallback - regex):
   - `[A-Z][a-z]+ [A-Z][a-z]+( [A-Z][a-z]+)?` (capitalized words)
   - **Warning**: High false positive rate (could match company names)
   
**LLM Validation Required**: YES
- Distinguish person vs. company name
- Context: "John Smith" (person) vs. "Smith Industries" (company)

**Examples**:
```
✅ REDACT:
"Account holder: John Michael Smith"
"Customer: Jane Doe"
"Cardholder: ROBERT J. BROWN"

❌ PRESERVE:
"Chase Bank" (institution name)
"Apple Inc." (company name)
```

---

### F2 - Social Security Number (SSN)

**Detection Methods**:
1. **Pattern-based** (regex - highly reliable):
   - `\d{3}-\d{2}-\d{4}` (format: 123-45-6789)
   - `\d{9}` (format: 123456789, if near "SSN" label)
   
2. **Context validation**:
   - Near labels: "SSN:", "Social Security:", "SS#:"
   - In tax forms (Box a, b, c on W-2)

**LLM Validation Required**: NO (pattern is sufficient)

**Redaction Rules**:
- **Default**: Show last 4 digits → `***-**-1234`
- **Alternative**: Full redaction → `[REDACTED SSN]`
- **NEVER**: Show full SSN

**Special Cases**:
- Tax forms: Always redact (CRITICAL for IRS compliance)
- Loan applications: Always redact (CRITICAL for GLBA)
- Employee records: Always redact (CRITICAL for employment privacy)

**Examples**:
```
Input: "SSN: 123-45-6789"
Output: "SSN: ***-**-6789"

Input: "Social Security Number: 987654321"
Output: "Social Security Number: ***-**-4321"
```

---

### F3 - Date of Birth (DOB)

**Detection Methods**:
1. **Pattern-based** (regex):
   - `\d{1,2}/\d{1,2}/\d{4}` (format: 12/15/1985)
   - `\d{4}-\d{2}-\d{2}` (format: 1985-12-15)
   - `\d{2}/\d{2}/\d{2}` (format: 12/15/85)
   
2. **Context validation**:
   - Near labels: "DOB:", "Date of Birth:", "Born:", "Birthdate:"
   - Age calculation context: "age 38 (DOB: 12/15/1985)"

**LLM Validation Required**: YES
- Distinguish DOB vs. account opening date vs. transaction date
- Context matters: "DOB: 12/15/1985" (redact) vs. "Statement date: 12/15/2025" (preserve)

**Redaction Rules**:
- **Default**: `[REDACTED DOB]`
- **Alternative**: Partial → `**/**/19**` (show century)
- **Year only**: Sometimes acceptable → `Born: 1985`

**Examples**:
```
✅ REDACT:
"DOB: 12/15/1985" → "DOB: [REDACTED DOB]"
"Date of Birth: 1985-12-15" → "Date of Birth: [REDACTED DOB]"
"Born: 12/15/85" → "Born: [REDACTED DOB]"

❌ PRESERVE (not DOB):
"Account opened: 03/15/2020"
"Transaction date: 12/15/2025"
"Statement period: 01/01/2025 - 01/31/2025"
```

---

### F4 - Home Address

**Detection Methods**:
1. **Pattern-based** (regex):
   - Street: `\d+\s+[A-Z][a-z]+\s+(Street|St|Avenue|Ave|Road|Rd|Drive|Dr|Lane|Ln|Boulevard|Blvd|Court|Ct)`
   - Apartment: `(Apt|Unit|Suite|#)\s*[A-Z0-9]+`
   - City, State, ZIP: `[A-Z][a-z]+,\s*[A-Z]{2}\s+\d{5}(-\d{4})?`
   
2. **Context validation**:
   - Near labels: "Address:", "Home:", "Residence:", "Property address:"
   - Multi-line address blocks

**LLM Validation Required**: YES
- Distinguish home vs. business address
- Context: "Home: 123 Main St" (redact) vs. "Bank branch: 456 Oak Ave" (preserve)

**Redaction Rules**:
- **Full address**: `[REDACTED ADDRESS]`
- **Partial** (if needed): Keep city/state → "City, ST"
- **Never show**: Street number + street name

**Examples**:
```
✅ REDACT:
"123 Main Street, Apt 4B, San Francisco, CA 94102"
→ "[REDACTED ADDRESS]"

"Home address:
456 Oak Avenue
Los Angeles, CA 90001"
→ "Home address: [REDACTED ADDRESS]"

⚠️ PARTIAL (if context requires):
"123 Main St, San Francisco, CA 94102"
→ "San Francisco, CA" (keep city/state for regional context)
```

---

### F5 - Mailing Address

**Detection Methods**:
Same as F4 (Home Address), plus:
- PO Box patterns: `P\.?O\.?\s*Box\s+\d+`
- Near labels: "Mailing address:", "Correspondence:", "Send to:"

**Redaction Rules**:
Same as F4

---

### F6 - Phone Number

**Detection Methods**:
1. **Pattern-based** (regex):
   - `\(\d{3}\)\s*\d{3}-\d{4}` (format: (408) 555-0199)
   - `\d{3}-\d{3}-\d{4}` (format: 555-123-4567)
   - `\d{3}\.\d{3}\.\d{4}` (format: 555.123.4567)
   - `\+1-\d{3}-\d{3}-\d{4}` (format: +1-555-123-4567)
   
2. **Context validation**:
   - Near labels: "Phone:", "Tel:", "Contact:", "Mobile:", "Cell:"

**LLM Validation Required**: YES
- Distinguish personal vs. customer service numbers
- Context: "Phone: 555-0199" (redact) vs. "Customer service: 1-800-123-4567" (preserve)

**Redaction Rules**:
- **Default**: Show last 4 → `***-***-0199`
- **Full**: `[REDACTED PHONE]`
- **Exception**: Preserve toll-free numbers (1-800, 1-888, 1-877, etc.)

**Examples**:
```
✅ REDACT:
"Phone: (408) 555-0199" → "Phone: ***-***-0199"
"Contact: 555-123-4567" → "Contact: ***-***-4567"

❌ PRESERVE (customer service):
"Call us: 1-800-123-4567"
"Support: 1-888-555-1234"
```

---

### F7 - Email Address

**Detection Methods**:
1. **Pattern-based** (regex - highly reliable):
   - `[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}`
   
2. **Context validation**:
   - Near labels: "Email:", "E-mail:", "Contact:"

**LLM Validation Required**: YES
- Distinguish personal vs. corporate support emails
- Context: "john.smith@gmail.com" (redact) vs. "support@company.com" (preserve)

**Redaction Rules**:
- **Default**: Show domain → `***@gmail.com`
- **Full**: `[REDACTED EMAIL]`
- **Exception**: Preserve generic company emails (support@, info@, sales@)

**Examples**:
```
✅ REDACT:
"Email: john.smith@gmail.com" → "Email: ***@gmail.com"
"Contact: jane.doe@yahoo.com" → "Contact: ***@yahoo.com"

❌ PRESERVE (company generic):
"support@chase.com"
"customerservice@bank.com"
```

---

### F8 - Driver's License Number

**Detection Methods**:
1. **Pattern-based** (state-specific):
   - California: `[A-Z]\d{7}` (format: A1234567)
   - New York: `\d{9}` or `\d{3}\s\d{3}\s\d{3}`
   - Texas: `\d{8}` (format: 12345678)
   - **Note**: Patterns vary by state
   
2. **Context validation**:
   - Near labels: "DL:", "Driver's License:", "License #:", "ID #:"
   - On loan applications, insurance forms

**LLM Validation Required**: YES
- Confirm it's a DL number (not other ID type)

**Redaction Rules**:
- **Always**: `[REDACTED DL]`
- **Never**: Show any part of DL number

**Examples**:
```
✅ REDACT:
"DL: A1234567" → "DL: [REDACTED DL]"
"Driver's License: NY-123-456-789" → "Driver's License: [REDACTED DL]"
```

---

### F9 - Passport Number

**Detection Methods**:
1. **Pattern-based** (country-specific):
   - US: `\d{9}` (format: 123456789)
   - Near "Passport" label to distinguish from other 9-digit numbers
   
2. **Context validation**:
   - Near labels: "Passport:", "PP:", "Passport Number:"
   - On international banking forms, visa applications

**LLM Validation Required**: YES
- Distinguish passport vs. other ID numbers

**Redaction Rules**:
- **Always**: `[REDACTED PASSPORT]`
- **Never**: Show any part of passport number

**Examples**:
```
✅ REDACT:
"Passport: 123456789" → "Passport: [REDACTED PASSPORT]"
"PP: X12345678" → "PP: [REDACTED PASSPORT]"
```

---

## Context-Dependent Rules

### When to Preserve (Don't Redact)

**F1 (Name)**: Preserve if:
- Company/institution name (not person)
- Product name
- Public figure in non-identifying context

**F6 (Phone)**: Preserve if:
- Toll-free customer service (1-800, 1-888, 1-877, 1-866)
- Company main line

**F7 (Email)**: Preserve if:
- Generic company email (support@, info@, sales@, help@)
- Not a personal email account

---

## Used By Regulations

### GLBA (Gramm-Leach-Bliley Act)
**Coverage**: F1-F9 (all)  
**Priority**: CRITICAL for F2 (SSN), HIGH for others  
**Context**: When linked to financial accounts

### CCPA (California Consumer Privacy Act)
**Coverage**: F1-F9 (all)  
**Priority**: HIGH (all personal identifiers)  
**Context**: California residents' data

### IRS (Tax Regulations)
**Coverage**: F1-F3, F4-F5 (name, SSN, DOB, addresses on tax forms)  
**Priority**: CRITICAL for F2 (SSN), HIGH for others  
**Context**: Tax returns, W-2, 1099 forms

### FCRA (Fair Credit Reporting Act)
**Coverage**: F1-F9 (all)  
**Priority**: HIGH (credit report data)  
**Context**: Credit reports, background checks

### SOX (Sarbanes-Oxley Act)
**Coverage**: F1-F3 (executive personal data)  
**Priority**: MEDIUM (for executive compensation disclosure)  
**Context**: Corporate financial documents with employee data

---

## Used By Document Types

### Personal Documents
- ✅ Bank statements (F1, F4, F6)
- ✅ Tax returns (F1-F5)
- ✅ Credit card statements (F1, F4-F5, F6-F7)
- ✅ Credit reports (F1-F9 all)
- ✅ Loan applications (F1-F9 all)
- ✅ Investment statements (F1, F4, F6-F7)

### Corporate Documents
- ✅ Employee compensation analysis (F1-F3, F2 for SSN)
- ✅ Board minutes (F1, F6-F7 for director contact info)
- ✅ Audit reports (F1 for employee names in findings)

---

## Testing

### Test Case 1: Bank Statement Header

**Input**:
```
Account holder: John Michael Smith
Address: 123 Main Street, Apt 4B, San Francisco, CA 94102
Phone: (415) 555-0199
SSN: ***-**-6789 (last 4 shown on original)
```

**Expected Output**:
```
Account holder: [REDACTED NAME]
Address: [REDACTED ADDRESS]
Phone: ***-***-0199
SSN: ***-**-6789
```

---

### Test Case 2: Tax Form (W-2)

**Input**:
```
Box a: 123-45-6789 (Employee SSN)
Box e: Jane Elizabeth Doe
Box f: 456 Oak Avenue, Los Angeles, CA 90001
```

**Expected Output**:
```
Box a: ***-**-6789
Box e: [REDACTED NAME]
Box f: [REDACTED ADDRESS]
```

---

### Test Case 3: Context Awareness (Don't Over-Redact)

**Input**:
```
Account holder: John Smith
Bank name: Wells Fargo Bank
Customer service: 1-800-869-3557
Email: customerservice@wellsfargo.com
```

**Expected Output**:
```
Account holder: [REDACTED NAME]
Bank name: Wells Fargo Bank ← PRESERVED (institution)
Customer service: 1-800-869-3557 ← PRESERVED (toll-free)
Email: customerservice@wellsfargo.com ← PRESERVED (generic company)
```

---

## Summary

**Total Identifiers**: 9  
**CRITICAL**: 1 (F2 - SSN)  
**HIGH**: 7 (F1, F3-F5, F8-F9)  
**MEDIUM**: 1 (F6-F7 context-dependent)

**Key Principle**: Always redact when in doubt. Better to over-redact than under-redact.

**Next Steps**:
1. Implement detection patterns (regex + LLM prompts)
2. Test with sample documents
3. Validate compliance against GLBA, CCPA, IRS requirements
