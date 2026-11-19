# Appointment Tracking System

Clinic scheduling terms used by the Iteration II analysis model.

## Language

**Waiting-list entry**:
One patient's request for a service at a clinic, with professional, specialty or date preferences and an administrative priority. Acceptance fills one explicitly selected entry, not every matching request by that patient.
_Avoid_: Offer, reservation

**Resource**:
A predefined room, equipment item or assisting-role pool available at a clinic. A specific room or equipment item has capacity one; an assisting-role pool represents capacity without identifying individual personnel.
_Avoid_: Named employee (for an assisting role)

**Resource requirement**:
The quantity of a particular predefined resource needed by a medical service. Requiring two specific rooms means one requirement for each room; requiring two nurses means two units of the nurse-role pool.
_Avoid_: Allocation

**Resource allocation**:
The quantity of a resource committed to one appointment for its full interval. Concurrent active allocations cannot exceed the resource's capacity.
_Avoid_: Requirement, offer hold
