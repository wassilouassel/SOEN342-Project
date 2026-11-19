# Appointment tracking — Java CLI proof of concept

Dependency-free, object-oriented **Java 17+**, terminal only. The question this PoC exercises is whether explicit availability, recurrence, CSV interchange, and atomic appointment requests collaborate correctly. It is not the complete appointment tracking application.

## Run and test

From the repository root, with a JDK (not only a JRE) on `PATH`:

```sh
./prototype/run.sh
./prototype/test.sh
```

Both scripts use `javac --release 17 -encoding UTF-8`, compile into a temporary directory, and clean it up. No Maven, Gradle, database, Python, downloads, or external libraries are required. EOF at the menu **or midway through a command** exits normally; a partial command does not commit. Type `0` to exit. Every new process starts with **zero patients, professionals, services, slots, referrals, schedules, and appointments**. IDs are local to the process and printed after registration.

All state is in memory. CSV is an **explicit import/export of availability offerings**, not a database or a backup of the application. Restarting discards bookings as well as other state. Never use real patient information in this demonstration.

## Exact booking walkthrough

Paths below assume you launched from the repository root. Menu numbers are stable:

1. Choose `5`, enter `prototype/samples/availability-future.csv`. Expect `ImportResult[professionalsCreated=5, slotsCreated=8]`. Amélie is `PRO1`; the first slot is `S1`.
2. Choose `1`: first name `Alex`, last name `Example`, birth date `1990-01-01`, phone `555-0100`, insurance/RAMQ `DEMO1` (or leave blank for a private/uninsured patient). Expect `P1`.
3. Choose `2`: service name `Visit`, duration `30`, requires referral `n`. Expect `M1`.
4. Choose `7`, enter `Alex`. Choose the desired patient explicitly if there are several matches.
5. Choose `8`: service `M1`, professional `PRO1`, from `2099-11-02`, to `2099-11-02`. Expect `S1` at 09:00 and `S2` at 09:30, both at the single clinic.
6. Choose `9`: patient `P1`, service `M1`, slot `S1`, channel `PHONE` (or `CLINIC`), referral blank. Expect appointment `A1`, `status=requested`, `requestedBy=staff`. **It is not confirmed.**
7. Repeat step 5. Only `S2` remains. Re-requesting `S1` is rejected with `slotUnavailable`; no second appointment is created.
8. Choose `10` to inspect full in-memory state. All availability is shown, including reserved slots, separately from appointments.
9. Choose `6`, enter `/tmp/clinic-availability.csv`. All eight offerings are exported, including the reserved slot. Reimporting this into the same process is rejected as duplicate availability and does not clear its reservation. Importing into a fresh process restores offerings only, **not bookings**.
10. Choose `0` to discard state and exit.

To run the same interaction noninteractively:

```sh
./prototype/run.sh <<'INPUT'
5
prototype/samples/availability-future.csv
1
Alex
Example
1990-01-01
555-0100
DEMO1
2
Visit
30
n
7
Alex
8
M1
PRO1
2099-11-02
2099-11-02
9
P1
M1
S1
PHONE

10
0
INPUT
```

`availability.csv` contains the **exact eight supplied November 2026 rows**, including UTF-8 `Amélie`, the five professionals, and the required headers. Import permits past offerings so that the full dataset can be retained and exported. Search and booking reject nonfuture starts. `availability-future.csv` uses the same rows with year 2099 for a runnable demonstration before that year. If those dates are no longer future, create new slots using the menu below; neither sample is automatically seeded.

## Make availability without any import

Start a fresh process:

1. Choose `3`; enter `Robin`, `Example`, `Dr.`, `Family Medicine`. Expect `PRO1`.
2. Choose `4`: professional `PRO1`, from `2099-11-02`, to `2099-11-16`, recurrence `WEEKLY`, weekdays `MONDAY,WEDNESDAY`, start `09:00`, end `10:00`, slot duration `30`. The matching weekdays **within inclusive bounds** are expanded into two slots per date. All weekdays, including weekends, are valid choices.
3. For monthly recurrence, choose `4`: professional `PRO1`, from `2099-12-01`, to `2100-03-31`, recurrence `MONTHLY`, day `31`, start `13:00`, end `14:00`, slot duration `30`. December, January and March produce slots; February is **skipped**, not clamped. Start/end bounds remain inclusive.
4. Register a patient and service, then search/book using the printed IDs.

The date window may cover at most 3661 inclusive days. Times must be minute-precision, on one day, with start before end. The window must divide exactly into positive-duration slots; a remainder is rejected rather than silently discarded. Overlapping or duplicate offerings for the same professional are rejected atomically, including conflicts on a later recurrence date. Separate professionals may have simultaneous offerings. An otherwise valid recurrence matching no date creates zero slots and records the schedule; the CLI reports that count. Offering past dates is allowed, but not booking them.

## Referral demonstration (optional)

Register a service with `requires referral = y`. Booking without a referral fails with `referralRequired`. Choose `11` to create a demo referral for a registered patient, with a target professional ID and/or specialty, plus active `y/n`. Use the resulting `R1` when booking. Any supplied referral, even for an optional-referral service, must exist, be active, belong to the patient, and match **every supplied target**. A successful request does not consume the referral. This demo registration is not a real referral issuance workflow or permission system.

## Rules and deliberate simplifications

- One configured **Main clinic**; all professionals practise there. `America/Toronto` is the clinic-local time zone. The location and time-zone constants are configuration, not seeded domain data. No location registration or cross-clinic travel rules.
- Booking is performed by a simulated administrative staff operator for a patient at the clinic or by phone. Both channels store `requestedBy=staff`, `status=requested`. There is no automatic confirmation and no patient self-service actor.
- A service specifies duration and whether referral is required. The booked interval is `[slot.start, slot.start + service.durationMinutes)`. The **entire interval** must fit in one offered slot; adjacent slots cannot be combined. Only the slot's start is a candidate; arbitrary start times are not offered.
- A shorter service is allowed within a larger slot, but **reserves the whole selected slot**. The remainder is not split or offered again. Patient conflicts use the actual service interval; professional reservations additionally lock that entire slot. The appointment stores derived `endTime` for clarity in this PoC, unlike the analysis model where duration is derived from the service.
- Requested appointments reserve time. Professional and patient intervals cannot overlap; touching endpoints are allowed. Schedule validation is represented by containment within explicit materialized availability. Imported rows are explicit one-off offerings, not reconstructed recurring schedules.
- Search is **advisory**, filters only professional conflicts, and does not reserve or evaluate patient/referral constraints. Booking repeats time, full-interval, referral, and conflict checks. Every façade operation is `synchronized` on the same `ClinicSystem` instance; check-and-reserve cannot interleave. This is single-process serialization, not a distributed transaction or database lock.
- `Clock` is injectable in `ClinicSystem(Clock)`; time must be strictly later than current clinic-local time. Tests advance a clock after search to demonstrate booking revalidation. Minute-resolution local times are used; DST ambiguity/nonexistent-time policy is not production-grade.
- Patient registration requires name, birth date (not future), and phone. Insurance/RAMQ is optional; only **nonblank** insurance values must be unique. O01 is simplified to one nonblank query: partial combined name, or exact patient ID/phone/insurance. It does not implement the original multi-field criteria API.
- Service/professional compatibility, resource allocation, rooms/equipment, waitlists, offers, cancellation, confirmation, arrival, clinical records, consultations, prescriptions, follow-up, payments, auth, role/care-scope enforcement, and audit are **not implemented**. Arrival/consultation/waitlist/resource diagrams elsewhere are design-only, not claims of executable features. This PoC does not implement CR1/CR3 or all nine operation contracts.

## CSV contract

Exact ordered header:

```csv
First Name,Last Name,Title,Specialty,Date,Start,End
```

UTF-8 input/output preserves accents; an initial UTF-8 BOM and CRLF or LF are accepted. Dates use ISO `YYYY-MM-DD`; times use ISO local time with minute precision (`HH:MM` recommended). All seven values are required; end must follow start on the same date. CSV supports quoted commas/newlines and doubled quotes. Malformed quoting, blank records, missing/extra columns, malformed dates/times, duplicate or overlapping professional availability are rejected with a record number where applicable.

Professionals deduplicate by trimmed, case-insensitive **first name + last name + title + specialty**. Accents are preserved and not stripped. New rows for an existing professional are allowed if they do not overlap. Import parses/validates the whole file, stages all professional/slot changes, then commits together. An exact duplicate is an error (not an idempotent merge). No failed import creates a professional, adds a slot, or resets a booking.

Export contains the **entire availability dataset**, including past and reserved offerings, never just the current search result. It excludes IDs, patients, services, appointments, referrals, and recurrence rules. Reserved status cannot be represented in this specified seven-column format. Output is written to a temporary sibling file, then moved over the target (atomic replacement when the filesystem supports it). The parent directory must exist; an existing target is replaced. Header-only export/import is valid for an empty dataset. Professionals with no slots are not representable in this format.

## Class and system-operation map

`Domain` contains immutable nested records (`Patient`, `ClinicProfessional`, `MedicalService`, `AvailabilitySlot`, `Schedule`, `Appointment`, `Referral`, `CsvRow`, `ImportResult`) and enums. No mutable collection is exposed.

| Class | Responsibility / collaboration |
|---|---|
| `Main` | Interactive UTF-8 terminal adapter; collects complete input before invoking an operation; reports validation errors and handles EOF. |
| `ClinicSystem` | Public synchronized façade; owns patient/service/referral catalogs and injected clock; the single transaction boundary. |
| `AvailabilityCatalog` | Professional identity and offered slots; expands/stages/validates/commits `Schedule` recurrence; atomic `importRows`. |
| `AppointmentBook` | In-memory reservation ledger; `professionalAvailable`, `patientAvailable`, `reserve`; stores requested appointments only. |
| `CsvAvailabilityStore` | Strict CSV parsing/serialization and filesystem I/O, no business-state mutation. |

| Operation | Actual Java signature and call path |
|---|---|
| Search patient (O01 subset) | `ClinicSystem.searchPatient(String query)` -> matching immutable patients. |
| Search availability (O02 subset) | `searchAvailability(String serviceId, String professionalId, LocalDate from, LocalDate to)` -> catalog slots filtered by duration/future/bounds and `AppointmentBook.professionalAvailable`. |
| Request appointment (O03 subset) | `requestAppointment(String patientId, String serviceId, String slotId, RequestChannel channel, String referralId)` -> catalog `slot`/`professional`, clock/full-fit/referral checks, book conflict checks, `AppointmentBook.reserve`. Professional, location and start derive from selected slot; they are not repeated caller arguments. |
| Make availability | `makeAvailability(Schedule schedule)` -> `AvailabilityCatalog.makeAvailability` -> expand recurrence -> `validateNewSlots` -> commit slots and schedule. |
| Import availability | `importAvailability(Path path)` -> `CsvAvailabilityStore.read` -> `AvailabilityCatalog.importRows` -> staged professional dedupe and slot conflict checks -> commit, return `ImportResult`. |
| Export availability | `exportAvailability(Path path)` -> `CsvAvailabilityStore.write` with **all** catalog slots/professionals; returns `void`, CLI reports success. |

Registration methods are `registerPatient`, `registerService`, `registerProfessional`, and `registerReferral`. Snapshot accessors (`patients`, `services`, `professionals`, `slots`, `schedules`, `appointments`, `referrals`) support the state display and tests. These snapshots are immutable lists of immutable records.

## Verification

`test.sh` compiles production and test sources with Java 17 language/API targeting and `-Xlint:all`, then runs `ClinicSystemTest` with assertions enabled. Explicit check helpers remain active without `-ea`. Tests cover:

- empty boot; registration rejection; optional insurance; patient search and immutable snapshots;
- professional dedupe; inclusive weekly/monthly recurrence; skipped month days; divisibility/invalid ranges; no partial slots or schedules after a late recurrence collision;
- service full-interval fit, adjacent endpoints, requested reservations, patient/professional overlap, stale advisory results, shorter-service whole-slot locking;
- missing/inactive/wrong-patient/wrong-professional/wrong-specialty referrals and valid multi-target referral;
- injected clock, start exactly now, past starts, booking after time advances;
- exact supplied sample (eight slots/five professionals), UTF-8 roundtrip, BOM/CRLF/quoted commas/newlines/quotes;
- malformed and overlapping/duplicate imports with no partial state, including within-file duplicates and attempted import over a booked slot;
- complete export including reserved slots, and header-only empty export;
- two concurrent competing requests with exactly one success;
- real CLI subprocesses: menu/mid-command EOF, malformed number/date recovery, registration, import/search/phone booking.

No real authentication, audit, persistence, resources, waitlists, or clinical operations are asserted by these tests.
