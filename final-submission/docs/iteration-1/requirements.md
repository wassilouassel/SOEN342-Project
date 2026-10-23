# Iteration 1 — requirements and scope

## Source and interpretation

The authoritative source is the supplied four-page iteration-one assignment brief. Page references below refer to that brief. This document is a structured extraction, not a replacement for the original assignment. No marking rubric or preliminary-iteration assessment instructions were supplied; their contents cannot be verified.

Page 1 requires analysis for iteration one. The **eventual** product is an object-oriented **command-line application with no graphical or web interface**. This submission is analysis, not a claim that an application has been implemented.

## System requirements

| ID | Requirement | Source |
|---|---|---|
| R01 | Maintain uniquely identified patients; insurance identifiers are optional. | p2 |
| R02 | Search by insurance number, date of birth, phone, partial first name and/or last name. | p2 |
| R03 | Maintain healthcare professionals (family physicians, specialists, nurses, others), professional information and specialties; support practice at multiple locations. | p2 |
| R04 | Maintain clinic locations, services and professional schedules. | p2 |
| R05 | Patients or authorized clinic staff may request appointments; check professional availability and scheduling constraints. | p2 |
| R06 | Where required by the service, validate a referral from a physician or authorized healthcare professional. | p2 |
| R07 | Support appointment confirmation, rescheduling and cancellation. | p2 |
| R08 | Administrative staff register patient arrival on the appointment day. | p3 |
| R09 | Healthcare professionals conduct consultations and record observations, diagnoses, treatment recommendations and follow-up requirements as relevant. | p3 |
| R10 | Support prescriptions containing one or more medications and requests for follow-up appointments. | p3 |
| R11 | Maintain referrals with referring professional, requested specialty or professional, clinical reason, priority and status; a referral may lead to multiple appointments. External referring physicians need not practise at the clinic. | p3 |
| R12 | Distinguish RAMQ coverage from uninsured paid services, direct payment and private insurance; record applicable fees, generate invoices and register payments for uninsured services. | p2–3 |
| R13 | Maintain patient clinical records; enforce role-dependent access, with administrative access distinct from authorized clinical access. | p2–3 |
| R14 | Record relevant activities for accountability and confidentiality. | p3 |
| R15 | Exclude electronic RAMQ claims, electronic pharmacy prescribing and integration with provincial systems at this phase. | p3 |
| R16 | Eventual implementation is CLI-only and object-oriented. No application implementation is required in this analysis iteration. | p1 |

## Required deliverables

| ID | Deliverable | Source |
|---|---|---|
| D01 | Use-case diagram identifying critical and non-critical use cases. | p4 |
| D02 | UML domain model. | p4 |
| D03 | System sequence diagrams showing success and failure for critical use cases. | p4 |
| D04 | Three fully dressed scenarios for three critical use cases, each with at least one success and one failure case; corresponding SSDs. | p4 |
| D05 | Identify system operations and provide operation contracts. | p3–4 |

## Analysis assumptions requiring instructor/stakeholder confirmation

These rules make the scenarios precise; they are **not quoted requirements**.

- **A01 — Criticality:** UC03 Request Appointment, UC07 Register Arrival and UC08 Conduct Consultation and Record Clinical Information are the three critical workflows. The brief gives booking for a patient already at the clinic as an example, not a mandatory exclusive selection. UC03 supports that walk-in booking through administrative staff; UC07 covers arrival for an existing booking.
- **A02 — Lifecycle:** `requested` reserves capacity, `confirmed` permits arrival, `completed` follows a recorded consultation, and `cancelled` releases capacity. Rescheduling changes the reservation rather than introducing a persistent `rescheduled` status. Arrival is an optional timestamp, not another lifecycle state. A reserved slot cannot overlap another requested/confirmed appointment for the professional across locations or for the patient; service duration determines its end. A completed appointment continues to occupy its historic interval.
- **A03 — Booking validation:** Validate again at booking/rescheduling/follow-up commit, including schedule, location, duration, service eligibility, conflicts and required referral; a previous availability response is not a reservation. Referral must concern this patient, be active and match requested specialty/professional. Referral expiry and urgency policies are unresolved.
- **A04 — Arrival:** Staff confirm identity; arrival requires a confirmed, non-arrived appointment on the current clinic-local date. No unapproved early/late grace-period restriction is invented. Duplicate arrival is rejected without changing the original time.
- **A05 — Clinical authorization:** Only a clinic professional authorized for this patient's care may read clinical records or record consultation/prescription/follow-up. Recording the consultation requires a confirmed, arrived appointment assigned to that professional; there is at most one recorded consultation per appointment in this iteration. Clinical write privileges within professional types require stakeholder confirmation; being a nurse is not by itself prescribing authorization.
- **A06 — Access and audit:** Authenticated session and role checks are implicit for all operations. Patients can act only for themselves; staff act only within assigned authority. Patient lookup returns minimum identity data, never clinical details, and is restricted to authorized staff or the patient's own identity. Record successful changes, clinical reads, audit review and denied attempts with actor, action, target, timestamp and outcome, without copying diagnoses or other clinical payload into the audit log. Dedicated audit-review permission is not granted automatically to every staff member.
- **A07 — Financial recording:** Fees are nonnegative money amounts in CAD; payment amounts are positive and cannot exceed the unpaid balance. Partial payments are allowed. Registering a payment records receipt; it is not payment-gateway integration or an insurance claim. Private-insurance details do not automatically make a service RAMQ-covered. O22 accepts a manually established patient/service coverage outcome and administrative basis; only uninsured outcomes permit a Fee, which retains that snapshot. The system does not adjudicate eligibility. No tax, reimbursement or tariff policy is assumed.
- **A08 — External referrals:** Authorized clinic users may record referrals received from external professionals; external integration/login is not required. A referral requests a specialty, a professional, or both; if both, they must be compatible. It may be linked to zero or many appointments.
- **A09 — Boundaries:** Identity/session provisioning, backup/retention policy, exact schedule constraints, consent rules, drug-interaction checking and statutory billing/prescribing rules are unresolved, not silently treated as implemented. Invalid user input produces a controlled rejection, not a broken precondition blamed on the user.

## Use-case catalogue

Critical means selected for detailed risk-bearing analysis; non-critical means supporting in this iteration, **not** optional functionality or low clinical importance. Every listed goal is inside the eventual CLI system boundary.

| ID | Goal | Primary actor(s) | Iteration classification |
|---|---|---|---|
| UC01 | Manage Patient Registration | Administrative staff | Supporting/non-critical |
| UC02 | Search Patient | Authorized staff/professional; patient for self | Supporting/non-critical |
| UC03 | Request Appointment | Patient, authorized clinic staff | **Critical** |
| UC04 | Confirm Appointment | Patient, authorized clinic staff | Supporting/non-critical |
| UC05 | Reschedule Appointment | Patient, authorized clinic staff | Supporting/non-critical |
| UC06 | Cancel Appointment | Patient, authorized clinic staff | Supporting/non-critical |
| UC07 | Register Arrival | Administrative staff | **Critical** |
| UC08 | Conduct Consultation and Record Clinical Information | Authorized treating professional | **Critical** |
| UC09 | View Clinical Record | Authorized healthcare professional | Supporting/non-critical |
| UC10 | Issue Prescription | Authorized prescriber | Supporting/non-critical |
| UC11 | Request Follow-up Appointment | Authorized treating professional | Supporting/non-critical |
| UC12 | Issue/Record Referral | Authorized professional; staff for received external referral only | Supporting/non-critical |
| UC13 | Update Referral Status | Authorized professional/administrative staff within role | Supporting/non-critical |
| UC14 | Manage Professional Profile | Authorized administrative staff | Supporting/non-critical |
| UC15 | Manage Schedule | Authorized professional/administrative staff | Supporting/non-critical |
| UC16 | Record Fees and Generate Invoice | Authorized administrative staff | Supporting/non-critical |
| UC17 | Register Payment | Authorized administrative staff | Supporting/non-critical |
| UC18 | Review Audit Trail | User with dedicated audit-review permission | Supporting/non-critical |

There is no electronic external-system actor: RAMQ, insurers, pharmacies and provincial systems are outside the implemented boundary. Clinic location, specialty and medical-service catalogue maintenance are covered by UC14–15 and their system operations; they are not hidden database setup prerequisites.
