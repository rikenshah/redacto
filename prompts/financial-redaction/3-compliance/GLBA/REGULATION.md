# GLBA (Gramm-Leach-Bliley Act)

## Table of Contents
1. [Overview](#overview)
2. [Requirements](#requirements)
3. [Who Must Comply](#who-must-comply)
4. [Penalties](#penalties)
5. [Applicability Rules](#applicability-rules)
6. [References](#references)

---

## Overview

**Full Name**: Gramm-Leach-Bliley Act (also known as Financial Services Modernization Act)  
**Enacted**: November 12, 1999  
**Jurisdiction**: United States  
**Enforced By**: 
- Federal Trade Commission (FTC)
- Federal Reserve Board
- Office of the Comptroller of the Currency (OCC)
- Federal Deposit Insurance Corporation (FDIC)
- State banking regulators

**Citation**: 15 USC § 6801-6809; 15 CFR Part 313 (Privacy Rule)

### Purpose

The GLBA requires financial institutions to:
1. Protect the privacy and security of consumers' **"Nonpublic Personal Information" (NPI)**
2. Provide clear privacy notices explaining information practices
3. Allow consumers to opt-out of certain information sharing
4. Implement appropriate safeguards to protect customer data

### Key Concept: Nonpublic Personal Information (NPI)

**NPI Definition** (15 CFR § 313.3(n)):
> Personally identifiable financial information:
> - Provided by a consumer to obtain a financial product or service
> - About a consumer resulting from any transaction with them
> - Otherwise obtained about a consumer in connection with providing a financial product or service

**Examples of NPI**:
- Name + account number
- Name + credit card number  
- Name + balance
- Name + transaction history
- Credit reports
- Account opening documents
- Loan applications

**NOT NPI**:
- Publicly available information (phone books, government records, published media)
- De-identified/aggregate data
- Information about companies (unless sole proprietor)

---

## Requirements

### 1. Privacy Rule (15 CFR Part 313)

Financial institutions must:

#### Initial Privacy Notice
- Provide clear notice of privacy practices at the time of establishing a customer relationship
- Explain what NPI is collected, how it's used, and with whom it's shared

#### Annual Privacy Notice
- Provide annual notices to customers explaining current privacy practices
- Include opt-out rights (if applicable)

#### Opt-Out Rights
- Allow customers to opt-out of information sharing with non-affiliated third parties
- Exceptions: Sharing for processing transactions, preventing fraud, or as required by law

#### Privacy Notice Contents
Must include:
- Categories of NPI collected
- Categories of NPI disclosed
- Categories of affiliates/non-affiliates receiving information
- Policies for protecting confidentiality and security
- Opt-out procedures (if applicable)

---

### 2. Safeguards Rule (16 CFR Part 314)

Financial institutions must:

#### Written Information Security Plan
Develop, implement, and maintain a comprehensive written information security program that:
- Is appropriate to the size, complexity, and nature of business
- Includes administrative, technical, and physical safeguards

#### Required Elements
1. **Designate Coordinator**: Assign employee(s) to coordinate information security program
2. **Risk Assessment**: Identify and assess risks to customer information
3. **Design Safeguards**: Implement controls to minimize identified risks
4. **Vendor Management**: Require service providers to maintain appropriate safeguards
5. **Evaluate & Adjust**: Regularly test and monitor safeguards, adjusting as needed

#### Specific Security Measures (2021 Amendment)
- Access controls and authentication
- Data inventory and classification
- Encryption of data in transit and at rest (where reasonable)
- Incident response plan
- Multi-factor authentication for any individual accessing customer information
- Regular monitoring and logging
- Annual penetration testing and vulnerability assessments

---

### 3. Pretexting Protection (15 USC § 6821)

Prohibits:
- **Pretexting**: Obtaining customer financial information under false pretenses
- Using false, fictitious, or fraudulent statements to obtain NPI
- Using forged, counterfeit, lost, or stolen documents to obtain NPI
- Asking another person to obtain NPI through pretexting

**Criminal Offense**: Violations can result in fines and imprisonment

---

## Who Must Comply

### Covered Entities ("Financial Institutions")

Per 15 USC § 6809(3), a financial institution is any entity that engages in "financial activities" including:

#### Traditional Financial Institutions
- Banks and credit unions
- Savings and loan associations
- Federal credit unions
- Trust companies

#### Investment & Securities
- Securities brokers and dealers
- Investment advisors
- Mutual funds
- Investment companies

#### Insurance
- Insurance companies (life, health, property, casualty)
- Insurance agents and brokers

#### Lending
- Mortgage lenders and brokers
- Auto lenders
- Payday lenders
- Consumer finance companies
- Check cashing services

#### Credit Services
- Credit counseling services
- Debt collectors
- Credit reporting agencies

#### Tax & Financial Services
- Tax preparation services
- Financial planners
- Real estate settlement services
- Courier services (for financial documents)

#### "Finders" (under Dodd-Frank)
- Anyone compensated for arranging financial transactions

### Personal & Corporate Applicability

#### Personal Finance Documents
**GLBA Applies** when processing:
- Personal bank statements ✅
- Personal loan applications ✅
- Personal investment statements ✅
- Personal insurance policies ✅
- Personal tax returns (with financial institution data) ✅
- Personal credit reports ✅
- Personal credit card statements ✅

#### Corporate Finance Documents  
**GLBA Applies** when processing:
- Employee compensation data (personal financial info) ✅
- Executive personal financial disclosures ✅
- Board member personal contact/financial data ✅
- Employee benefit account information ✅

**GLBA Does NOT Apply** to:
- Pure corporate financial statements (no individual NPI)
- Business-to-business transactions (unless sole proprietor)
- Public company disclosures (already public information)

---

## Penalties

### Civil Penalties

#### Administrative Fines
- **Individuals**: Up to **$100,000 per violation**
- **Organizations**: Up to **$500,000 per violation**
- Enforced by: FTC, Federal banking regulators

#### State Enforcement
- State attorneys general can also enforce GLBA
- Additional state-level fines may apply

#### Private Right of Action
- **Limited**: No general private right of action under GLBA
- Exception: Some states allow private lawsuits for violations

### Criminal Penalties

#### Pretexting Violations (15 USC § 6823)
- **Basic Offense**: Up to 5 years imprisonment + fines
- **Selling Obtained Information**: Up to 10 years imprisonment + fines
- **Corporate Officers**: Can be held personally liable

### Recent Enforcement Examples

**FTC Enforcement Actions**:
- 2023: $X million settlement for inadequate data security (example)
- Common violations: Failure to provide privacy notices, inadequate safeguards, unauthorized disclosure

**Banking Regulator Actions**:
- Consent orders requiring compliance programs
- Mandatory third-party audits
- Civil money penalties for repeat violations

---

## Applicability Rules

### When GLBA Applies in ShieldText

```yaml
glba_applies_if:
  # Document contains financial institution + personal data
  conditions:
    all_of:
      - document_type:
          - bank_statement
          - loan_application
          - investment_statement
          - credit_report
          - insurance_policy
          - tax_return (with financial data)
          - credit_card_statement
      - contains_personal_identifiers:
          - name AND (account_number OR ssn OR financial_data)
  
  OR
  
  # Identifiers detected indicating NPI
  identifiers_detected:
    critical:
      - F2 (SSN)
      - F10-F24 (Any account numbers)
    with_name:
      - F1 (Name) + any financial identifier
```

### When GLBA Does NOT Apply

```yaml
glba_not_applicable_if:
  conditions:
    any_of:
      - no_financial_institution_involved: true
      - no_personal_identifiers: true
      - only_public_information: true
      - only_corporate_data: true (no individual NPI)
      - document_type:
          - medical_records (HIPAA, not GLBA)
          - education_records (FERPA, not GLBA)
          - employment_records (unless financial accounts)
```

### Detection Logic

```
Step 1: Identify Document Type
IF document contains:
   - Bank name/logo OR
   - "Account Number:" OR
   - "Routing Number:" OR
   - "Credit Card Number:" OR
   - "Loan Account:" OR
   - "Investment Account:"
THEN: Likely financial document

Step 2: Identify Personal Data
IF document also contains:
   - Personal name AND
   - (SSN OR account number OR balance OR transaction history)
THEN: Contains NPI

Step 3: Apply GLBA
IF (Step 1 = TRUE) AND (Step 2 = TRUE)
THEN:
   GLBA = APPLICABLE
   Status: Must comply with GLBA requirements
ELSE:
   GLBA = NOT APPLICABLE
   Status: Check other regulations
```

---

## Interaction with Other Regulations

### GLBA + CCPA (California Consumer Privacy Act)
- **Overlap**: Both protect personal financial information
- **CCPA Carve-Out**: CCPA exempts information covered by GLBA
- **In Practice**: For California residents' financial data, GLBA takes precedence
- **ShieldText Approach**: Apply both standards (more protective)

### GLBA + PCI DSS (Payment Card Industry)
- **Overlap**: Both protect payment card data
- **Scope**: GLBA covers broader financial data, PCI DSS specific to card processing
- **In Practice**: Apply both (PCI DSS is more strict for card data)
- **ShieldText Approach**: Use PCI DSS rules for cards, GLBA for other financial data

### GLBA + SOX (Sarbanes-Oxley)
- **Overlap**: Minimal (SOX is corporate governance, GLBA is consumer privacy)
- **Interaction**: Corporate documents with employee NPI require both
- **ShieldText Approach**: Apply GLBA for employee personal financial data in SOX contexts

### GLBA + State Privacy Laws
- **State Laws**: Some states have additional financial privacy requirements
- **GLBA as Floor**: State laws can be more protective, not less
- **ShieldText Approach**: Apply strictest standard

---

## References

### Legal Citations
- **Primary Statute**: 15 USC §§ 6801-6809
- **Privacy Rule**: 15 CFR Part 313
- **Safeguards Rule**: 16 CFR Part 314
- **Pretexting**: 15 USC § 6821-6827

### Official Resources
- **FTC GLBA Page**: https://www.ftc.gov/business-guidance/privacy-security/gramm-leach-bliley-act
- **Privacy Rule**: https://www.ecfr.gov/current/title-15/subtitle-B/chapter-I/subchapter-C/part-313
- **Safeguards Rule**: https://www.ecfr.gov/current/title-16/chapter-I/subchapter-C/part-314

### Guidance Documents
- FTC: "How to Comply with the Privacy of Consumer Financial Information Rule"
- FTC: "Standards for Safeguarding Customer Information" (Safeguards Rule guidance)
- Federal banking agencies: Interagency guidance on response programs for unauthorized access

### Enforcement
- **FTC Enforcement Actions**: https://www.ftc.gov/enforcement
- **OCC Enforcement Actions**: https://www.occ.gov/topics/supervision-and-examination/enforcement-actions

---

## Document History

**Last Updated**: April 30, 2026  
**Version**: 1.0  
**Changes**:
- Initial documentation for ShieldText financial redaction skill
- Reflects 2021 Safeguards Rule amendments

**Next Review**: When GLBA regulations are amended or updated
