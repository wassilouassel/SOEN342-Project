# Iteration 1 — Appointment Tracking System analysis

## 1. Scope, authority, and reading guide

**System under study:** the clinic Appointment Tracking System (ATS). This iteration produces analysis artifacts only, not an application, software design, database schema, or integration. The eventual system is an object-oriented **command-line application**, not a graphical/web interface [S1].

**Authoritative assignment:** all four supplied assignment pages were inspected; those images are not bundled in this submission. Existing PDFs and their exported PNGs elsewhere in the repository are legacy drafts, not requirements authority.

| Source | Requirements used here |
| --- | --- |
| S1 — `project-requirements/page-1.png` | Iteration-one analysis; eventual CLI and object-oriented implementation. |
| S2 — `project-requirements/page-2.png` | Patients and search keys; professionals, specialties, locations and schedules; RAMQ/private insurance/uninsured services; appointment requests, availability, scheduling constraints, conditional referrals, confirmation, rescheduling and cancellation. |
| S3 — `project-requirements/page-3.png` | Administrative arrival registration; consultation information, prescriptions and follow-ups; internal/external referring professionals and referral details; fees, invoices and payments; role-restricted access and accountability. Electronic RAMQ claims, pharmacy transmission and provincial integrations are outside scope. |
| S4 — `project-requirements/page-4.png` | Critical/supporting use cases, UML domain model, three fully dressed critical scenarios, corresponding success/failure SSDs, system operations and operation contracts. |

**Authority labels:** `[S#]` means explicit source evidence; `[D#]` means a provisional analysis decision (not a stakeholder-approved clarification); `[A#]` means an analyst assumption requiring stakeholder validation. A source's silence is not treated as evidence for an assumption. Specific role allocations, lifecycle rules and security details below are provisional analysis decisions or assumptions, not quotations from the assignment.

### Provisional analysis decisions

The analyst selected these working rules to make the partial brief precise; they require stakeholder/instructor confirmation just like the A-numbered assumptions. They are not additional instructions from the user or source. `requirements.md` A01–A09 summarize the same decisions; the D/A/BR identifiers here are local cross-references.

| ID | Clarification |
| --- | --- |
| D1 | Only UC03, UC07 and UC08 are critical for this iteration. Every other catalogue goal is supporting/noncritical, **not unimportant**. |
| D2 | Patients may request/confirm/reschedule/cancel their own appointments; authorised clinic staff may do so on behalf of patients. Patient lookup is limited by role and ownership. The session actor is implicit in every operation. |
| D3 | Appointment statuses are exactly `requested`, `confirmed`, `completed`, `cancelled`. A successful request reserves the slot; confirmation is separate. Rescheduling changes the time, not the status. Arrival is an independent nullable `arrivalTime`. |
| D4 | Arrival requires a confirmed appointment on its location's local calendar date. A consultation requires confirmed status, recorded arrival, and the authorised treating professional. Recording it creates the consultation, links it to the patient's clinical record, and completes the appointment. |
| D5 | `requestFollowUp` creates a requested, slot-reserving appointment linked to its originating consultation and uses the same scheduling/referral validation as UC03. A professional can search availability first. Prescriptions need a nonempty item list with medication, dosage and instructions per item. |
| D6 | Healthcare professionals request follow-ups and issue referrals. Authorised clinic professionals/staff capture referrals originating with external professionals; no external integration is promised. A referral targets a specialty, a professional, or both. External professionals need not practise at any clinic location. |
| D7 | Preserve role-restricted clinical access; audit successful writes, sensitive reads and denied actions without recording clinical content. Model user accounts, roles and audit events conceptually, not as implementation classes. |
| D8 | Use the exact operation names and parameters listed in §8. Three SSDs are black-box actor/system interactions only. Non-sequence diagrams use Smetana. |

### Artifact map

- `diagrams/use-cases.puml`: all 18 catalogue goals; three critical goals distinguished; conditional referral checking shown with `«extend»`.
- `diagrams/domain-model.puml`: conceptual objects, relationships, multiplicities, constraints, access roles and audit.
- `diagrams/ssd-request-appointment.puml`: UC03 main success and lookup/authorization/availability/referral/booking failures.
- `diagrams/ssd-register-arrival.puml`: UC07 main success, lookup failures, invalid state/date, duplicate arrival.
- `diagrams/ssd-record-consultation.puml`: UC08 success, clinical-access/recording failure, optional prescription and follow-up success/failure.

The three SSDs deliberately use just **one initiating actor and the ATS boundary** each. Validation, audit and reservation are system responsibilities, not internal lifelines or calls to invented services.

## 2. Actors and stakeholders

| Actor or stakeholder | Goals and interests |
| --- | --- |
| Patient | Accurate registration and own appointments; convenient, nonconflicting appointments; confidentiality; correct clinical and financial records. A patient's account cannot browse other patients or clinical records under this analysis. |
| Authorised clinic staff | Shared scheduling role for patient lookup and request/confirmation/reschedule/cancellation on behalf of a patient. Authorisation is operation- and patient-scoped, not blanket clinical access. |
| Administrative staff | A specialisation of authorised clinic staff: registration, arrival, permitted external-referral capture/status handling, fees/invoices and payments. No general right to clinical narrative. |
| Healthcare professional | A specialisation of authorised clinic staff: authorised clinical-record access, consultations for their patients, prescriptions, referrals and follow-up requests. Includes family physicians, specialists, nurses and other professionals [S2]; prescribing authority is narrower than merely holding this role [A9]. |
| Practice administrator | A specialisation of authorised clinic staff: professional profiles and schedules [A10]. Administrative authority alone does not grant clinical-record access. |
| Audit reviewer | Explicitly authorised accountability role; reviews protected audit metadata, not clinical content [A10]. Other roles do not inherit this permission. |
| Referring professional, internal or external | Correct referral attribution, specialty/professional target, clinical reason, priority and status. An external professional is a **domain party**, not an implemented system actor/integration. |
| Clinic and treating team | Reliable reservations and arrivals, accurate care documentation, appropriate fee/payment records, accountable access. |
| RAMQ, private insurers, pharmacies, provincial systems | Contextual stakeholders only; no electronic claims, prescription delivery, insurance verification or provincial integration is implemented or promised [S3]. |

## 3. Use-case catalogue and priorities

All catalogue goals have **ATS scope** and **user-goal level**. Internal validation helpers in the diagram are subfunctions, not additional catalogue goals. Criticality reflects the iteration's end-to-end scheduling–arrival–care path, not security importance or eventual delivery priority [D1].

| ID and goal | Primary actor(s) | Priority | Brief normal scenario / outcome |
| --- | --- | --- | --- |
| UC01 Manage Patient Registration | Administrative staff | Supporting | Search for an existing patient before creating/updating demographics, contact and optional insurance details. Assign a unique patient identifier; a missing insurance number must not block registration. Reject unauthorised updates and resolve potential duplicates rather than silently merging [S2, A2]. |
| UC02 Search Patient | Patient; authorised clinic staff | Supporting | Submit insurance number, birth date, phone, partial first/last name, or a combination. Return only authorised, minimal identity matches; disambiguate explicitly. No match grants no permission to use an invented patient ID [S2, D2]. |
| **UC03 Request Appointment** | **Patient; authorised clinic staff** | **Critical** | Identify the patient; find a valid service/professional/location/time; validate any required referral; create a requested appointment and reserve its interval. Fully dressed in §5. |
| UC04 Confirm Appointment | Patient; authorised clinic staff | Supporting | Select an authorised requested appointment; recheck eligibility and its reservation; set `confirmed` without a second booking. Failure leaves it requested. Confirmation is not implicit in requesting [D2–D3]. |
| UC05 Reschedule Appointment | Patient; authorised clinic staff | Supporting | Select an authorised requested/confirmed, not-yet-arrived appointment; choose another valid time and revalidate schedule/referral. Reserve the new interval and release the old one together; preserve status. Failure retains the original booking [D2–D3, A4]. |
| UC06 Cancel Appointment | Patient; authorised clinic staff | Supporting | Select an authorised requested/confirmed, not-yet-arrived appointment; cancel and release its slot. Preserve historical data. Completed care cannot be cancelled as a scheduling action [D2–D3, A4]. |
| **UC07 Register Arrival** | **Administrative staff** | **Critical** | Identify the patient and their confirmed appointment for today's local date; record arrival without changing appointment status. Fully dressed in §6. |
| **UC08 Conduct Consultation and Record Clinical Information** | **Healthcare professional** | **Critical** | Review the authorised clinical record, conduct care, record the consultation, and complete the appointment; optionally prescribe or request follow-up. Fully dressed in §7. |
| UC09 View Clinical Record | Authorised healthcare professional | Supporting | Select a patient in an authorised care context; return their clinical history and audit the sensitive read. Denial discloses no history [S3, D4, D7]. |
| UC10 Issue Prescription | Authorised prescribing healthcare professional | Supporting | From a recorded consultation, provide one or more medication/dosage/instruction items; validate authority and completeness, persist a linked prescription, and audit. No pharmacy transmission [S3, D5, A9]. |
| UC11 Request Follow-up Appointment | Healthcare professional | Supporting | From a consultation, choose service/professional/location/time (optionally after availability search); apply UC03 rules and create a requested appointment linked to that consultation. It is not automatically confirmed [D5]. |
| UC12 Issue Referral | Healthcare professional; authorised administrative staff for external capture only | Supporting | A professional issues a patient referral with author, specialty and/or target professional, clinical reason, priority and status. Alternatively, authorised clinic staff capture an externally issued referral, preserving the external author's identity and attribution. Staff capture is not staff clinical authorship [S3, D6, A8]. |
| UC13 Update Referral Status | Authorised healthcare professional; authorised administrative staff within scope | Supporting | Find an existing referral, select a permitted transition, retain author/target/history, and audit the change. Invalid transitions or unauthorised clinical fields are rejected; precise vocabulary is unresolved [S3, A6, A8]. |
| UC14 Manage Professional Profile | Practice administrator | Supporting | Maintain professional information, kind, specialties, offered services and clinic locations. An external referring professional can have no clinic practice location or schedule [S2–S3, D6, A10]. |
| UC15 Manage Schedule | Practice administrator; explicitly authorized professional/staff | Supporting | Maintain a clinic professional's location/service-specific availability periods. Reject changes that invalidate an existing reservation until affected appointments are explicitly handled [S2, A3, A10]. |
| UC16 Record Fees and Generate Invoice | Authorised administrative staff | Supporting | For uninsured/chargeable services, record assessed fees against appointments and generate a patient invoice grouping uninvoiced charges with priced lines. Service coverage is evaluated separately from the patient's insurance membership; no claims submission [S2–S3, A11]. |
| UC17 Register Payment | Authorised administrative staff | Supporting | Select an invoice, record a valid amount/date/method/reference, and update the outstanding balance. This records payment; it does not process a card or contact an insurer [S3, A11]. |
| UC18 Review Audit Trail | Audit reviewer | Supporting | Select permitted metadata filters and inspect accountability records; deny unauthorised review and audit the sensitive read. Do not expose clinical narrative or permit audit rewriting [S3, D7, A10]. |

### Include, extend, and scope interpretation

- UC03 and UC07 **include UC02** for patient identification in the scenarios below; an already identified patient may supply their existing identity context rather than repeat an unrestricted search.
- UC03 and UC05 include availability search; UC11 conditionally uses it when the professional needs new candidate times. All three include scheduling-constraint validation. These are reusable subfunctions, not separate user goals; a previous availability result never replaces request-time validation.
- **Check required referral extends Validate scheduling constraints** at the referral-validation extension point, **only when the chosen service requires a referral**. A conditional extension is not optional permission to bypass a mandatory rule when its condition holds. Even if no referral was supplied, the required-referral check runs and rejects the request. A voluntarily supplied referral is also checked before it is linked.
- UC08 includes UC09 in this analysed care flow. UC10 and UC11 extend UC08 **after the consultation has been successfully recorded**, if clinically indicated. They also remain separately invocable supporting goals in that consultation's authorised care context.
- UC12/UC13 are separate referral goals; booking neither automatically issues a referral nor automatically changes its status [A6]. Audit applies across goals, rather than being drawn as a mandatory user goal in every association.

## 4. Shared rules, assumptions, and conceptual vocabulary

### Business and integrity rules

| Rule | Requirement and provenance |
| --- | --- |
| BR01 Identity | Every patient has a unique system patient ID. Insurance number is an optional search key, not the mandatory system identity. Supported search keys are insurance number, birth date, phone, and partial first and/or last name [S2]. Staff must resolve ambiguity; the system never silently chooses the first result [A2]. |
| BR02 Actor scope | Every operation uses the authenticated session actor implicitly. Check permissions and ownership/care scope at execution, including reads. A caller-supplied patient ID is not proof of entitlement. Patients see own minimal lookup results; administrative staff see only operational identity/appointment data, not clinical narrative [S3, D2, D7, A1]. |
| BR03 Valid slot | Service, professional and clinic location must exist and form a permitted offering. The professional must be qualified for the service and scheduled at that location. The start must be in the future under A3, and the whole interval `[startTime, startTime + service duration)` must fit a schedule period; it cannot overlap the professional's requested/confirmed reservations at **any** location or the patient's active reservations [S2, D3, A3]. No walk-in exception bypasses this rule. |
| BR04 Reservation | An availability result is advisory, not a hold. Revalidate all constraints when requesting/rescheduling and reserve without a double booking. Requested and confirmed appointments reserve time. Cancellation releases it; completion remains care history, not future availability. Rescheduling preserves the old interval on failure and never creates a `rescheduled` status [D3, A3–A4]. |
| BR05 Referral | A referral-required service needs an existing usable referral belonging to that patient, issued by an appropriately authorised professional. At least one of target specialty/target professional is present; the selected professional/service must satisfy all supplied targets. Validate status and any applicable validity restrictions. A referral can support multiple appointments; booking does not consume it automatically [S2–S3, D6, A6]. |
| BR06 Lifecycle | Only `requested`, `confirmed`, `completed`, `cancelled` are appointment statuses. Normal path: requested → confirmed → completed; requested/confirmed → cancelled where allowed. Arrival is nullable timestamp metadata, not an `arrived` status. Completion is caused by successful consultation recording [D3–D4]. |
| BR07 Arrival | Only an authorised administrative actor may register arrival. The appointment must be confirmed and its start date in the clinic location's timezone must equal today's date there. Record system current time once; a duplicate is rejected without replacing the first arrival time [S3, D4, A5]. No arrival for a requested, cancelled or completed appointment. |
| BR08 Consultation | Only the authorised treating professional may record against an existing confirmed, arrived appointment. At most one consultation is created per appointment in this iteration. Patient identity comes from the appointment, not a replaceable narrative field. Creation, association to the patient's clinical record and transition to completed occur together; failure does none [D4, A7]. |
| BR09 Clinical access | Clinical-record access is role- and care-context-restricted. Search/arrival rights do not imply clinical-reading rights. Clinical reads and writes must not leak content in failures, appointment responses, administrative views or audit metadata [S3, D7, A1]. |
| BR10 Prescribing | A prescription belongs to a recorded consultation; items are nonempty, each containing medication, dosage and instructions. Validate the professional's prescribing scope and patient-care authority. Creating a prescription does not deliver it electronically to a pharmacy [S3, D5, A9]. |
| BR11 Follow-up | Only an authorised healthcare professional requests follow-up in the consultation's patient context. `requestFollowUp` uses BR03–BR05, reserves a slot, creates status requested with no arrival, and links the new appointment to the source consultation. It does not reopen or undo the completed source appointment [D5]. |
| BR12 Coverage and payment | Patient insurance describes a person's optional RAMQ/private memberships. Service coverage describes whether a service is RAMQ-covered, uninsured or conditionally covered. Neither implies that every service for an insured patient is covered. Fees, invoice lines and recorded payments are separate financial concepts; no insurer/gateway calls are assumed [S2–S3, A11]. |
| BR13 Accountability | Audit every successful write, sensitive read and denied action. Record actor, time, action, target identifier/type, outcome and safe reason code, optionally affected patient ID. Do **not** log clinical narrative, medications, search criteria, insurance numbers, returned records or unfiltered request/response payloads. Successful governed operations and their audit evidence must not silently diverge [S3, D7, A12]. |

### Labelled analyst assumptions and unresolved decisions

These complete the partial description sufficiently for consistent scenarios. They are **not established by the source images**. Open questions later reference these IDs. A1–A13 are local identifiers in this file (distinct from the A01-style assumption IDs in `requirements.md`).

| ID | Working assumption | Question to validate |
| --- | --- | --- |
| A1 — session and care authority | Actors have an authenticated account with role assignments; patient ownership and a treating-care relationship further constrain role permissions. No general patient clinical-record access or break-glass workflow is assumed. | How are identity, proxy access, covering professionals and care relationships established and revoked? |
| A2 — identification | Registration creates a clinical record; staff use at least two available identifiers to resolve similar matches, with no automatic duplicate merge. Patient self-search is restricted to the session's linked patient. | Which identifiers are mandatory and how are duplicates, minors and uninsured patients handled? |
| A3 — scheduling | Each service has a positive default duration; the appointment preserves its booked duration. New requests start in the future; same-day future bookings are allowed. A schedule contains dated location/service availability periods; whole-interval professional and patient overlaps are rejected. Touching endpoints do not overlap. Times use the location timezone, with an unambiguous instant/offset. | Are buffers, holidays, capacity, variable durations, travel time, concurrent services, booking windows or patient-overlap exceptions needed? |
| A4 — post-arrival changes | Only requested/confirmed appointments with no arrival may be rescheduled/cancelled. No automatic expiry of requested reservations, no-show status, waitlist or walk-in override is introduced. | Are requested-slot expiry, cancellation deadlines, reversal or no-show workflows required? |
| A5 — arrival policy | No early/late time-of-day cutoff beyond the appointment-local-date check; duplicate arrival returns `alreadyArrived` with the existing time and without a second write. | Are early arrival, lateness, overnight clinics or correction permissions needed? |
| A6 — referral validity | A referral may be linked to many appointments. Booking does not change its status. The working status vocabulary is `active`, `closed`, `cancelled`; only active referrals are usable, and existing reservations must be resolved before closing/cancelling their required referral. Final status/validity/transition policies remain subject to approval. | What statuses permit booking, who may change them, and are expiry or usage limits required? |
| A7 — clinical completion | One consultation per appointment. At least one of observations, diagnoses, treatment recommendations or follow-up requirements is nonblank; not all categories must be present. No post-completion edit/reopen workflow is specified. | What minimum clinical content is mandatory, and how are amendments/cosignatures handled? |
| A8 — external referral capture | Authorised clinic staff may transcribe an external referral's necessary fields with scoped access; the external professional remains the clinical author and the capturing account is audited. Status-only administrative views redact the clinical reason unless specifically permitted. | Which staff can see/transcribe referral clinical reasons, and what authenticity checks are required? |
| A9 — prescribing scope | An authorised prescribing professional can prescribe in their recorded consultation context; being a nurse/other professional does not automatically confer prescribing authority. Dosage and instructions are required text; no drug-interaction checker is promised. | What licence/medication restrictions and signature/printing requirements apply? |
| A10 — administrative roles | Practice administrators or staff with explicit management permissions manage profiles/schedules; professionals with explicit schedule permission may manage their own availability. Designated audit reviewers review audit metadata. Roles can be combined only through explicit authorisation; none grants clinical access automatically. | Who grants/revokes roles, can professionals additionally maintain their own profiles, and who reviews reviewers' access? |
| A11 — financial detail | A fee is an assessed charge against an appointment, with amount/currency; each fee is billed at most once. Authorized staff supply a manually assessed patient/service coverage outcome and administrative basis; only an uninsured outcome permits a fee, retaining that coverage decision as a fee snapshot (O22). No coverage is inferred solely from patient membership. An invoice groups one or more charges for the same patient and its lines preserve charged amounts. Each recorded payment belongs to one invoice and may be partial. No refunds, tax calculation, insurance adjudication or payment processing is assumed. | Are multiple payers, overpayments/refunds, tax rules or insurer allocations required? |
| A12 — audit reliability | Audit records are protected against ordinary-user modification. If required audit evidence cannot be retained, deny a sensitive read before disclosure or leave a write uncommitted; do not return false success. | What retention, clock, availability, redaction and integrity requirements apply? |
| A13 — workload and usability | Booking and arrival are frequent during clinic hours; consultation recording occurs once per completed visit; exact volumes and latency targets are unknown. CLI output uses readable identifiers, precise safe errors and recoverable input prompts. | What concurrency, accessibility, response-time, backup and recovery targets must be accepted? |

### Conceptual-model interpretation

`Patient` is the care recipient; `UserAccount` is a permission-bearing system identity, not a synonym for patient or professional. `HealthcareProfessional` includes internal practitioners and external referrers. A clinic practitioner has at least one practice location; an external referrer may have none. Specialty is a professional qualification/referral target; service is the bookable activity with duration and coverage policy. Schedule periods describe potential availability; requested/confirmed appointments reserve intervals within it. Consultation is recorded care linked to a patient clinical record, not another name for appointment. Referral is a consultation request, distinct from follow-up appointment. Fees are assessed service charges against appointments, invoice lines group those charges for billing, and payments record money received.

## 5. UC03 — Request Appointment (critical, fully dressed)

### Identification and conditions

| Field | Description |
| --- | --- |
| Scope / level | ATS / user goal. |
| Primary actors | Patient requesting for self; authorised clinic staff requesting on behalf of a patient. The SSD depicts the staff variant; patient self-service uses the same operations with own-patient scope. |
| Supporting participants | None implemented. Professionals, clinic and referring professionals are stakeholders, not called external systems. |
| Stakeholders and interests | Patient: correct accessible slot and privacy. Clinic/professional: valid workload and no double booking. Referrer: correctly applied referral. Staff: clear resolution of missing/ambiguous data and no false success. |
| Trigger | The actor decides to request a clinic service for an identified or to-be-identified patient. |
| Preconditions | Actor has an authorised session. A patient registration exists (otherwise UC01 is performed separately). Relevant service, professional, location and schedules are maintained. **A free slot and valid referral are not assumed**; they are checked in the flow. |
| Minimum guarantee | No unauthorised patient data is disclosed. No invalid appointment or partial reservation is retained. A failed request leaves the schedule and existing bookings unchanged; denied actions are audited with safe metadata. |
| Success guarantee | Exactly one appointment exists for the selected patient/service/professional/location/time, status `requested`, null arrival and stored duration. Its interval is reserved; any supplied valid referral is linked. Audit evidence identifies the requesting account. No confirmation, arrival, consultation or invoice is created implicitly. |

### Main success scenario

1. Actor submits supported patient-identification criteria using `searchPatient(criteria)`.
2. ATS applies role/ownership limits, returns minimal permitted identity matches, and the actor selects and verifies exactly one patient (BR01–BR02).
3. Actor supplies service, professional, clinic location and a date range via `searchAvailability(serviceID, professionalID, locationID, dateRange)`.
4. ATS checks the offering, schedule and service duration and returns valid candidate intervals, without reserving them. Actor chooses a start time and supplies a referral identifier if required or applicable.
5. Actor submits `requestAppointment(patientID,serviceID,professionalID,locationID,startTime,referralID?)`.
6. ATS rechecks patient scope, service/professional/location compatibility, the entire interval, overlap constraints and any required/supplied referral. Referral checking is conditional on the service rule, not on whether the actor remembered to enter an identifier.
7. ATS creates the requested appointment, links its patient, service, professional, location and optional referral, reserves its interval and records the successful write together.
8. ATS returns appointment ID, `requested` status and the booked service/location/local date-time/duration. Actor sees that confirmation is still a separate UC04 action.

### Extensions tied to main-flow steps

- **1a / 2a — invalid criteria, denied scope or no patient match (failure):** ATS returns a safe validation/denial/no-match result with no clinical content. The attempt ends without availability search or booking. Actor may start a new attempt with corrected criteria; missing registration goes through UC01 first. A denial is audited.
- **2b — multiple permitted matches (alternate success):** actor refines criteria, repeats steps 1–2 and explicitly chooses the verified patient; continue at step 3. Until resolved, no booking operation is permitted.
- **3a / 4a — invalid offering/date range or no availability (failure or retry):** ATS explains the permissible problem without creating a hold. Actor may revise choices and repeat step 3; abandoning ends with no appointment.
- **4b — service requires referral (alternate success):** actor obtains/selects an already captured referral; step 6 verifies it and continues to step 7. A new referral is issued/captured separately through UC12, never fabricated during booking.
- **6a — referral missing, mismatched, unusable or from an unauthorised issuer (failure):** reject the request; no appointment and no reserved slot. Actor may arrange a valid referral and begin a corrected attempt. Administrative responses do not reveal the clinical reason.
- **6b — slot taken since search or other scheduling constraint fails (failure):** reject without partial reservation; actor may return to step 3. Search results do not guarantee booking.
- **6c — actor no longer authorised or patient data became invalid (failure):** deny and audit; do not disclose extra data or write a booking.
- **7a — persistence or required-audit failure (failure):** no appointment/reservation is committed; report no success. Existing appointments remain unchanged.

**Explicit success example:** staff find patient P1, obtain an available 30-minute clinic service and a valid required referral, and create A1 as requested; A1 reserves the interval but is not yet confirmed.

**Explicit failure example:** the same slot is reserved after availability search but before step 6. The request fails and creates no A1 or second reservation. A no-match patient lookup instead ends before any scheduling operations. Both failure paths appear in the SSD.

### Rules, data variations, frequency and special requirements

- **Rules:** BR01–BR06, BR09 and BR13; assumptions A1–A4, A6, A12–A13.
- **Data variations:** patient lookup by any source-supported key or combination; uninsured patient without insurance number; partial names and ambiguous identities; optional referral for unrestricted services versus mandatory referral for restricted services; clinic services of differing duration; local time/date and daylight-saving ambiguity handled explicitly; patient self-service limited to own identity. A voluntarily supplied referral must still match.
- **Frequency:** frequent during clinic hours; one successful request creates one appointment. Quantitative rates are unknown [A13].
- **Special requirements:** CLI-only eventual interface [S1]; no clinical narrative in search/booking responses; clear local date/time and identifiers; revalidation under concurrent booking; no silent selection among patients; safe recovery after failure; required audit evidence retained [D7, A12–A13].
- **Open questions:** A1 (self/proxy authority), A2 (disambiguation), A3 (buffers/capacity/windows), A4 (request expiry), A6 (referral validity), A13 (performance targets). No unvalidated expiry/overbooking policy is implied.

## 6. UC07 — Register Arrival (critical, fully dressed)

### Identification and conditions

| Field | Description |
| --- | --- |
| Scope / level | ATS / user goal. |
| Primary actor | Authorised administrative staff. |
| Supporting participants | None implemented. The patient presents to staff; there is no kiosk, notification or external check-in service. |
| Stakeholders and interests | Patient: correct and prompt check-in without another patient's information. Treating professional: reliable arrival evidence. Staff/clinic: no duplicate or incorrect arrival; accountable operations. |
| Trigger | Patient presents at the clinic for an appointment and staff initiate check-in. |
| Preconditions | Staff member has a session permitted to identify patients and register arrival. Patient registration exists. **An existing confirmed appointment and correct local date are verified**, not assumed away. |
| Minimum guarantee | No clinical record is exposed. Failed lookup/state/date/authority checks do not alter any arrival or appointment. Existing arrival is not overwritten. Denied actions are audited safely. |
| Success guarantee | Selected appointment has one persisted current arrival timestamp and an audit record of the registering actor. Status remains `confirmed`; patient, professional, start time and referral are unchanged. No consultation or invoice is created. |

### Main success scenario

1. Staff submits patient criteria using `searchPatient(criteria)`.
2. ATS returns minimal authorised matches; staff verify and select the correct patient.
3. Staff submits `findAppointment(patientID,date)` for the intended clinic-local date.
4. ATS returns only permitted appointment summaries (ID, service, professional, location, local start, status and arrival indication). Staff selects the intended appointment and verifies its location/date; the date lookup alone does not establish arrival eligibility.
5. Staff invokes `registerArrival(appointmentID)`.
6. ATS revalidates staff authority, appointment existence, `confirmed` status, absent arrival, and that the appointment's local start date equals the current date at its clinic location.
7. ATS records current arrival time and successful-write audit evidence together, keeping status `confirmed`.
8. ATS returns appointment ID, registered arrival time and unchanged status. Staff can direct the patient to await the professional.

### Extensions tied to main-flow steps

- **1a / 2a — invalid/denied patient search or no patient found (failure):** return a safe result and stop. No appointment lookup or arrival operation follows in this attempt; resolve registration/criteria separately.
- **2b — ambiguous patient (alternate success):** refine identification and repeat steps 1–2; only after verification proceed to step 3.
- **3a / 4a — no appointment or denied lookup (failure):** return no permitted match and stop before `registerArrival`. New booking, if appropriate, is UC03 plus separate confirmation UC04, not an implicit check-in side effect.
- **4b — multiple same-day appointments (alternate success):** staff distinguishes service/professional/location/start time, chooses the correct appointment and proceeds to step 5; never register all matches automatically.
- **6a — requested/cancelled/completed appointment, wrong local date, or revoked authority (failure):** reject and audit the denied action; leave status/arrival unchanged. A requested appointment must first be confirmed under UC04; no automatic confirmation occurs.
- **6b — arrival already registered, including a concurrent duplicate (failure of the duplicate command):** return `alreadyArrived` and the original arrival time; do not overwrite it or create another successful-write event. The original confirmed status remains. Treat any disclosed sensitive metadata according to audit policy.
- **7a — persistence or audit failure (failure):** retain no new arrival timestamp, report failure, and preserve the pre-existing appointment.

**Explicit success example:** A1 is confirmed for today at location L1; staff identify P1, find A1 and register an arrival timestamp while A1 stays confirmed.

**Explicit failure example:** the appointment lookup returns no appointment for P1 on the requested date. The attempt ends; `registerArrival` is never called. A confirmed appointment for tomorrow also fails the step-6 local-date check without alteration.

### Rules, data variations, frequency and special requirements

- **Rules:** BR01–BR02, BR06–BR07, BR09 and BR13; A1–A2, A5, A12–A13.
- **Data variations:** all patient search keys; zero/one/multiple same-date appointments; a patient's appointments in different location timezones; a duplicate arrival; a nonconfirmed or cancelled booking. A date search matches each appointment's own location-local date, not the operator computer's date.
- **Frequency:** once per attending appointment, concentrated near clinic appointment times; duplicate attempts may occur [A13].
- **Special requirements:** minimal administrative display, safe no-match/denial messages, local-date correctness, concurrent duplicate protection and clear distinction between arrival timestamp and appointment status. No clinical content is needed for check-in.
- **Open questions:** A2 (identity checks), A5 (early/late/overnight policy and correction), A13 (peak check-in performance). Clinical care without a confirmed arrival is not silently enabled.

## 7. UC08 — Conduct Consultation and Record Clinical Information (critical, fully dressed)

### Identification and conditions

| Field | Description |
| --- | --- |
| Scope / level | ATS / user goal. The physical consultation includes work outside the software boundary. |
| Primary actor | The appointment's authorised treating healthcare professional. |
| Supporting participants | Patient participates in care, but only the professional sends the SSD's system events. No pharmacy, insurer, laboratory or external clinical system is called. |
| Stakeholders and interests | Patient: confidential and correct care history, medication and follow-up instructions. Professional: sufficient authorised context and durable documentation. Clinic: only eligible arrived appointments completed, no duplicate consultations and accountable access. |
| Trigger | Treating professional is ready to consult an arrived patient and document the encounter. |
| Preconditions | Professional has an authenticated clinical session and a selected appointment/patient context from the clinic workflow. For success, the appointment is confirmed and arrived and the actor is its authorised treating professional; ATS rechecks this at recording time. A patient clinical record exists [A2]. Failed access or stale state is handled below rather than trusted from an earlier display. |
| Minimum guarantee | Unauthorised access reveals no clinical content. Failed recording creates no consultation or clinical-record addition and does not complete the appointment. Failed optional prescription/follow-up creates no partial item list or reservation and does not undo an already recorded consultation. Denials are audited without narrative. |
| Success guarantee | One consultation records supplied observations, diagnoses, treatment recommendations and follow-up requirements as applicable. It links the appointment, its patient clinical record and treating professional; appointment status becomes `completed` with arrival preserved. Sensitive record read and successful write are audited. Any successfully chosen optional prescription/follow-up is linked to this consultation and separately audited. |

### Main success scenario

1. In the selected appointment's patient context, professional invokes `viewClinicalRecord(patientID)`.
2. ATS validates the session's clinical role and patient-care access, records audit evidence for the sensitive read, and returns the authorised clinical record.
3. Professional conducts the consultation with the patient outside the system, using appropriate history, and prepares observations, diagnoses, treatment recommendations and follow-up requirements as applicable.
4. Professional submits `recordConsultation(appointmentID,observations,diagnoses,treatmentRecommendations,followUpRequirements)`.
5. ATS rechecks the professional's treating authority, the appointment's confirmed status and recorded arrival, record consistency, absence of an existing consultation and clinical-data validity.
6. ATS creates the consultation, associates it to the appointment, treating professional and patient's clinical record, marks the appointment `completed` and retains successful-write audit evidence together. It preserves the arrival timestamp.
7. ATS returns consultation ID and completed appointment status. The core use case has succeeded. Professional may choose the step-7 extensions for a prescription and/or a follow-up; neither is mandatory and neither is an electronic transmission.

### Extensions tied to main-flow steps

- **1a / 2a — record missing or clinical access denied (failure):** return a safe failure with no record content and stop this attempt before `recordConsultation`. Resolve the clinical context or registration separately; audit denial. A successful prior patient search would not override this check.
- **4a / 5a — missing/invalid clinical data (failure or correction):** reject without creating a consultation or changing status; professional corrects inputs and repeats step 4. Under A7 at least one clinical category must be nonblank; all four need not be mandatory.
- **5b — not treating/authorised professional, no arrival, nonconfirmed state, patient/record inconsistency or existing consultation (failure):** reject safely and audit denial. No consultation is added and no appointment is completed. The professional must not bypass the check by supplying another appointment ID.
- **6a — storage or required-audit failure (failure):** no consultation, record association or completion is retained. Report failure, not a fabricated consultation ID.
- **7a — prescription indicated (optional success):** professional submits `issuePrescription(consultationID,items)`. ATS checks care/prescribing authority, existing consultation and a nonempty set of complete medication/dosage/instruction items. ATS creates and links the prescription and items, audits, and returns its ID. Core consultation remains completed.
- **7a.1 — prescribing denied, empty/incomplete items, unknown medication or audit/write failure (optional failure):** create no prescription or partial items; return safe errors. Professional may correct and retry this supporting goal. The already committed consultation remains completed; no pharmacy call occurs.
- **7b — follow-up indicated (optional success):** professional chooses service/professional/location and, if needed, calls `searchAvailability(serviceID, professionalID, locationID, dateRange)`. After choosing a time and any required referral, professional submits `requestFollowUp(consultationID,serviceID,professionalID,locationID,startTime,referralID?)`. ATS derives the patient from the consultation and reapplies UC03's actor, offering, full-interval, overlap and referral rules. ATS creates and reserves a **requested** follow-up appointment linked to this consultation, audits it and returns its ID. Confirmation remains UC04.
- **7b.1 — availability search returns invalid/no options (optional failure):** do not invoke `requestFollowUp` in this attempt; revise choices in a new attempt or leave the follow-up requirement documented but unscheduled.
- **7b.2 — authority, scheduling or referral validation fails at follow-up request, or audit/write fails (optional failure):** create no follow-up appointment or reservation. Original consultation and completed appointment remain intact; professional may retry with corrected inputs. A previously displayed free slot is not a guarantee.
- **7c — both prescription and follow-up indicated (alternate success):** perform 7a and 7b independently after step 7. Either may succeed even if the other fails; report their outcomes separately without implying full rollback of care.

**Explicit success example:** professional H1 reads P1's authorised record, records a consultation for arrived confirmed appointment A1, and receives C1 with A1 completed. H1 may then obtain prescription RX1 and requested follow-up A2 linked to C1.

**Explicit failure example:** a professional outside P1's permitted care context attempts step 1. ATS denies the read, audits the denial without clinical content, and the attempt ends before recording. Alternatively, if A1 loses eligibility before step 5, recording fails without a consultation or completion.

### Rules, data variations, frequency and special requirements

- **Rules:** BR02–BR06, BR08–BR13; A1–A3, A6–A7, A9, A12–A13.
- **Data variations:** any clinically appropriate combination of the four narrative categories; explicit absence versus missing mandatory content; zero or multiple prescription items but at least one for an issued prescription; multiple medications; no/one/both optional actions; follow-up with another qualified professional or clinic location; required/optional referral; empty follow-up availability results. A follow-up patient ID cannot be supplied to redirect care.
- **Frequency:** ordinarily once per completed appointment; optional prescriptions and follow-up requests depend on clinical need. Volumes are not specified [A13].
- **Special requirements:** least-privilege sensitive reads; treatment-context authorisation rechecked on write; no narrative in audit/errors; consultation/record/completion consistency; clear distinction between core success and optional-action failure; CLI input must preserve entered clinical text on recoverable validation errors without writing it to logs [A12–A13].
- **Open questions:** A1 (covering practitioners), A7 (minimum content, amendments/cosignatures), A9 (prescribing authority), A6 (referral validity for follow-ups), A12 (audit retention/reliability). No laboratory ordering, diagnosis coding system, pharmacy delivery or automatic referral generation is assumed.

## 8. System operations and operation contracts

The single authoritative contract set is [operation-contracts.md](operation-contracts.md). O01–O09 define the exact events used in these three SSDs; O10–O25 cover all supporting goals. Session actor, role/care scope, validation, atomicity and audit rules are implicit in every event. The BR rules above refine those contracts under the explicitly provisional analysis assumptions.

| Critical use case | Contract events |
|---|---|
| UC03 | O01 searchPatient; O02 searchAvailability; O03 requestAppointment |
| UC07 | O01 searchPatient; O04 findAppointment; O05 registerArrival |
| UC08 | O06 viewClinicalRecord; O07 recordConsultation; optional O08 issuePrescription and O02/O09 follow-up |

## 9. Consistency checks and acceptance examples

| Check | Required outcome |
| --- | --- |
| Search by partial name finds two patients | No silent first-match selection; refine identity before any booking or arrival. |
| Patient tries another patient's ID | Deny scoped read/write without identity/clinical disclosure; audit denial. |
| Availability appeared free but another request won | Revalidate; at most one overlapping requested/confirmed reservation is accepted. |
| Service requires referral but `referralID?` is absent | Conditional referral check executes and rejects; no reservation. |
| Referral targets neither specialty nor professional | Reject referral/booking as invalid; at least one target is required. |
| External referring professional has no clinic location | Retain valid external author identity; no fabricated clinic schedule/account/integration is necessary. |
| Uninsured patient has no insurance number | Registration/lookup by other supported keys and appointment requests remain possible. |
| RAMQ-insured patient requests an uninsured service | Do not infer service coverage from patient insurance; fees/invoice/payment may apply. |
| Requested appointment presented for arrival | Reject arrival until separately confirmed. |
| Operator computer and clinic location have different dates | Use appointment location's local date for arrival eligibility. |
| Two staff register arrival concurrently | Preserve one original timestamp, with no duplicate successful write. |
| Clinical read denied or patient/appointment lookup empty | SSD branch terminates; no dependent write may follow. |
| Confirmed appointment has no arrival / wrong professional records | Reject consultation without completion or clinical-record mutation. |
| Consultation recorded successfully | One linked consultation, appointment completed, original arrival preserved; no implicit invoice/prescription/follow-up. |
| Prescription has no items or an item lacks dosage/instructions | Reject whole prescription; keep recorded consultation completed. |
| Follow-up slot/referral invalid | No follow-up reservation; source care remains recorded. |
| Follow-up succeeds | New requested appointment linked to consultation, reserving its slot; separate confirmation needed. |
| Audit examined | Actor/action/target/time/outcome are present; no clinical text, medication details, raw search criteria or insurance numbers. |

**Iteration boundary:** analysis documents, editable diagram sources and rendered PNG diagrams are delivered. No application, implementation classes or external integrations are implemented. The source does not settle A1–A13; these remain explicit validation work rather than hidden promises.
