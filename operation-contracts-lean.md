# Operation Contracts - Appointment Tracking System

Contracts for the nine system operations used in SSD1 (Request Appointment), SSD2 (Register Arrival) and SSD3 (Conduct Consultation and Record Clinical Information). Postconditions describe changes to the domain model (instances created, associations formed, attributes set). Class, attribute and association names come from the domain model; operation and parameter names come from the SSDs.

## 1. Common rules

Every operation is invoked by an authenticated actor; the session identity is implicit. The following rules apply to all contracts.

| ID | Rule |
|---|---|
| **CR1 Audit** | Every state-changing operation, every patient-linked read (O01, O04, O06) and every denied attempt creates an *AuditRecord* *Performed-by* the acting *SystemUser*, which *Concerns* the affected *Patient* when one is identified. It never holds clinical content, search criteria or medication details. If it cannot be retained, the operation fails with `auditUnavailable`. Availability searches (O02) are not audited. |
| **CR2 Atomicity** | A rejected operation creates no instance, forms no association and changes no attribute (except the AuditRecord of the denied attempt). All postconditions of a success hold together or not at all. |
| **CR3 Authorisation** | Role and scope are checked on every call. A Patient acts only on their own record. AdministrativeStaff receive no clinical content. A HealthcareProfessional receives clinical content only for patients in their care. |
| **CR4 Clinic time** | "Today" and "current time" are evaluated in clinic-local time at the appointment's ClinicLocation. |

**Booking validation (BV)**, shared by O02 (BV2 to BV4, professional only), O03 and O09. The booked interval is `[dateTime, dateTime + MedicalService.durationMinutes)`.

| ID | Rule |
|---|---|
| **BV1 Existence** | The Patient, MedicalService, ClinicProfessional and ClinicLocation exist, and the professional *Practises-at* that location. |
| **BV2 Future** | `dateTime` is later than the current time. |
| **BV3 Schedule** | The whole interval lies within a *Schedule* the professional *Follows*, which *Applies-to* the location, matches the `dayOfWeek` and whose `effectiveFrom` / `effectiveTo` contain the date. |
| **BV4 No overlap** | No `requested` or `confirmed` Appointment overlaps the interval for the same professional (at any location) or the same patient. Touching end points do not overlap; cancelled appointments do not count. |
| **BV5 Referral** | If the service `requiresReferral`, `referralID` must be supplied. Any supplied referral must be *Subject-of* this patient, be active, and have targets satisfied by the professional (target professional equals `professionalID`, and/or target specialty is one of the professional's specialties; if both, both must hold). |
| **BV6 Concurrency** | Two simultaneous requests for overlapping intervals cannot both succeed. |

**Failure codes and SSD labels.** The SSDs show failures as labels; this table maps each code to them. Where no label is given, the failure is not drawn on any SSD.

| Code | Meaning | SSD label |
|---|---|---|
| (empty result) | Search finds nothing (normal outcome) | "No patient found", "No appointment found", "No availability" |
| `invalidInput` | Missing or malformed parameter | |
| `notFound` | Referenced identifier does not exist | O07: "Recording rejected" |
| `accessDenied` | Role or care scope does not permit the call | O07: "Recording rejected"; O08: "Prescription rejected"; O09: "Follow-up rejected" |
| `invalidState` | State does not permit the call | O05: "Appointment not confirmed"; O07: "Recording rejected" |
| `slotUnavailable` | BV1 (offering), BV2, BV3, BV4 or BV6 fails | O03: "Slot unavailable"; O09: "Follow-up rejected" |
| `referralRequired` | Required referral not supplied (BV5) | O03: "Referral required"; O09: "Follow-up rejected" |
| `referralInvalid` | Supplied referral unusable or mismatched (BV5) | O03: "Referral not valid"; O09: "Follow-up rejected" |
| `wrongDate` | Appointment's clinic-local date is not today | O05: "Appointment is not today" |
| `alreadyArrived` | `arrivalTime` already recorded | O05: "Arrival already registered" |
| `auditUnavailable` | Required AuditRecord could not be retained | |
| `systemError` | Persistence failure; nothing committed | O07: "Recording failed" |

## 2. Contracts

### O01 - searchPatient

- **Signature:** `searchPatient(criteria)`, where `criteria` is at least one of `insuranceNumber`, `dateOfBirth`, `phoneNumber`, `firstName` (partial allowed), `lastName` (partial allowed); supplied fields must all match.
- **Returns:** matching patients (`patientID`, `name`, `dateOfBirth`).
- **Use case / SSD:** UC02 Search Patient (included by UC03, UC07) / SSD1, SSD2.

**Preconditions**
1. The actor is authenticated and authorised to search patients (a Patient may search only their own record).
2. `criteria` contains at least one supported field.

**Postconditions**
1. No Patient, Appointment, Referral or ClinicalRecord attribute or association is changed.
2. The result contains every Patient matching all criteria, limited to `patientID`, `name` and `dateOfBirth`. No clinical content is returned.
3. When nothing matches, the result is empty. The system never selects among several matches; the actor chooses one `patientID`.

**Fails with:** `invalidInput`, `accessDenied`, `auditUnavailable`, or an empty result.

### O02 - searchAvailability

- **Signature:** `searchAvailability(serviceID, professionalID, locationID?, dateRange)`. `locationID` is optional; when omitted, all locations where the professional practises are searched.
- **Returns:** available slots (`dateTime`, `locationID`).
- **Use case / SSD:** UC03 Request Appointment (includes Search Availability), also used by UC05 and, through UC08, by UC11 / SSD1 (shown without `locationID`), SSD3.

**Preconditions**
1. The actor is authenticated and may request scheduling information.
2. The MedicalService and the ClinicProfessional exist; if supplied, the ClinicLocation exists and the professional *Practises-at* it.
3. `dateRange` is valid (start not after end).

**Postconditions**
1. No domain state is changed and no AuditRecord is created.
2. The result lists each candidate `dateTime` and `locationID` for which BV2, BV3 and BV4 (for the professional) hold over the service's full duration. It is empty if there are none.
3. The result is advisory: nothing is reserved, and a listed slot may be taken before it is requested (BV6). Patient-specific overlap and referral rules are not evaluated here.

**Fails with:** `invalidInput`, `notFound`, `accessDenied`, or an empty result.

### O03 - requestAppointment

- **Signature:** `requestAppointment(patientID, serviceID, professionalID, locationID, dateTime, referralID?)`.
- **Returns:** `appointmentID`, `status = requested`.
- **Use case / SSD:** UC03 Request Appointment (includes Search Patient, Search Availability and Validate Scheduling Constraints; extended by Validate Referral when the service requires a referral) / SSD1.

**Preconditions**
1. The actor is authenticated. AdministrativeStaff may request for any registered patient; a Patient only for their own `patientID`.
2. BV1 to BV6 hold. An earlier availability result does not substitute for this check.
3. BV5 holds, including when `referralID` is omitted for a service that requires one.

**Postconditions**
1. An *Appointment* `a` was created with a unique `appointmentID`.
2. `a.dateTime` was set to `dateTime`, `a.status` to `requested`, and `a.requestedBy` to *staff* or *patient* according to the actor. `a.arrivalTime` is absent.
3. `a` was associated with the Patient (*Books*), the ClinicProfessional (*Conducts*), the ClinicLocation (*Hosts*) and the MedicalService (*Service-of*).
4. If `referralID` was supplied, `a` was associated with that Referral (*Leads-to*). The Referral itself is unchanged and stays usable for other appointments.
5. The interval is reserved: it counts toward BV4 for the professional and the patient.
6. No confirmation, arrival, consultation, fee or invoice was created.

**Fails with:** `slotUnavailable`, `referralRequired`, `referralInvalid`, `notFound`, `invalidInput`, `accessDenied`, `auditUnavailable`, `systemError`.

### O04 - searchAppointment

- **Signature:** `searchAppointment(patientID, date)`, where `date` is a clinic-local calendar date.
- **Returns:** appointments (`appointmentID`, `service`, `professional`, `location`, `dateTime`, `status`, `arrivalTime`).
- **Use case / SSD:** UC07 Register Arrival / SSD2.

**Preconditions**
1. The actor is authenticated AdministrativeStaff (or another actor authorised for scheduling information).
2. The Patient exists and was identified through O01. `date` is valid.

**Postconditions**
1. No Appointment is created or modified.
2. The result lists every *Appointment* the patient *Books* whose `dateTime` falls on `date` at its ClinicLocation, with the fields above and no clinical content. All statuses are returned; eligibility for arrival is decided by O05. The result is empty when there is none.
3. When several appointments are returned, none is selected on the actor's behalf.

**Fails with:** `invalidInput`, `notFound`, `accessDenied`, `auditUnavailable`, or an empty result.

### O05 - registerArrival

- **Signature:** `registerArrival(appointmentID)`.
- **Returns:** `appointmentID`, `arrivalTime`, `status = confirmed`.
- **Use case / SSD:** UC07 Register Arrival (includes Search Patient) / SSD2.

**Preconditions**
1. The actor is authenticated AdministrativeStaff authorised to register arrivals.
2. The Appointment exists and `a.status = confirmed`.
3. The clinic-local date of `a.dateTime` is today at `a`'s ClinicLocation (CR4).
4. `a.arrivalTime` is absent.

**Postconditions**
1. `a.arrivalTime` was set to the current date and time.
2. `a.status` is unchanged (`confirmed`). Arrival is a timestamp, not a status.
3. `a` was associated with the AdministrativeStaff who registered it (*Registers-arrival-of*).
4. No Consultation was created and no other attribute or association of `a` changed.

**Fails with** (checked in this order): `notFound`, `accessDenied`, `invalidState`, `wrongDate`, `alreadyArrived` (the original `arrivalTime` is kept and returned), `auditUnavailable`, `systemError`.

### O06 - viewClinicalRecord

- **Signature:** `viewClinicalRecord(patientID)`.
- **Returns:** the patient's clinical record: its consultations, their prescriptions and related referrals.
- **Use case / SSD:** UC09 View Patient Clinical Record / not shown in any SSD.

**Preconditions**
1. The actor is an authenticated HealthcareProfessional authorised for this patient's care. An administrative role alone is not sufficient.
2. The Patient exists and has a *ClinicalRecord* (*Has-record*).

**Postconditions**
1. No clinical object is created, modified or deleted.
2. The result comprises the ClinicalRecord, the Consultations it *Contains*, the Prescriptions each *Yields*, and the Referrals the patient is *Subject-of*.
3. The AuditRecord for the read is created before anything is disclosed (CR1).

**Fails with** (no clinical content is disclosed): `accessDenied`, `notFound`, `auditUnavailable`.

### O07 - recordConsultation

- **Signature:** `recordConsultation(appointmentID, observations, diagnoses, treatmentRecommendations, followUpRequirements)`. Each clinical field may be empty, but at least one must be non-blank.
- **Returns:** `consultationID`, `appointmentStatus = completed`.
- **Use case / SSD:** UC08 Conduct Consultation and Record Clinical Information / SSD3.

**Preconditions**
1. The actor is an authenticated HealthcareProfessional authorised for the patient's care and is the ClinicProfessional associated with the Appointment (*Conducts*).
2. The Appointment exists, `a.status = confirmed` and `a.arrivalTime` is present.
3. No Consultation exists yet for the Appointment (*Results-in* is 0..1) and the patient's *ClinicalRecord* exists.
4. At least one clinical field is non-blank and all supplied values are valid.

**Postconditions**
1. A *Consultation* `c` was created with a unique `consultationID`, and its four clinical attributes were set from the parameters.
2. `c` was associated with the Appointment (*Results-in*) and with the patient's ClinicalRecord (*Contains*).
3. `a.status` was set to `completed`. `a.arrivalTime` and all other associations of `a` are unchanged.
4. No Prescription or follow-up Appointment was created; those are O08 and O09.

**Fails with** (no Consultation is created and the Appointment is not completed): `accessDenied`, `invalidState`, `invalidInput`, `notFound`, `auditUnavailable`, `systemError`.

### O08 - issuePrescription

- **Signature:** `issuePrescription(consultationID, items)`, where each item has a `medicationID`, `dosage` and `instructions`.
- **Returns:** `prescriptionID`.
- **Use case / SSD:** UC10 Issue Prescription (extends UC08 when medication is prescribed) / SSD3, frame `[consultation recorded AND medication prescribed]`.

**Preconditions**
1. The actor is an authenticated ClinicProfessional with prescribing authority and care scope for this Consultation's patient. A clinical role alone is not enough.
2. The Consultation exists.
3. `items` has at least one entry; each names an existing *Medication* and has non-blank `dosage` and `instructions`.

**Postconditions**
1. A *Prescription* `p` was created with a unique `prescriptionID` and `issueDate` set to the current date.
2. `p` was associated with the Consultation (*Yields*) and the issuing ClinicProfessional (*Prescribes*).
3. For each entry, a *PrescriptionItem* was created with its `dosage` and `instructions`, associated with `p` (*Lists*, composition) and with its Medication (*Refers-to*).
4. The Consultation and its Appointment are unchanged. Nothing is transmitted to a pharmacy.

**Fails with** (no Prescription and no partial item list is saved; the consultation stays recorded): `accessDenied`, `invalidInput`, `notFound`, `auditUnavailable`, `systemError`.

### O09 - requestFollowUp

- **Signature:** `requestFollowUp(consultationID, serviceID, professionalID, locationID, dateTime, referralID?)`. The patient is not a parameter.
- **Returns:** `appointmentID`, `status = requested`.
- **Use case / SSD:** UC11 Request Follow-up Appointment (extends UC08 when a follow-up is required; includes Search Availability and Validate Scheduling Constraints) / SSD3, frame `[consultation recorded AND follow-up required]`.

**Preconditions**
1. The actor is an authenticated ClinicProfessional with access to the originating Consultation.
2. The Consultation exists. The patient is derived from its ClinicalRecord (*Has-record*), never from the caller.
3. BV1 to BV6 hold for that patient. The earlier O02 result is not a hold.

**Postconditions**
1. A new *Appointment* `f` was created exactly as in O03: unique `appointmentID`, `dateTime`, `status = requested`, no `arrivalTime`, associated with the Patient (*Books*), ClinicProfessional (*Conducts*), ClinicLocation (*Hosts*), MedicalService (*Service-of*) and, if supplied, the Referral (*Leads-to*).
2. `f` was associated with the originating Consultation (*Followed-up-by*), and `f.requestedBy` was set to *staff*.
3. The originating Consultation and its `completed` Appointment are unchanged, as is any earlier Prescription.
4. `f` is not confirmed; confirmation is a separate operation.

**Fails with** (no follow-up is created; the recorded consultation is never rolled back): `accessDenied`, `notFound`, `invalidInput`, `slotUnavailable`, `referralRequired`, `referralInvalid`, `auditUnavailable`, `systemError`.

## 3. Assumptions

1. The booked interval is derived from `MedicalService.durationMinutes`; the model stores no duration or end time on Appointment.
2. `ClinicLocation` has no time-zone attribute, so clinic-local time is assumed to be one zone.
3. The Referral status vocabulary is not defined; "usable" means active.
4. `requestedBy` has only *patient* and *staff*, so a follow-up requested by a professional is recorded as *staff*.
