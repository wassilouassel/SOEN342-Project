# Iteration 2 — Assignment artifacts

This submission builds on the team's existing [use-case diagram](../../diagrams/final%20Use%20case%20diagram%20363.png), [domain model](../../diagrams/final%20uml%20domain.png), three SSDs and [operation contracts](../../operation-contracts-lean.md). Originals are preserved. The updated versions below reconcile the assignment's additional requirements with those artifacts.

## Scope

The assignment requires updated use-case/domain diagrams and SSDs, interaction diagrams for at least three critical use cases, a partial design class model and a functional prototype.

The PoC implements only the specified scheduling functionality: booking at the clinic/by phone, searching availability, creating recurring availability and importing/exporting availability CSV. Patient, professional and service registration support those operations. Referral validation follows the existing booking contract.

The screenshots suggest leaving detailed design of the new waitlist/resource changes to Iteration III. They are represented here in **analysis diagrams and system events**, not implemented or expanded into speculative offer/hold infrastructure. The original arrival and consultation workflows have interaction/class designs but are not required PoC features.

## Updated use-case and domain diagrams

| Artifact | Editable source | Preview |
|---|---|---|
| Updated use-case diagram, critical and non-critical cases | [PlantUML](../../diagrams/iteration-2/analysis/use-cases.puml) | [PNG](../../diagrams/iteration-2/rendered/analysis/use-cases.png) |
| Updated UML domain model | [PlantUML](../../diagrams/iteration-2/analysis/domain-model.puml) | [PNG](../../diagrams/iteration-2/rendered/analysis/domain-model.png) |

The complete diagrams remain overview/reference views. For normal-page reading, use the focused views below; domain views include/filter the same model rather than duplicating its definitions.

| Focused use-case view | Editable source | Preview |
|---|---|---|
| Appointments and waiting list | [PlantUML](../../diagrams/iteration-2/analysis/use-cases-scheduling.puml) | [PNG](../../diagrams/iteration-2/rendered/analysis/use-cases-scheduling.png) |
| Consultation and clinical records | [PlantUML](../../diagrams/iteration-2/analysis/use-cases-clinical.puml) | [PNG](../../diagrams/iteration-2/rendered/analysis/use-cases-clinical.png) |
| Registration, administration and billing | [PlantUML](../../diagrams/iteration-2/analysis/use-cases-administration.puml) | [PNG](../../diagrams/iteration-2/rendered/analysis/use-cases-administration.png) |

Use-case views share [definitions and relationships](../../diagrams/iteration-2/analysis/use-cases-shared.iuml); unmarked cases are explicitly non-critical.

| Focused domain view | Editable source | Preview |
|---|---|---|
| People, scheduling and referrals | [PlantUML](../../diagrams/iteration-2/analysis/domain-scheduling.puml) | [PNG](../../diagrams/iteration-2/rendered/analysis/domain-scheduling.png) |
| Waiting lists | [PlantUML](../../diagrams/iteration-2/analysis/domain-waiting-list.puml) | [PNG](../../diagrams/iteration-2/rendered/analysis/domain-waiting-list.png) |
| Resource quantities and capacity | [PlantUML](../../diagrams/iteration-2/analysis/domain-resources.puml) | [PNG](../../diagrams/iteration-2/rendered/analysis/domain-resources.png) |
| Clinical information, billing and audit | [PlantUML](../../diagrams/iteration-2/analysis/domain-clinical-billing.puml) | [PNG](../../diagrams/iteration-2/rendered/analysis/domain-clinical-billing.png) |

The redraw retains the team's actors, use cases and domain vocabulary. Waiting-list preferences/priority, staff removal, resource requirements, predefined resource subtypes, availability slots and recurring schedules come from the existing model. The analysis now makes allocation quantities and resource capacity explicit. No `WaitingListOffer`, resource-hold entity or withdrawal workflow is introduced.

The existing offered-slot association records which appointment is offered; it does not reserve capacity. A declined patient stays on the waiting list unless staff removes them. Offers follow highest-priority-first order; accepted booking supplies the exact waiting-list entry ID. Resource availability is checked for the complete appointment interval. `ResourceAllocation.quantityAllocated` makes quantities explicit: rooms/equipment have capacity one, while assisting roles are predefined capacity pools, never named personnel. See [domain vocabulary](../../GLOSSARY.md). Billing and clinical concepts from the team model are retained without implementing additional functionality.

## Updated SSDs

| Artifact | Editable source | Preview |
|---|---|---|
| SSD 1 — Request Appointment (UC03) | [PlantUML](../../diagrams/iteration-2/ssd/01-request-appointment.puml) | [PNG](../../diagrams/iteration-2/rendered/ssd/01-request-appointment.png) |
| SSD 2 — Register Arrival (UC07) | [PlantUML](../../diagrams/iteration-2/ssd/02-register-arrival.puml) | [PNG](../../diagrams/iteration-2/rendered/ssd/02-register-arrival.png) |
| SSD 3 — Record Consultation (UC08) | [PlantUML](../../diagrams/iteration-2/ssd/03-record-consultation.puml) | [PNG](../../diagrams/iteration-2/rendered/ssd/03-record-consultation.png) |
| SSD 4 — Join / Manage Waiting List | [PlantUML](../../diagrams/iteration-2/ssd/04-manage-waiting-list.puml) | [PNG](../../diagrams/iteration-2/rendered/ssd/04-manage-waiting-list.png) |
| SSD 5 — Cancel and Offer Available Appointment | [PlantUML](../../diagrams/iteration-2/ssd/05-cancel-and-offer.puml) | [PNG](../../diagrams/iteration-2/rendered/ssd/05-cancel-and-offer.png) |
| SSD 6 — Make Availability | [PlantUML](../../diagrams/iteration-2/ssd/06-make-availability.puml) | [PNG](../../diagrams/iteration-2/rendered/ssd/06-make-availability.png) |
| SSD 7 — Import / Export Availability | [PlantUML](../../diagrams/iteration-2/ssd/07-import-export-availability.puml) | [PNG](../../diagrams/iteration-2/rendered/ssd/07-import-export-availability.png) |

Booking still returns `requested`, arrival leaves status `confirmed`, and consultation recording completes the appointment. Prescription/follow-up failures do not undo a recorded consultation. These rules come from the team's SSDs/contracts.

## Three critical-use-case interaction diagrams

| Artifact | Status | Editable source | Preview |
|---|---|---|---|
| Request Appointment (UC03) | Implemented scheduling PoC | [PlantUML](../../diagrams/iteration-2/interactions/01-request-appointment.puml) | [PNG](../../diagrams/iteration-2/rendered/interactions/01-request-appointment.png) |
| Register Arrival (UC07) | Design only | [PlantUML](../../diagrams/iteration-2/interactions/02-register-arrival.puml) | [PNG](../../diagrams/iteration-2/rendered/interactions/02-register-arrival.png) |
| Record Consultation (UC08) | Design only | [PlantUML](../../diagrams/iteration-2/interactions/03-record-consultation.puml) | [PNG](../../diagrams/iteration-2/rendered/interactions/03-record-consultation.png) |

## Partial design class model

The two views together cover the three critical interaction diagrams:

- **Scheduling classes:** [PlantUML](../../diagrams/iteration-2/design/partial-class-diagram.puml) · [PNG](../../diagrams/iteration-2/rendered/design/partial-class-diagram.png).
- **Arrival/consultation classes:** [PlantUML](../../diagrams/iteration-2/design/clinical-class-diagram.puml) · [PNG](../../diagrams/iteration-2/rendered/design/clinical-class-diagram.png). Includes the controllers, access checks, clock, audit, clinical-record lookup and atomic-change collaborators referenced by interactions 2–3. These are design classes, not additional implemented features.

`AppointmentBook`/`Appointment` in the clinical view describe the target extension of the scheduling classes, not a second implemented class. The immutable PoC records are a partial implementation; clinical staging methods are not claimed to exist in Java.

## Functional Java PoC

See [prototype/README.md](../../prototype/README.md) for exact input steps, sample files and limitations.

```bash
./prototype/test.sh
./prototype/run.sh
```

| Conceptual SSD operation | Java mapping |
|---|---|
| `searchPatient(criteria)` | `ClinicSystem.searchPatient(query)`; simplified lookup |
| `searchAvailability(serviceID, professionalID?, locationID?, dateRange, specialtyID?)` | `searchAvailability(serviceId, professionalId, from, to)`; professional-specific subset, one clinic |
| `requestAppointment(patientID, serviceID, professionalID, locationID, dateTime, referralID?, waitingListEntryID?)` | `requestAppointment(patientId, serviceId, slotId, channel, referralId)`; slot supplies professional/time and clinic is configured; waitlist linkage is analysis-only |
| `makeAvailability(...)` | `makeAvailability(Schedule)`; returns generated slots |
| `importAvailability(sourcePath)` | `importAvailability(Path)`; returns import counts |
| `exportAvailability(destinationPath)` | `exportAvailability(Path)`; returns `void`, CLI reports completion |

The PoC starts empty; data is held in memory. Weekly/monthly patterns use inclusive dates. A service must fit in one selected slot; shorter services conservatively reserve that slot without splitting it. Imported slots are explicit one-off availability. UTF-8 CSV export contains all availability definitions, including booked/past ones, but is not a patient/appointment backup. No database or automatic persistence is added.

One clinic (`America/Toronto`) is a PoC simplification. Real access enforcement and audit are baseline design requirements, not implemented production controls. Use synthetic patient data only.

## Requirement mapping

| Assignment requirement | Evidence |
|---|---|
| Updated use-case diagram and domain model | Updated analysis diagrams, retaining team concepts |
| Updated SSDs and new system operations | SSDs 1–7; [contract changes](operation-contracts-additions.md) |
| At least three critical interaction diagrams | UC03, UC07, UC08 |
| UML class diagram / partial design model | Scheduling and clinical views |
| Book at clinic or by phone | PoC `CLINIC`/`PHONE` channels |
| Search availability | PoC advisory search and booking-time revalidation |
| Make recurring availability | PoC date range, weekdays/monthly day, start/end times |
| Import/export availability CSV | Explicit file paths; entire availability dataset exported |
| Start fresh; database optional | Empty startup, in-memory state, no database |
| Waitlist preferences/priority, cancellation eligibility, phone offers, optional removal | Analysis model and SSDs 4–5 |
| Predefined rooms/equipment/assisting roles | Domain model and booking/resource validation in SSDs |

The sample appointment/waiting-patient tables illustrate possible datasets. Only availability import/export is required by the explicit PoC list; no additional importers are implemented. The preliminary-iteration assessment rubric is not present, so this mapping covers the five supplied screenshots and available repository artifacts.

## Checks and rendering

- `./prototype/test.sh`: **86 checks**, including booking conflicts, recurrence, CSV validation/roundtrip, future-time revalidation and CLI behaviour.
- Java sources target Java 17 with `--release 17`; executed on available JDK 27, not separately on a JDK 17 runtime.
- All diagram sources are rendered to PNG and SVG; local documentation links and shell syntax are checked.

Use installed PlantUML or a local JAR (no remote rendering service):

```bash
PLANTUML_JAR=/absolute/path/to/plantuml.jar ./tools/render-diagrams.sh svg
PLANTUML_JAR=/absolute/path/to/plantuml.jar ./tools/render-diagrams.sh png
```

PlantUML 1.2025.2 renders the sources using its built-in Smetana class/use-case layout, without external Graphviz.
