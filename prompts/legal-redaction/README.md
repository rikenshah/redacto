# Legal Redaction Skills for LLM (Real Estate)

## Purpose

These skills teach your LLM how to redact real estate legal documents to protect buyer/seller identities and confidential negotiation terms.

---

## Use Cases

From your secondary-industries.md (Real Estate):

### 1. Contract Negotiation
**Problem**: Need to share draft purchase agreement with contractor for quote, but must hide "final price" and "private concessions".
**Solution**: Redact buyer/seller names, negotiation terms, and financial concessions while preserving property details.

### 2. Multi-Party Document Sharing
**Problem**: Attorney needs to share contract with inspector, appraiser, or contractor - each needs different information.
**Solution**: Redact personal identifiers and confidential terms based on recipient's role.

### 3. Private Document Sharing
**Problem**: Lease agreements or disclosure forms contain sensitive information that must be protected when sharing with third parties.
**Solution**: Redact owner/tenant names, financial terms, and personal details while preserving legal language.

---

## Structure

```
legal-redaction/
│
├── SKILL.md                            ← Master skill - LLM workflow
├── README.md                           ← This file
│
├── 1-documents/                        ← Document-specific redaction rules
│   ├── purchase-agreements/            (to be built)
│   ├── lease-contracts/                (to be built)
│   ├── title-documents/                (to be built)
│   ├── disclosure-forms/               (to be built)
│   └── settlement-statements/          (to be built)
│
├── 2-identifiers/                      ← What to detect
│   ├── L1-L10-personal/                (to be built)
│   ├── L11-L20-financial/              (to be built)
│   ├── L21-L30-negotiation/            (to be built)
│   └── L31-L40-confidential/           (to be built)
│
└── 3-protections/                      ← Validation rules
    ├── contractual-privacy/            (to be built)
    ├── negotiation-confidentiality/    (to be built)
    └── attorney-work-product/          (to be built)
```

---

## How It Works

### 4-Step LLM Process

**1. CLASSIFY** → Determine document type
- Purchase agreement? Lease? Title document? Disclosure form?
- Load appropriate document skill

**2. DETECT** → Find identifiers
- L1-L10 (Personal) → Buyer/seller names, addresses, phone numbers, email
- L11-L20 (Financial) → Purchase prices, deposits, loan amounts
- L21-L30 (Negotiation) → Concessions, contingencies, special agreements
- L31-L40 (Confidential) → Attorney notes, legal strategy, private clauses

**3. REDACT** → Transform text
- Replace L1-L40 with `[REDACTED]` placeholders
- Keep legal language, property details, standard terms

**4. VALIDATE** → Confirm protection
- Check: No personal identifiers or confidential terms remain
- Result: ✅ PROTECTED or list violations

---

## Example: Purchase Agreement

**BEFORE (CONFIDENTIAL TERMS EXPOSED)**:
```
REAL ESTATE PURCHASE AGREEMENT

Property: 456 Maple Drive, Springfield, IL 62701
Seller: Robert & Jennifer Martinez
  Phone: (555) 987-6543
  Email: martinez.family@email.com
Buyer: William Chen
  Phone: (555) 123-4567
  Email: wchen@email.com

Seller's Attorney: Sarah Thompson, Thompson & Associates
Buyer's Attorney: Michael Johnson, Johnson Law Group

FINANCIAL TERMS:
Purchase Price: $425,000
  - Initial Offer: $410,000 (rejected)
  - Seller Counter: $435,000
  - Final Agreement: $425,000

Earnest Money: $10,000
Down Payment: $85,000 (20%)

SELLER CONCESSIONS:
- $8,000 toward closing costs
- $600 home warranty
- Washer/dryer to remain
- Fence repair ($2,500)

PROPERTY: 3 bed, 2.5 bath, 2,400 sq ft, 2-car garage, Built 2015

Attorney Notes (Confidential):
Seller motivated - job relocation
Buyer max budget is $430,000

❌ All parties identified, negotiation history visible, confidential notes exposed
```

**LLM ANALYSIS**:
```
REDACT:
- "Robert & Jennifer Martinez" (L1: Seller names)
- "(555) 987-6543", "martinez.family@email.com" (L2: Seller contact)
- "William Chen" (L3: Buyer name)
- "(555) 123-4567", "wchen@email.com" (L4: Buyer contact)
- "Sarah Thompson, Thompson & Associates" (L5: Seller attorney)
- "Michael Johnson, Johnson Law Group" (L6: Buyer attorney)
- "$425,000", "$410,000", "$435,000" (L11-L12: Prices and negotiation history)
- "$10,000" (L13: Earnest money)
- "$85,000" (L14: Down payment)
- "$8,000", "$600", "$2,500", "washer/dryer" (L21: All concessions)
- "Seller motivated - job relocation", "Buyer max budget $430,000" (L31: Attorney notes)

PRESERVE:
- Property address (needed for contractor)
- Property specifications (3 bed, 2.5 bath, 2,400 sq ft, garage, year)
- Standard contingencies (inspection, financing, appraisal)
- Legal terms and clauses
```

**AFTER (CONFIDENTIALITY PROTECTED)**:
```
REAL ESTATE PURCHASE AGREEMENT (REDACTED)

Property: 456 Maple Drive, Springfield, IL 62701
Seller: [REDACTED]
  Contact: [REDACTED]
Buyer: [REDACTED]
  Contact: [REDACTED]

Seller's Attorney: [REDACTED]
Buyer's Attorney: [REDACTED]

FINANCIAL TERMS:
Purchase Price: [REDACTED]
Negotiation History: [REDACTED]
Earnest Money: [REDACTED]
Down Payment: [REDACTED]

SELLER CONCESSIONS: [REDACTED]

PROPERTY: 3 bed, 2.5 bath, 2,400 sq ft, 2-car garage, Built 2015

CONTINGENCIES:
- Inspection (10 business days)
- Financing (30 days)
- Appraisal contingency
- Sale of buyer's current property (45 days)

✅ All parties de-identified
✅ Negotiation terms protected
✅ Property details preserved for contractor quote
✅ Legal enforceability maintained
```

---

## What Gets Redacted vs Preserved

### 🔴 REDACT (L1-L40 Identifiers)

**L1-L10 (Personal Identifiers)**
- Buyer names
- Seller names
- Attorney names (both parties)
- Real estate agent names
- Phone numbers
- Email addresses
- Personal addresses (buyer/seller residences)
- Social Security Numbers (if on documents)

**L11-L20 (Financial Terms)**
- Purchase price (final agreed amount)
- Initial offer amounts
- Counter-offer amounts
- Earnest money deposit amounts
- Down payment amounts
- Loan amounts
- Property tax amounts
- HOA fees

**L21-L30 (Negotiation Terms)**
- Seller concessions (dollar amounts)
- Closing cost credits
- Repair agreements and costs
- Items included in sale (appliances, furniture)
- Special conditions or terms
- Private agreements between parties
- Inspection repair negotiations
- Contingency waiver terms

**L31-L40 (Confidential Information)**
- Attorney work notes
- Legal strategy discussions
- Client motivations ("must sell quickly")
- Maximum/minimum budgets
- Alternative offers considered
- Private attorney-client communications

### 🟢 PRESERVE (Property & Legal Language)

**Property Information**
- Property address (for contractor/inspector use)
- Square footage
- Number of bedrooms/bathrooms
- Lot size
- Year built
- Property type (single-family, condo, etc.)
- Garage/parking specifications
- Property features (pool, deck, etc.)

**Legal Terms**
- Standard contingencies (inspection, financing, appraisal)
- Closing timeline
- Possession date
- Title requirements
- Standard contract clauses
- Legal descriptions (lot/block/subdivision)
- Zoning information
- Property condition clauses

**Professional Standards**
- Attorney firm names (can be preserved if needed for verification)
- Brokerage names (not individual agent names)
- Title company name
- Inspection company requirements

---

## Real Estate Legal Standards

### Contractual Privacy
Protects identities of buyers and sellers in real estate transactions. Names, contact information, and personal circumstances must be redacted when sharing with third parties.

### Negotiation Confidentiality
Protects the negotiation process including offer amounts, counter-offers, and concessions. This information gives unfair advantage if disclosed and can harm future negotiations.

### Attorney Work Product
Protects attorney notes, legal strategy, and client communications. These are privileged communications that must remain confidential.

### Fair Housing Act
Ensures legal documents don't contain discriminatory language or terms that violate protected characteristics (race, color, national origin, religion, sex, familial status, or disability).

---

## Current Status

✅ **Foundation** - Master skill and README created  
🚧 **Identifiers** - L1-L40 categories to be built  
🚧 **Document types** - Purchase agreements, leases, title docs, disclosures, settlement statements  
🚧 **Protections** - Contractual privacy, negotiation confidentiality, attorney work product frameworks

**Next Phase**: Build identifier detection skills
- L1-L10 (personal identifiers)
- L11-L20 (financial terms)
- L21-L30 (negotiation terms)

---

## Summary

✅ **Real estate legal document foundation** - Contracts, agreements, and private documents  
🚧 **40 identifier categories** - Personal, financial, negotiation, confidential (to be built)  
✅ **Contractual privacy** - Framework defined  
✅ **Negotiation confidentiality** - Framework defined  
✅ **Attorney work product protection** - Framework defined

**These skills teach your LLM exactly what to look for and what to redact in real estate legal documents.**
