# Requirement-to-artifact traceability

The supplied brief is [`requirements.md`](requirements.md)'s source, not the original draft diagrams. “Covered” here means represented in analysis; no runtime implementation is claimed.

| Requirement | Use cases / operations | Evidence |
|---|---|---|
| R01 Patient identity, optional insurance | UC01; O10–11 | Domain Patient/ClinicalRecord; contracts |
| R02 All five patient-search criteria | UC02; O01 | Search contract; UC03/UC07 flows and SSDs |
| R03 Professional types, specialties, multiple locations | UC14; O17–19 | HealthcareProfessional kind/practice-kind attributes, specialty and practice-location associations |
| R04 Locations, services, schedules | UC14–15; O18–21 | Domain Schedule/AvailabilityPeriod/Service/ClinicLocation; invariant B |
| R05 Patient/staff requests, scheduling checks | UC03; O02–03 | Fully dressed UC03; request SSD success and failure |
| R06 Conditional referral | UC03/UC11/UC12; O03/O09/O15 | Required-referral failures, domain Referral targets, invariant B |
| R07 Confirm/reschedule/cancel | UC04–06; O12–14 | Use-case model and lifecycle contracts |
| R08 Day-of arrival | UC07; O04–05 | Fully dressed UC07, arrival SSD, arrivalTime and registering actor in AuditEvent |
| R09 Consultations and clinical fields | UC08; O07 | Fully dressed UC08, consultation SSD and domain |
| R10 Multi-medication prescriptions, follow-up | UC10–11; O08–09 | UC08 extensions; PrescriptionItem multiplicity and follow-up association |
| R11 Referrals incl external referrers and multiple resulting appointments | UC12–13; O15–17 | Domain referral associations and fields; assumptions A03/A08 |
| R12 RAMQ/uninsured/private, fees/invoices/payments | UC01/UC16–17; O10–11/O20/O22–24 | Coverage data, financial domain and payment contracts |
| R13 Role-based clinical access | UC09; O06–08 and shared rules | UserAccount/roles, UC08 denial and SSD, A05–06 |
| R14 Accountability/confidentiality | UC18; O25 and shared audit rule | AuditEvent model; read/write/denial audit without narratives |
| R15 External integrations excluded | All | Requirements boundary; no claim/payment/pharmacy integration |
| R16 CLI/OOP eventual product | All | README and requirements scope; conceptual model is not application code |

## Deliverable checklist

| Deliverable | Submission evidence |
|---|---|
| D01 Critical and non-critical use-case diagram | `diagrams/use-cases.puml` and `diagrams/use-cases.png`; classification catalogue in requirements |
| D02 UML domain model | `diagrams/domain-model.puml` and `diagrams/domain-model.png` |
| D03 Success/failure SSDs for critical cases | `diagrams/ssd-request-appointment.*`, `diagrams/ssd-register-arrival.*`, `diagrams/ssd-record-consultation.*` |
| D04 Three fully dressed critical scenarios, success and failure each | `scenarios.md` UC03, UC07, UC08; matched SSDs; `supporting-scenarios.md` adds brief success/failure flows for every supporting goal |
| D05 System operation identification/contracts | `operation-contracts.md` O01–O25; critical event names match SSDs |

## Submission scope

This folder contains the final analysis artifacts only. Diagrams are supplied as PNG images with their editable PlantUML sources. Original drafts and internal review reports are excluded. Requirements and additional analyst assumptions are distinguished explicitly.

## Validation limits

This is a document/diagram review, not execution testing of a clinic application. Render diagrams and inspect their PNGs; validate file links, the three critical SSDs' success/failure branches, domain multiplicities and exact operation names against contracts. The preliminary iteration's assessment section was not supplied; conformance to that unseen material cannot be asserted.
