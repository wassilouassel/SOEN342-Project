# SOEN342-Project

SOEN 342 Software Engineering Project — Appointment Tracking System.

## Team Credentials

| Name | Student ID |
|---|---|
| armin mohebbipou | 40243251 |
| wassil ouassel | 40320013 |
| Mostafa Maraie | 40261020 |

## Iteration 2 — design and functional prototype

Start with **[the iteration-2 delivery guide](docs/iteration-2/README.md)** for the diagram catalogue, requirement mapping and implementation scope.

- **Updated analysis diagrams:** [use-case diagram and UML domain model](diagrams/iteration-2/analysis/), based on the team's existing artifacts.
- **Updated SSDs:** [7 editable PlantUML sources](diagrams/iteration-2/ssd/).
- **UML interaction diagrams:** [the three original critical use cases](diagrams/iteration-2/interactions/).
- **Partial class diagrams:** [scheduling and arrival/consultation design views](diagrams/iteration-2/design/).
- **Rendered diagrams:** [PNG and SVG files](diagrams/iteration-2/rendered/).
- **Contracts:** [iteration-2 additions and amendments](docs/iteration-2/operation-contracts-additions.md).
- **Java CLI PoC:** [source, examples and instructions](prototype/README.md). Booking, availability search, recurring availability creation and CSV import/export; no external Java dependencies or database.

With a JDK supporting Java 17 or later:

```bash
./prototype/test.sh
./prototype/run.sh
```

The prototype starts empty. Waitlists/resources are covered by analysis diagrams and SSDs; their detailed design is deferred as suggested by the assignment. Arrival/consultation have interaction and class designs but are not implemented PoC features. Production authentication/audit are not implemented. Use synthetic patient data only.

To regenerate diagrams: `./tools/render-diagrams.sh svg` (requires PlantUML installed or `PLANTUML_JAR` pointing to a local JAR).

## Existing analysis artifacts

The earlier README linked to `final-submission/docs/iteration-1/`, which is not present in this checkout. The available baseline artifacts are:

1. [Use-case diagram](diagrams/final%20Use%20case%20diagram%20363.png)
2. [UML domain model](diagrams/final%20uml%20domain.png)
3. UC03 Request Appointment — [original SSD](ssd1%20-%20SSD%20-%20Request%20Appointment.png)
4. UC07 Register Arrival — [original SSD](final%20ssd2.png)
5. UC08 Conduct Consultation and Record Clinical Information — [original SSD](final%20ssd3.png)
6. [Original operation contracts](operation-contracts-lean.md)

These originals remain unchanged for reference; the updated analysis diagrams, SSDs and design artifacts are under `diagrams/iteration-2/`.
