# SOEN342-Project

SOEN 342 Software Engineering Project — Appointment Tracking System.

## Team Credentials

| Name | Student ID |
|---|---|
| armin mohebbipou | 40243251 |
| wassil ouassel | 40320013 |
| Mostafa Maraie | 40261020 |

## Iteration 1 — final analysis submission

The final artifacts are in **[final-submission/docs/iteration-1/](final-submission/docs/iteration-1/)**. This iteration supplies analysis, not an implemented application. The eventual system must be object-oriented and CLI-only, with no graphical or web interface.

1. [Requirements, scope, assumptions and critical/non-critical use-case catalogue](final-submission/docs/iteration-1/requirements.md)
2. [Use-case diagram](final-submission/docs/iteration-1/diagrams/use-cases.png) · [PlantUML source](final-submission/docs/iteration-1/diagrams/use-cases.puml)
3. [UML domain model](final-submission/docs/iteration-1/diagrams/domain-model.png) · [PlantUML source](final-submission/docs/iteration-1/diagrams/domain-model.puml)
4. [Three fully dressed critical use cases](final-submission/docs/iteration-1/scenarios.md), each with success and failure scenarios:
   - UC03 Request Appointment — [SSD](final-submission/docs/iteration-1/diagrams/ssd-request-appointment.png)
   - UC07 Register Arrival — [SSD](final-submission/docs/iteration-1/diagrams/ssd-register-arrival.png)
   - UC08 Conduct Consultation and Record Clinical Information — [SSD](final-submission/docs/iteration-1/diagrams/ssd-record-consultation.png)
5. [Supporting use-case success/failure scenarios](final-submission/docs/iteration-1/supporting-scenarios.md)
6. [System operations and operation contracts](final-submission/docs/iteration-1/operation-contracts.md)
7. [Requirement traceability](final-submission/docs/iteration-1/traceability.md)

The root PDFs are retained as original drafts, not the current submission. Final PNG diagrams can be viewed directly without a renderer.

### Regenerate diagrams

With Java and PlantUML 1.2026.1, set `PLANTUML_JAR` to your local jar and run from the repository root:

```sh
java -Djava.awt.headless=true -jar "$PLANTUML_JAR" -charset UTF-8 -tpng final-submission/docs/iteration-1/diagrams/*.puml
```

Non-sequence diagrams use the Java Smetana layout engine; Graphviz is not required. Additional policy rules are labeled as provisional assumptions. The preliminary-iteration assessment section was not provided, so compliance with that unseen rubric cannot be asserted.
