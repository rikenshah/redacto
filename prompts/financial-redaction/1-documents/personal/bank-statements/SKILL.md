---
name: bank-statements
description: Redacts personal bank statements for safe sharing with accountants, financial advisors, or lenders ensuring GLBA and CCPA compliance. Removes account numbers, routing numbers, SSN, debit card numbers, transaction details, balances, and personal identifiers while preserving institution name and statement structure. Use when processing bank statements, checking account statements, savings statements, online banking downloads, or when the user mentions bank statements, account statements, financial statements, or GLBA compliance.
---

# Bank Statement Redaction

## Purpose

Redact personal bank statements to safely share with accountants, financial advisors, lenders, or other third parties while maintaining GLBA and CCPA compliance.

## Document Type

**Personal Bank Statement**

Common formats:
- PDF statements (download from online banking)
- Paper statements (photographed/scanned)
- Mobile banking screenshots
- CSV/Excel exports

Typical institutions:
- Traditional banks (Chase, Bank of America, Wells Fargo, etc.)
- Credit unions
- Online banks (Ally, Marcus, etc.)
- NeoBanks (Chime, Current, etc.)

---

## What Gets Redacted

### Critical Information (Always Redact)
- **Account Number**: Last 4 digits shown only
- **Routing Number**: Last 4 digits shown only (or fully redacted)
- **Social Security Number**: If present (rare, but check)

### High Priority (Redact in Most Cases)
- **Account Holder Name**: Full name redacted
- **Address**: Full address redacted (may keep city/state if needed)
- **Phone Number**: Last 4 digits shown only
- **Email Address**: Domain shown only (e.g., ***@gmail.com)
- **Account Balance**: Current/available balance
- **Previous Balance**: Opening balance
- **Total Deposits/Withdrawals**: Summary amounts

### Medium Priority (Context-Dependent)
- **Transaction Details**: Individual transaction amounts, dates, descriptions
- **Merchant Names**: If they reveal behavioral patterns
- **Check Numbers**: If personal checks
- **Wire Transfer Details**: Reference numbers, beneficiary info

### What to Preserve
- **Bank Name/Logo**: Identifies the institution
- **Statement Period**: Date range (e.g., "Statement for March 2025")
- **Account Type**: Checking, Savings, Money Market
- **Interest Rate**: General APY (if not personally identifying)
- **Fee Structure**: General fee information
- **Customer Service Contact**: Bank's phone number

---

## Required Identifier Categories

This document type needs these identifier categories:

```yaml
required_identifiers:
  critical:
    - 2-identifiers/personal/F1-F9-identity/
      reason: "Account holder name, address, contact info"
      identifiers: [F1, F4, F6, F7]
    
    - 2-identifiers/personal/F10-F24-accounts/
      reason: "Account number, routing number, balance"
      identifiers: [F10, F11, F14]
  
  high_priority:
    - 2-identifiers/personal/F91-F105-transactions/
      reason: "Transaction details, amounts, merchants"
      identifiers: [F91, F92, F97, F98, F99]
```

---

## Processing Workflow

```
1. DOCUMENT IDENTIFICATION
   └─> Detected: Bank statement (logo, "Account Number:", etc.)

2. LOAD IDENTIFIER CATEGORIES
   ├─> F1-F9-identity/ (name, address, phone)
   ├─> F10-F24-accounts/ (account #, routing #, balance)
   └─> F91-F105-transactions/ (transaction details)

3. SCAN & DETECT
   ├─> Header section: Account holder info
   ├─> Summary section: Balances
   └─> Transaction section: Individual transactions

4. APPLY REDACTION RULES
   ├─> Critical: Full redaction or last 4 digits
   ├─> High: Full redaction
   └─> Medium: Context-dependent

5. VALIDATE COMPLIANCE
   ├─> Check: 3-compliance/GLBA/VALIDATION.md
   └─> Check: 3-compliance/CCPA/VALIDATION.md

6. GENERATE OUTPUT
   ├─> Redacted statement (visual + text)
   └─> Compliance report
```

---

## Document Structure

### Typical Bank Statement Layout

```
┌─────────────────────────────────────────────────┐
│ BANK LOGO                    Customer Service   │
│                              1-800-XXX-XXXX     │
├─────────────────────────────────────────────────┤
│ Account Holder Info (REDACT THIS SECTION)       │
│ John Michael Smith           ← F1 (Name)        │
│ 123 Main Street, Apt 4B      ← F4 (Address)     │
│ San Francisco, CA 94102      ← F4 (Address)     │
│ Phone: (415) 555-0199        ← F6 (Phone)       │
│ Email: john@email.com        ← F7 (Email)       │
├─────────────────────────────────────────────────┤
│ Account Summary                                  │
│ Account Number: 1234567890   ← F10 (CRITICAL)   │
│ Routing Number: 021000021    ← F11 (CRITICAL)   │
│ Account Type: Checking                           │
│ Statement Period: 03/01-03/31/2025              │
├─────────────────────────────────────────────────┤
│ Balance Summary (REDACT THIS SECTION)           │
│ Beginning Balance:  $5,432.12  ← F14 (Balance)  │
│ Total Deposits:     $3,200.00                    │
│ Total Withdrawals:  $2,150.50                    │
│ Ending Balance:     $6,481.62  ← F14 (Balance)  │
├─────────────────────────────────────────────────┤
│ Transaction History (REDACT DETAILS)            │
│ Date    Description          Amount  Balance    │
│ 03/01   Deposit ATM         +500.00  5,932.12   │
│ 03/03   Amazon.com          -45.99   5,886.13   │
│ 03/05   Starbucks           -5.50    5,880.63   │
│ 03/10   Direct Deposit      +2,500   8,380.63   │
│ ...                                              │
└─────────────────────────────────────────────────┘
```

### Redacted Output

```
┌─────────────────────────────────────────────────┐
│ [BANK LOGO PRESERVED]        Customer Service   │
│                              1-800-XXX-XXXX     │
├─────────────────────────────────────────────────┤
│ Account Holder Info                             │
│ [REDACTED NAME]                                 │
│ [REDACTED ADDRESS]                              │
│ [REDACTED ADDRESS]                              │
│ Phone: ***-***-0199                             │
│ Email: ***@email.com                            │
├─────────────────────────────────────────────────┤
│ Account Summary                                  │
│ Account Number: ********7890                     │
│ Routing Number: ****0021                         │
│ Account Type: Checking                           │
│ Statement Period: 03/01-03/31/2025              │
├─────────────────────────────────────────────────┤
│ Balance Summary                                  │
│ Beginning Balance:  [REDACTED BALANCE]          │
│ Total Deposits:     [REDACTED AMOUNT]           │
│ Total Withdrawals:  [REDACTED AMOUNT]           │
│ Ending Balance:     [REDACTED BALANCE]          │
├─────────────────────────────────────────────────┤
│ Transaction History                             │
│ Date    Description          Amount  Balance    │
│ 03/01   [REDACTED]          [RED.]  [RED.]      │
│ 03/03   [REDACTED]          [RED.]  [RED.]      │
│ 03/05   [REDACTED]          [RED.]  [RED.]      │
│ 03/10   [REDACTED]          [RED.]  [RED.]      │
│ ...                                              │
└─────────────────────────────────────────────────┘
```

---

## Use Cases

### Use Case 1: Sending to Accountant

**Scenario**: Small business owner needs to share bank statements with tax preparer.

**What to Redact**:
- ✅ Name (unless accountant needs it for tax filing)
- ✅ Address (unless accountant needs it)
- ✅ Account number (show last 4 for reference)
- ✅ Transaction details (unless needed for tax deductions)

**What to Preserve**:
- Bank name
- Statement period
- Account type
- General transaction categories (if needed)

### Use Case 2: Mortgage Application

**Scenario**: Home buyer needs to prove income/assets to lender.

**What to Redact**:
- ✅ Routing number (lender doesn't need it)
- ✅ Individual transaction details (lender only needs balances)
- ✅ Personal phone/email (lender has it separately)

**What to Preserve**:
- Name (lender needs to verify identity)
- Account number (last 4 for verification)
- Balances (required for asset verification)
- Large deposit sources (may need to explain)

### Use Case 3: Sending to Financial Advisor

**Scenario**: Client shares statements with advisor to review spending patterns.

**What to Redact**:
- ✅ Account number (advisor doesn't need it)
- ✅ Routing number
- ✅ Exact transaction amounts (can show percentages)
- ✅ Merchant names (if privacy concern)

**What to Preserve**:
- Transaction categories (for budget analysis)
- Date ranges
- Summary totals

### Use Case 4: Dispute Resolution

**Scenario**: Contesting unauthorized charges with merchant or bank.

**What to Redact**:
- ✅ Other transactions (not related to dispute)
- ✅ Account balance (not relevant)
- ✅ Personal contact info (if submitting to third party)

**What to Preserve**:
- Account number (last 4 for reference)
- Disputed transaction details
- Transaction dates
- Merchant name (for disputed charge)

---

## Compliance Requirements

### GLBA (Gramm-Leach-Bliley Act)

**Applies**: ✅ YES - Bank statements contain NPI (Nonpublic Personal Information)

**Critical Requirements**:
- F10 (Account Number): 100% redaction (show last 4 only)
- F11 (Routing Number): 100% redaction
- F2 (SSN): 100% redaction (if present)

**High Priority**:
- F1 (Name): 95%+ redaction
- F4 (Address): 95%+ redaction
- F6-F7 (Phone/Email): 95%+ redaction
- F14 (Balance): 95%+ redaction

**Validation**: See `3-compliance/GLBA/VALIDATION.md`

**Penalty if Non-Compliant**: Up to $500,000 per violation

---

### CCPA (California Consumer Privacy Act)

**Applies**: ✅ If account holder is California resident

**Requirements**:
- Personal identifiers (F1, F4, F6-F7): Must be redacted
- Financial account info (F10-F11): Must be redacted
- Commercial information (F91-F99): Must be redacted if identifiable

**Note**: GLBA exempts some data from CCPA, but ShieldText applies both standards

---

## Expected Output

### User-Facing Report

```
✅ Bank Statement Redacted Successfully

Document: Chase Bank Statement - March 2025
Processing Time: 0.84 seconds
Identifiers Redacted: 23 items

Breakdown:
  • Personal Info: 4 items (name, address, phone, email)
  • Account Data: 3 items (account #, routing #, balance)
  • Transactions: 16 items (amounts, merchants, dates)

Compliance Status:
  ✅ GLBA Compliant - All NPI properly redacted
  ✅ CCPA Compliant - Personal information secured

Safe to share with: Accountant, financial advisor, lender
```

### Technical Validation (for audit)

```yaml
document_type: bank_statement
institution: chase_bank
statement_period: "2025-03-01 to 2025-03-31"

identifiers_found:
  critical:
    - F10: 1 instance (account number)
    - F11: 1 instance (routing number)
  high:
    - F1: 1 instance (name)
    - F4: 1 instance (address)
    - F6: 1 instance (phone)
    - F7: 1 instance (email)
    - F14: 4 instances (balances)
  medium:
    - F97: 12 instances (transaction amounts)
    - F98: 12 instances (transaction dates)
    - F99: 12 instances (merchant names)

redaction_summary:
  critical_redacted: 2/2 (100%) ✅
  high_redacted: 8/8 (100%) ✅
  medium_redacted: 36/36 (100%) ✅

compliance:
  glba:
    status: COMPLIANT
    critical: 100%
    high: 100%
    medium: 100%
  ccpa:
    status: COMPLIANT
    all_personal_data: redacted

approved_for_sharing: true
```

---

## Testing

### Test Case 1: Standard Checking Account

**Input**: Chase Bank checking statement (PDF)
**Expected Redactions**: Name, address, phone, email, account #, routing #, balances, transactions
**Expected Compliance**: ✅ GLBA, ✅ CCPA
**Processing Time**: <1 second

### Test Case 2: Business Account

**Input**: Wells Fargo business checking statement
**Expected Redactions**: Business name (if sole proprietor), account info, transactions
**Expected Compliance**: ✅ GLBA (if sole proprietor), ⚠️ Limited (if corporation)
**Processing Time**: <1 second

### Test Case 3: Savings Account

**Input**: Ally Bank savings statement (online PDF)
**Expected Redactions**: Similar to checking, plus interest earned
**Expected Compliance**: ✅ GLBA, ✅ CCPA
**Processing Time**: <1 second

---

## Common Pitfalls to Avoid

### ❌ Over-Redaction
**Don't redact**: Bank name, statement period, account type, customer service numbers
**Why**: Recipient needs context about which bank/account

### ❌ Under-Redaction
**Don't miss**: Routing number, full account number, full SSN, transaction details
**Why**: GLBA violation, potential fraud

### ❌ Inconsistent Redaction
**Don't**: Redact name in header but leave it in footer
**Why**: Defeats purpose of redaction

### ❌ Format Breaking
**Don't**: Make redacted statement unreadable (e.g., replacing entire lines)
**Why**: Recipient can't use the document

---

## Edge Cases

### Case 1: Joint Account
**Issue**: Two names on account
**Solution**: Redact both names consistently

### Case 2: Multiple Pages
**Issue**: Name appears on every page header
**Solution**: Redact on all pages (may use "[REDACTED - SEE PAGE 1]" on subsequent pages)

### Case 3: Foreign Transactions
**Issue**: International merchants, foreign currency
**Solution**: Redact merchant names, convert currency indicators

### Case 4: Cashier's Check Purchase
**Issue**: Check number + recipient name appear
**Solution**: Redact both (recipient name is sensitive)

### Case 5: Mobile Check Deposit
**Issue**: Image of check included in statement
**Solution**: Redact entire check image or use strong blur

---

## Performance Targets

- **Processing Time**: <1 second for typical 1-3 page statement
- **Accuracy**: 98%+ identifier detection rate
- **False Positives**: <2% (don't redact bank name, etc.)
- **False Negatives**: <1% (missing sensitive data)

---

## Next Steps After Redaction

1. **Review**: Quickly scan redacted statement for missed items
2. **Save**: Store redacted version securely
3. **Share**: Send via email, upload to portal, etc.
4. **Dispose**: Shred or securely delete original (if no longer needed)
5. **Audit**: Keep compliance report for records (optional)

---

## Related Document Types

Similar redaction needs:
- Credit card statements → `1-documents/personal/credit-cards/`
- Investment statements → `1-documents/personal/investment-statements/`
- Loan statements → `1-documents/personal/loan-applications/`

---

## Status

**Phase**: 1 - Foundation ✅
**Tested**: Pending
**Production Ready**: After testing

**Last Updated**: April 30, 2026
