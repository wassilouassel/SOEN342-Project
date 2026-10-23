# System operations and contracts

These are conceptual system events at the CLI/system boundary, not methods on implementation classes. IDs and outputs denote domain identities. Actor/session is implicit. SSD event names and parameter order use the signatures below.

## Shared contract rules

**Context:** each event is invoked by an authenticated actor. Role permission and patient/care scope are checked on every call (requirements A05–A06); they are runtime validations, not assumptions that allow an unauthorized request to mutate state. Dates, IDs, money, text lengths and enum values are validated. Optional parameters are marked `?`. A failed authorization returns `accessDenied` without disclosing clinical information or patient existence to an unauthorized party.

**Atomicity/minimum guarantee:** a rejected command creates no appointment, consultation, prescription, referral, fee, invoice or payment and changes no existing domain links/values. A failed read discloses no protected data. A separate audit event for the rejected attempt may still be created. Successful commands create an AuditEvent linked to the acting UserAccount and target, with time/action/outcome; authorized clinical reads and audit reviews also do so. Audit payload contains no clinical narrative. Audit recording must succeed with a protected action; otherwise that action fails without domain changes. Patient-linked identity and appointment-summary reads are sensitive and must be audited before disclosure; aggregate availability queries need not create domain objects or audit events unless they disclose sensitive data.

**Booking invariant B:** patient, healthcare professional with clinic practice, service and location exist; professional is eligible for that service at that location; start is in the future at request time; complete service-duration interval lies in an effective schedule; no overlapping requested/confirmed reservation exists for that professional across any location or for that patient; any required referral belongs to the patient, is active, and matches professional/specialty. Time comparisons use clinic time zone with unambiguous instants. Validate B at commit, not only at search. Cancelled appointments do not reserve capacity. Existing completed intervals cannot be double-booked retroactively. Changes to schedules/profiles cannot invalidate existing requested/confirmed appointments without first resolving them explicitly.

**Failure vocabulary:** `invalidInput`, `notFound`, `accessDenied`, `conflict`, `invalidState`, `slotUnavailable`, `referralRequired`, `referralInvalid`, `alreadyArrived`, `wrongDate`, `auditUnavailable`. Specific errors below refine this vocabulary. Expected rejections are defined behavior, not exceptions to the minimum guarantee.

## Critical-workflow operations

### O01 — `searchPatient(criteria)` — UC02, UC03, UC07
- **Context/preconditions:** clinic identity data exists; actor has identity-search authority (patient searches are self-only).
- **Input:** at least one supported field: insurance number, date of birth, phone, partial first name and/or partial last name. Supplied fields combine conjunctively; ambiguous matches require human identity confirmation. Case/format normalization must not merge different patient identities.
- **Success/output:** return zero or more minimal matching identities (patientID, names and necessary disambiguators), never clinical content. No domain changes.
- **Failure:** malformed/empty criteria → `invalidInput`; unauthorized scope → `accessDenied`. Zero matches is a normal empty result; the workflow may stop or branch to UC01, never silently create a patient.

### O02 — `searchAvailability(serviceID, professionalID, locationID, dateRange)` — UC03, UC05, UC11
- **Context/preconditions:** actor may request scheduling information in the specified scope.
- **Success/output:** return available start/end intervals for service duration satisfying schedule, professional/location eligibility and professional-capacity overlap constraints at query time; empty list if none. With no patientID in this query, patient-specific overlap and referral eligibility are checked only by the subsequent write. No reservation or clinical disclosure. Referral is validated at write, not inferred by this query.
- **Failure:** malformed range → `invalidInput`; missing catalogue identities → `notFound`; unauthorized → `accessDenied`.

### O03 — `requestAppointment(patientID, serviceID, professionalID, locationID, startTime, referralID?)` — UC03
- **Context/preconditions:** authenticated patient for self or authorized clinic staff; catalogue/schedule have been established.
- **Success postconditions:** one new Appointment is created with unique identity, startTime, stored bookedDurationMinutes and derived endTime, status `requested`, absent arrivalTime and requestedBy actor. It is associated with exactly the selected Patient, Service, HealthcareProfessional (clinic practice) and ClinicLocation, and with the supplied valid Referral if any. Capacity is reserved. Existing appointments and referrals are not changed. An AuditEvent records creation.
- **Output:** appointmentID, start/end, location and `requested` (not falsely reported as confirmed).
- **Failure:** absent patient/catalogue ID → `notFound`; malformed input → `invalidInput`; insufficient scope → `accessDenied`; failed capacity/schedule/eligibility checks in B → `slotUnavailable`; missing required referral → `referralRequired`; supplied mismatched/inactive/nonexistent referral → `referralInvalid`. No reservation is created on rejection. Simultaneous requests for a single slot cannot both succeed.

### O04 — `findAppointment(patientID, date)` — UC07
- **Context/preconditions:** authorized arrival staff (or a separately authorized actor in another goal); patient identity has been confirmed.
- **Success/output:** return matching appointment IDs, start/end, location, professional, status and arrival state for the clinic-local date, without clinical narratives. Return empty list when none. Multiple matches require explicit selection. No domain changes.
- **Failure:** malformed date → `invalidInput`; absent patient → `notFound`; scope violation → `accessDenied`.

### O05 — `registerArrival(appointmentID)` — UC07
- **Context/preconditions:** authenticated administrative staff; identity and selected appointment have been checked by the human.
- **Success postconditions:** the existing confirmed Appointment's arrivalTime becomes current time and its registering actor is retained in the linked AuditEvent. Appointment remains `confirmed`; no new Appointment or Consultation is created. AuditEvent records arrival.
- **Output:** appointmentID, arrivalTime, `confirmed`.
- **Failure:** missing appointment → `notFound`; unauthorized → `accessDenied`; not confirmed → `invalidState`; different clinic-local date → `wrongDate`; arrivalTime already present → `alreadyArrived`. Original timestamp is never overwritten by a duplicate.

### O06 — `viewClinicalRecord(patientID)` — UC09, UC08
- **Context/preconditions:** authenticated clinic professional authorized for this patient's care. Administrative role alone is insufficient.
- **Success/output:** return authorized clinical record content, consultations and relevant prescriptions/referrals. Create an AuditEvent for the read; no clinical objects are changed.
- **Failure:** forbidden care scope → `accessDenied` (no record content); absent accessible patient/record → `notFound`; audit failure → `auditUnavailable` with no disclosure.

### O07 — `recordConsultation(appointmentID, observations, diagnoses, treatmentRecommendations, followUpRequirements)` — UC08
- **Context/preconditions:** authenticated authorized treating professional. At least one clinically meaningful field must be present; individual fields may be absent when not relevant.
- **Success postconditions:** Appointment is confirmed, arrived and assigned to the acting professional; it has no existing Consultation. A new uniquely identified Consultation is created with supplied clinical fields and current recordedAt. It is associated with that Appointment, its Patient's existing ClinicalRecord, and the treating HealthcareProfessional. The Appointment status becomes `completed`; its arrivalTime and scheduling links are preserved. AuditEvent records action and target without the narrative.
- **Output:** consultationID and appointment status `completed`.
- **Failure:** missing accessible appointment → `notFound`; unauthorized or wrong treating professional → `accessDenied`; absent arrival/non-confirmed status/existing Consultation → `invalidState`; empty or malformed content → `invalidInput`. Rejection cannot leave a partial consultation or completed appointment.

### O08 — `issuePrescription(consultationID, items)` — UC10 (UC08 extension)
- **Context/preconditions:** acting professional has explicit prescribing authorization and care scope for this Consultation, not merely any clinical role.
- **Success postconditions:** create Prescription with unique identity and issueDate, linked to the Consultation and issuing HealthcareProfessional. For each item create one PrescriptionItem linked to that Prescription and an existing Medication, with nonempty dosage and instructions; there must be at least one item. AuditEvent records issuance. Consultation is not rewritten and no pharmacy is contacted.
- **Output:** prescriptionID and recorded items.
- **Failure:** inaccessible consultation → `notFound`/`accessDenied` as appropriate; missing medication, empty list, missing dosage/instructions → `invalidInput`. Entire prescription is rejected, not partially saved.

### O09 — `requestFollowUp(consultationID, serviceID, professionalID, locationID, startTime, referralID?)` — UC11 (UC08 extension)
- **Context/preconditions:** authorized treating professional with access to the originating Consultation; patient is derived from its clinical record, never caller-substituted.
- **Success postconditions:** create one new `requested` Appointment as O03, satisfying B and linked to the originating Consultation as follow-up. An optional valid referral association is created. Original consultation/appointment remains unchanged. AuditEvent records the request.
- **Output:** follow-up appointmentID, time/location and `requested`.
- **Failure:** inaccessible consultation → `notFound`/`accessDenied`; malformed input → `invalidInput`; B violations → `slotUnavailable`, `referralRequired` or `referralInvalid`. Failure does **not** roll back a previously recorded Consultation or Prescription; only this independent command is atomic.

## Supporting operation contracts

The shared access, atomicity and audit guarantees apply to every row. `details`/`changes` are the explicit fields identified in the input column, not arbitrary object updates. These contracts identify the remaining system boundary without pretending that all supporting goals require fully dressed scenarios this iteration.

| ID / operation / use case | Input and validation | Success postconditions/output | Rejection (no domain changes) |
|---|---|---|---|
| O10 `registerPatient(details)` UC01 | Authorized staff; names, birth date, phone, optional insurance memberships (kind RAMQ/private, insurer and insurance number); confirm possible duplicates rather than assuming insurance ID is universal | Create unique Patient, its one ClinicalRecord and zero or more PatientInsurance memberships; return patientID | Invalid demographic values, conflicting confirmed duplicate, forbidden role |
| O11 `updatePatient(patientID, changes)` UC01 | Staff; same editable fields as O10; ID immutable | Update only supplied demographics/PatientInsurance memberships; preserve appointments/record | Missing patient, invalid values, unresolved duplicate, forbidden role |
| O12 `confirmAppointment(appointmentID)` UC04 | Self patient or staff; status requested; reservation remains valid | Existing status becomes confirmed; preserve all links/reservation | Missing, unauthorized, invalidState, invalidated schedule/referral |
| O13 `rescheduleAppointment(appointmentID, professionalID, locationID, startTime, referralID?)` UC05 | Self patient or staff; requested/confirmed, not arrived; same patient/service; B with original reservation excluded from conflict check | Atomically move professional/location/time/referral links; preserve requested/confirmed status and identity; release old interval only on success | Missing, unauthorized, arrived/completed/cancelled, B violation; old reservation retained on failure |
| O14 `cancelAppointment(appointmentID)` UC06 | Self patient or staff; requested/confirmed, not arrived | Status becomes cancelled; reservation released; historical links retained | Missing, unauthorized, invalidState (including arrived); no history deletion |
| O15 `recordReferral(patientID, referringProfessionalID, specialtyID?, targetProfessionalID?, clinicalReason, priority)` UC12 | Authorized clinical issuer or staff transcribing a received referral; patient/referrer exist; at least one valid target; both targets compatible; nonempty reason and recognized priority; staff cannot originate clinical decision | Create unique active Referral with issueDate and patient/referrer/target links; external referrer permitted without clinic schedule | Missing/invalid target/reason/priority, incompatible targets, unauthorized issuance |
| O16 `updateReferralStatus(referralID, status)` UC13 | Authorized within role; recognized transition active→closed/cancelled; reject change invalidating an existing requested/confirmed appointment until that booking is resolved | Set status, preserving referral and appointment history | Missing, forbidden, invalid transition, active-booking conflict |
| O17 `saveProfessional(professionalID?, details)` UC14 | Authorized staff; name, professional type, licence where applicable, clinic/external classification, specialty IDs, clinic location IDs, qualifiedServiceIDs; all service IDs must exist and match professional qualifications; external may have no clinic location | Create or update HealthcareProfessional and specialty/practice-location/qualified-Service associations; return professionalID | Invalid specialty/location/identity, forbidden; change invalidates booked appointment |
| O18 `saveLocation(locationID?, name, address, phone, timeZone)` UC14 | Authorized staff; valid fields and clinic time zone | Create/update ClinicLocation; preserve booked location identity | Missing update target, malformed data, forbidden |
| O19 `saveSpecialty(specialtyID?, name)` UC14 | Authorized staff; nonempty unique name | Create/update Specialty; return identity | Missing update target, duplicate/invalid name, forbidden |
| O20 `saveMedicalService(serviceID?, name, durationMinutes, requiresReferral, coveragePolicy, standardFee?, specialtyID?)` UC15 | Authorized catalogue manager; positive duration; coveragePolicy RAMQ-covered/uninsured/conditional; nonnegative optional CAD fee; eligibility is not a claim guarantee | Create/update Service and optional specialty link | Invalid values/target, forbidden, duration change invalidates reservation |
| O21 `saveSchedule(scheduleID?, professionalID, locationID, weekday, startTime, endTime, effectiveFrom, effectiveTo, serviceIDs)` UC15 | Authorized professional for own schedule or staff; clinic professional practises at location, positive interval/date range, eligible services | Create/update the professional's Schedule and its dated AvailabilityPeriods with location/service associations | Malformed/conflicting schedule, invalid links, forbidden, change invalidates bookings |
| O22 `recordFee(appointmentID, description, amount, coverageAssessment)` UC16 | Authorized billing staff; amount ≥0 CAD; nonempty description; coverageAssessment contains outcome (RAMQ-covered/uninsured/undetermined) and nonempty administrative basis for this patient/service; only a manually established uninsured outcome permits charging | Create uninvoiced Fee linked to Appointment with assessed amount and immutable coverageOutcome=uninsured/coverageBasis snapshot; audit actor/action/target, not assessment payload | Missing appointment, RAMQ-covered/undetermined outcome, absent basis, invalid amount, unauthorized; no fee created |
| O23 `generateInvoice(patientID, feeIDs)` UC16 | Authorized staff; nonempty fees all for this patient, not previously invoiced | Create Invoice linked to Patient, one owned InvoiceLine per selected Fee (quantity 1, chargedUnitAmount=fee amount), and each line's Fee link; total=sum line amounts; derived status unpaid (or paid for zero total) | Missing fees/patient, mixed patients, already invoiced, forbidden |
| O24 `registerPayment(invoiceID, amount, method, reference?)` UC17 | Authorized staff; amount >0 and ≤unpaid balance; supported method; manually verified receipt | Create Payment linked to Invoice; balance decreases; status partiallyPaid or paid | Missing invoice, invalid/overpayment, forbidden; does not charge a card or submit insurance |
| O25 `reviewAuditTrail(filters)` UC18 | Dedicated audit-review permission; bounded date range and optional actor/action/target/outcome filters | Return matching metadata-only AuditEvents; record audit review itself | Unauthorized, invalid filters, auditUnavailable; never expose clinical payload |

### Recorded coverage decision (O20/O22)

Service.coveragePolicy is the general RAMQ-covered/uninsured/conditional catalogue policy, not proof of actual coverage for a particular patient. Before O22, authorized billing staff manually assess the patient/service context under clinic policy, including relevant insurance membership, and supply the outcome and administrative basis. The system records that decision with a successfully assessed Fee; it does not adjudicate eligibility. An uninsured catalogue policy supports an uninsured decision; RAMQ-covered or conditional policies still require the actual encounter decision rather than inferring it from membership. A claimed uninsured exception to a RAMQ-covered catalogue policy must be supported by the recorded basis. An undetermined or actually RAMQ-covered outcome always rejects O22. Assessment rules and permitted exceptions need stakeholder validation; no RAMQ/insurer API is contacted. The Fee snapshot preserves why a charge was allowed even if catalogue/insurance data changes. Avoid diagnoses and insurance numbers in the basis, and never copy it into audit metadata.

## State consistency examples

1. Two actors see one available slot. The first O03 reserves it; the second receives `slotUnavailable` and creates no Appointment.
2. A referral is required and absent. O03/O09 returns `referralRequired`; no booking exists despite an earlier availability response.
3. O05 called twice: first stores arrivalTime; second returns `alreadyArrived` and cannot alter it.
4. O07 succeeds but O08/O09 later fails: the completed consultation remains intact; the optional failure is reported independently.
5. A staff user calls O06: return `accessDenied`, record denied action metadata, disclose no diagnoses.
