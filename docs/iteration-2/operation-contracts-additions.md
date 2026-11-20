# Iteration 2 — Changes to the existing operation contracts

These changes supplement [the original contracts](../../operation-contracts-lean.md). They describe system behaviour, not additional PoC features. Existing authorisation, audit, atomicity and clinic-time rules remain the baseline. The assignment permits postponing the detailed design of the new waiting-list/resource requirements to Iteration III.

## Existing operations

### O02 — searchAvailability

**Unified analysis signature:** `searchAvailability(serviceID, professionalID?, locationID?, dateRange, specialtyID?)`.

A supplied professional restricts the search to that professional; otherwise search professionals matching the optional specialty (or all professionals if omitted). Supplied professional, location, date and specialty filters must all match. Return the matching professional, location and interval. Booking and follow-up supply a professional and may omit specialty; waiting-list searches may omit professional. The Java PoC retains its documented professional-specific subset.

Candidate appointments must fit explicit professional availability and satisfy service duration, professional conflicts and required resource availability. Availability may be generated from a recurring `Schedule` or imported as one-off `AvailabilitySlot`s. Searching creates no reservation. Patient/referral checks remain booking-time checks as in the original contract.

### O03 — requestAppointment and O09 — requestFollowUp

Keep the existing appointment state and referral rules. Add validation of the predefined resources required by the selected `MedicalService` for the complete appointment interval. Each specific room/equipment item has capacity one. An assisting role is a predefined pool with a positive capacity (for example, two nurse-role units), never named employees.

Each `ResourceRequirement` specifies one resource and a positive `quantityRequired`. Success creates one `ResourceAllocation` per required resource with `quantityAllocated = quantityRequired`. At every instant of the appointment interval, allocated quantities across active appointments must not exceed that resource's capacity. A service needing two specific rooms lists two requirements of one unit each; a service needing two nurses lists one requirement of two units against the nurse-role pool. Unavailable/maintenance resources cannot be allocated. These are analysis concepts only; resource implementation remains deferred.

Success associates the appointment with the selected availability and resources. If a required resource is unavailable, reject with `resourceUnavailable`; no partial appointment or allocation is created. Revalidate at booking time; an earlier availability search is not a reservation.

**O03 analysis signature:** `requestAppointment(patientID, serviceID, professionalID, locationID, dateTime, referralID?, waitingListEntryID?)`. O09 is unchanged apart from resource validation.

For a waiting patient's acceptance, staff supplies the selected `waitingListEntryID`. Validate that the entry exists, is still eligible (not accepted/removed), belongs to the supplied patient, and matches the service, location, professional/specialty/date preferences and applicable referral. An unknown entry fails with `notFound`; an incompatible or no-longer-eligible entry fails with `invalidInput`. Atomically create the appointment and allocations, form the `Fills-from` association with **that exact entry**, and mark it accepted; other matching entries remain unchanged. A rejection changes neither the entry nor the booking state. Omit the parameter for ordinary booking; never infer an entry from patient identity alone. Booking still returns `status=requested`; confirmation remains separate. The extra parameter is analysis-only, not a new Java PoC feature.

### O05 — registerArrival

No business-rule change. Preserve the original failure order: `notFound`, `accessDenied`, `invalidState`, `wrongDate`, `alreadyArrived`, followed by audit/commit failures. Arrival records a timestamp and staff association; appointment status stays `confirmed`.

## New system events

### joinWaitingList

`joinWaitingList(patientID, serviceID, locationID, professionalID?, specialtyID?, dateRange?, referralID?)`

The patient or staff requests placement when no matching appointment is available. Validate patient and preferences; at least one professional, specialty or date preference is supplied. Create a `WaitingListEntry` associated with the patient, service, location and supplied criteria. Return entry ID, priority and waiting status. Do not create an appointment or resource reservation.

### changeWaitingListPriority

`changeWaitingListPriority(entryID, priorityRank)`

Administrative staff changes an existing entry's priority. Return the updated entry and priority. The assignment does not prescribe a tie-breaking policy.

### removeWaitingListEntry

`removeWaitingListEntry(entryID)`

Administrative staff removes the patient from the waiting list, including when staff chooses removal after a declined phone offer. Return the entry ID and removed status. Decline alone does not remove the entry or make the patient permanently ineligible for later offers.

### cancelAppointment

`cancelAppointment(appointmentID)`

Validate the appointment and actor according to the existing appointment lifecycle. Set the cancelled status and free its professional capacity and allocated resources together. The system identifies waiting patients whose professional/specialty/date preferences and booking constraints match the available appointment. Return the cancellation result and present eligible entries to administrative staff in priority order; an empty list is a normal outcome.

The returned candidates include `entryID`, patient identity and criteria. Staff offers by phone outside the software, selecting the highest-priority eligible entry not yet contacted in this offer round; staff may choose among equal-priority entries. If staff continues after a decline, proceed to the next entry in priority order until booking succeeds, candidates are exhausted, or the slot is no longer available. A declined entry not removed remains eligible for future rounds, not retried repeatedly in the same round.

If accepted, staff invokes normal booking with the exact `waitingListEntryID`. If declined, staff may remove the patient; otherwise the patient remains on the waiting list. Do not introduce a separate offer object, capacity hold, withdrawal operation, expiry timer or mandatory exclusion after decline.

### makeAvailability

`makeAvailability(professionalID, locationID, dateRange, recurrence, startTime, endTime, slotMinutes)`

Administrative staff supplies a selected professional, inclusive date range, weekly weekday pattern or monthly day-of-month, and start/end times. Validate the range and positive slot duration. Generate the schedule and explicit slots together; reject overlapping professional availability with `duplicateAvailability` without partial creation.

The PoC uses whole fixed-duration slots, requires the daily interval to divide exactly by slot length, and skips monthly dates absent from a month. These are documented representation choices, not extra user workflows. Return the created slots (the conceptual SSD also identifies the schedule).

### importAvailability / exportAvailability

`importAvailability(sourcePath)` parses the supplied seven-column UTF-8 format:

```csv
First Name,Last Name,Title,Specialty,Date,Start,End
```

Validate all rows before adding professionals and explicit availability slots. Reuse matching professional identities; reject duplicate/overlapping intervals without partial changes. Import does not reset existing bookings. Return import counts or an input/file error.

`exportAvailability(destinationPath)` writes the entire availability dataset, including past and booked slot definitions, in that format. It does not change domain state. It is not a backup of patient or appointment records. The PoC returns `void` and displays completion rather than returning the conceptual SSD's count.

The screenshots also show example booked-appointment and waiting-patient datasets. The explicitly required PoC import/export operations concern availability; those other importers are not implemented.

## Scope mapping

- **Implemented PoC:** booking at clinic/by phone, searching availability, recurring availability creation, availability CSV import/export, and supporting patient/professional/service registration. Existing referral validation is retained from the baseline booking contract.
- **Analysis only:** waiting lists and resource allocation, as permitted for the new iteration requirements. Their concepts remain in the updated use-case/domain diagrams and SSDs; no speculative design classes are added.
- **Designed, not implemented:** the original arrival and consultation workflows, to provide the required interaction diagrams and corresponding partial class model.
- **Not added:** database persistence, web/GUI interfaces, automated phone integration, offer reservations or additional business workflows.
