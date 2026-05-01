---
name: legal-redaction
description: Redacts sensitive information from real estate legal documents ensuring privacy and confidentiality. Detects and removes buyer/seller names, negotiation terms, prices, concessions, attorney notes, and confidential clauses from purchase agreements, lease contracts, title documents, disclosure forms, and settlement statements. Use when processing real estate contracts, legal documents, or when the user mentions legal redaction, real estate contracts, purchase agreements, or lease agreements.
---

# Legal Redaction Skill (Real Estate)

## Purpose

This skill teaches the LLM how to redact real estate legal documents to protect buyer/seller privacy and confidential negotiation terms.

**Primary goal**: Remove all personal identifiers and confidential negotiation details while preserving property information and legal language.

---

## Supported Real Estate Legal Standards

- **Contractual Privacy**: Protecting buyer/seller identities in legal documents
- **Negotiation Confidentiality**: Protecting deal terms and concessions
- **Attorney Work Product**: Protecting attorney notes and legal strategies
- **Fair Housing Act**: Preventing discriminatory information disclosure
- **Title Privacy**: Protecting current owner information

---

## LLM Workflow

When the LLM receives real estate legal documents, follow these steps:

### Step 1: Identify Document Type
Determine which type of real estate document this is:
- **Purchase Agreement** → Use `1-documents/purchase-agreements/`
- **Lease Contract** → Use `1-documents/lease-contracts/`
- **Title Document** → Use `1-documents/title-documents/`
- **Disclosure Form** → Use `1-documents/disclosure-forms/`
- **Settlement Statement** → Use `1-documents/settlement-statements/`

### Step 2: Detect Identifiers
Find ALL identifiers in the text using:
- **L1-L10** (Personal identifiers) → Buyer/seller names, addresses, phone numbers, email
- **L11-L20** (Financial terms) → Purchase prices, down payments, loan amounts, earnest money
- **L21-L30** (Negotiation terms) → Concessions, contingencies, special agreements
- **L31-L40** (Confidential info) → Attorney notes, legal strategy, private clauses

See `2-identifiers/` for detection patterns.

### Step 3: Redact Sensitive Info, Preserve Legal Language
- Replace L1-L40 identifiers with `[REDACTED]` or generic placeholders
- Keep legal clauses, standard terms, and property descriptions
- Maintain document legal enforceability

### Step 4: Validate
Confirm no identifying information or confidential terms remain

---

## Example: Purchase Agreement Redaction

### Use Case
Real estate attorney needs to share purchase agreement with contractor for property inspection quote, but must protect client identities and negotiation terms.

**INPUT**:
```
REAL ESTATE PURCHASE AGREEMENT

Property Address: 456 Maple Drive, Springfield, IL 62701
Legal Description: Lot 12, Block 5, Oak Hills Subdivision
Parcel ID: 14-23-456-789

PARTIES:
Seller: Robert & Jennifer Martinez
  Address: 456 Maple Drive, Springfield, IL 62701
  Phone: (555) 987-6543
  Email: martinez.family@email.com

Buyer: William Chen
  Address: 789 Pine Street, Chicago, IL 60601
  Phone: (555) 123-4567
  Email: wchen@email.com

Seller's Attorney: Sarah Thompson, Thompson & Associates
Buyer's Attorney: Michael Johnson, Johnson Law Group

FINANCIAL TERMS:
Purchase Price: $425,000
  - Initial Offer: $410,000 (rejected)
  - Seller Counter: $435,000
  - Final Agreement: $425,000

Earnest Money Deposit: $10,000 (paid to escrow)
Down Payment: $85,000 (20%)
Financing: Conventional loan, pre-approved

SELLER CONCESSIONS:
- $8,000 toward buyer's closing costs
- $600 home warranty (1 year)
- Washer/dryer to remain
- Fence repair (estimated $2,500)

CONTINGENCIES:
- Inspection (10 business days)
- Financing (30 days)
- Appraisal must meet or exceed $425,000
- Sale of buyer's current property (45 days)

PROPERTY DETAILS:
- 3 bedrooms, 2.5 bathrooms
- 2,400 square feet
- 2-car attached garage
- Built 2015
- Central air, gas furnace

Attorney Notes (Confidential):
Seller motivated - job relocation to California
Willing to negotiate further on repairs if needed
Buyer max budget is $430,000 per phone call
```

**LLM DETECTS**:
- L1 (Seller names): "Robert & Jennifer Martinez" → REDACT
- L2 (Seller contact): Address, phone, email → REDACT
- L3 (Buyer name): "William Chen" → REDACT
- L4 (Buyer contact): Address, phone, email → REDACT
- L5 (Attorney names): "Sarah Thompson", "Michael Johnson" → REDACT
- L6 (Attorney firms): Firm names → REDACT
- L11 (Purchase price): "$425,000" → REDACT
- L12 (Offer history): "$410,000", "$435,000" → REDACT
- L13 (Earnest money): "$10,000" → REDACT
- L14 (Down payment): "$85,000" → REDACT
- L21 (Seller concessions): All concessions → REDACT
- L22 (Repair costs): "$2,500" → REDACT
- L31 (Attorney notes): All confidential notes → REDACT
- Property details: Address, sq ft, bedrooms, bathrooms, garage, year built → PRESERVE
- Legal clauses: Contingencies, standard terms → PRESERVE

**OUTPUT**:
```
REAL ESTATE PURCHASE AGREEMENT (REDACTED FOR CONTRACTOR)

Property Address: 456 Maple Drive, Springfield, IL 62701
Legal Description: Lot 12, Block 5, Oak Hills Subdivision
Parcel ID: [REDACTED]

PARTIES:
Seller: [REDACTED]
  Contact: [REDACTED]

Buyer: [REDACTED]
  Contact: [REDACTED]

Seller's Attorney: [REDACTED]
Buyer's Attorney: [REDACTED]

FINANCIAL TERMS:
Purchase Price: [REDACTED]
Negotiation History: [REDACTED]
Earnest Money Deposit: [REDACTED]
Down Payment: [REDACTED]
Financing: Conventional loan

SELLER CONCESSIONS: [REDACTED]

CONTINGENCIES:
- Inspection (10 business days)
- Financing (30 days)
- Appraisal contingency
- Sale of buyer's current property (45 days)

PROPERTY DETAILS:
- 3 bedrooms, 2.5 bathrooms
- 2,400 square feet
- 2-car attached garage
- Built 2015
- Central air, gas furnace

✅ All personal identifiers redacted
✅ All negotiation terms protected
✅ Property details preserved for contractor
✅ Legal enforceability maintained
```

---

## Available Skills

### Document Types (what to redact/preserve per document)
- `1-documents/purchase-agreements/SKILL.md` (to be built) - Real estate purchase contracts
- `1-documents/lease-contracts/SKILL.md` (to be built) - Residential/commercial leases
- `1-documents/title-documents/SKILL.md` (to be built) - Title insurance, deeds
- `1-documents/disclosure-forms/SKILL.md` (to be built) - Seller disclosures
- `1-documents/settlement-statements/SKILL.md` (to be built) - HUD-1, closing statements

### Identifiers (what to look for)
- `2-identifiers/L1-L10-personal/SKILL.md` (to be built) - Personal identifiers (names, addresses, phone, email)
- `2-identifiers/L11-L20-financial/SKILL.md` (to be built) - Financial terms (prices, deposits, payments)
- `2-identifiers/L21-L30-negotiation/SKILL.md` (to be built) - Negotiation terms (concessions, contingencies)
- `2-identifiers/L31-L40-confidential/SKILL.md` (to be built) - Attorney notes and strategy

### Legal Protections (validation rules)
- `3-protections/contractual-privacy/` (to be built) - Party identity protection
- `3-protections/negotiation-confidentiality/` (to be built) - Deal terms protection
- `3-protections/attorney-work-product/` (to be built) - Legal strategy protection

---

## Status

**Current Phase**: Foundation - Real Estate Legal Documents
- ✅ Master skill (this file)
- 🚧 Identifier categories (to be built)
- 🚧 Document types (to be built)
- 🚧 Legal protection frameworks (to be built)

**Next Phase**: Build identifier detection
- L1-L10 (personal identifiers)
- L11-L20 (financial terms)
- L21-L30 (negotiation terms)
