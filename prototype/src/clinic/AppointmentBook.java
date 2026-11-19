package clinic;

import clinic.Domain.*;
import java.time.*;
import java.util.*;
import static clinic.Domain.overlaps;

/** Reservation ledger. Called only while the façade monitor is held. */
final class AppointmentBook {
    private final List<Appointment> appointments = new ArrayList<>();

    List<Appointment> appointments() { return List.copyOf(appointments); }

    boolean professionalAvailable(AvailabilitySlot slot, LocalDateTime end) {
        return appointments.stream().noneMatch(a -> a.slotId().equals(slot.id())
                || (a.professionalId().equals(slot.professionalId())
                && overlaps(slot.start(), end, a.dateTime(), a.endTime())));
    }

    boolean patientAvailable(String patientId, LocalDateTime start, LocalDateTime end) {
        return appointments.stream().noneMatch(a -> a.patientId().equals(patientId)
                && overlaps(start, end, a.dateTime(), a.endTime()));
    }

    Appointment reserve(Patient patient, MedicalService service, AvailabilitySlot slot,
                        String location, RequestChannel channel, String referralId) {
        LocalDateTime end = slot.start().plusMinutes(service.durationMinutes());
        if (!professionalAvailable(slot, end) || !patientAvailable(patient.id(), slot.start(), end))
            throw new ClinicException("slotUnavailable", "Professional, patient, or whole slot already reserved");
        Appointment a = new Appointment("A" + (appointments.size() + 1), patient.id(), service.id(),
                slot.professionalId(), location, slot.id(), slot.start(), end, "requested", "staff", channel, referralId);
        appointments.add(a);
        return a;
    }
}
