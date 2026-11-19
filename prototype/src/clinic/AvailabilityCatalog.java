package clinic;

import clinic.Domain.*;
import java.time.*;
import java.util.*;
import static clinic.Domain.*;

/** Owns professional identity and offered time; never owns booking state. */
final class AvailabilityCatalog {
    private final Map<String, ClinicProfessional> professionals = new LinkedHashMap<>();
    private final Map<String, AvailabilitySlot> slots = new LinkedHashMap<>();
    private final List<Schedule> schedules = new ArrayList<>();

    List<ClinicProfessional> professionals() { return List.copyOf(professionals.values()); }
    List<AvailabilitySlot> slots() { return List.copyOf(slots.values()); }
    List<Schedule> schedules() { return List.copyOf(schedules); }

    ClinicProfessional professional(String id) {
        ClinicProfessional p = professionals.get(id);
        if (p == null) throw new ClinicException("notFound", "Professional not found: " + id);
        return p;
    }

    AvailabilitySlot slot(String id) {
        AvailabilitySlot slot = slots.get(id);
        if (slot == null) throw new ClinicException("notFound", "Slot not found: " + id);
        return slot;
    }

    ClinicProfessional registerProfessional(String first, String last, String title, String specialty) {
        first = text(first, "First name"); last = text(last, "Last name");
        title = text(title, "Title"); specialty = text(specialty, "Specialty");
        String identity = key(first, last, title, specialty);
        for (ClinicProfessional p : professionals.values()) {
            if (identity(p).equals(identity)) return p;
        }
        ClinicProfessional p = new ClinicProfessional("PRO" + (professionals.size() + 1), first, last, title, specialty);
        professionals.put(p.id(), p);
        return p;
    }

    private String identity(ClinicProfessional p) { return key(p.firstName(), p.lastName(), p.title(), p.specialty()); }

    List<AvailabilitySlot> makeAvailability(Schedule s) {
        if (s == null) throw new ClinicException("invalidInput", "Schedule is required");
        professional(s.professionalId());
        if (s.from() == null || s.to() == null || s.from().isAfter(s.to()) || s.recurrence() == null
                || s.start() == null || s.end() == null || !s.start().isBefore(s.end())
                || s.slotMinutes() <= 0 || s.start().getSecond() != 0 || s.start().getNano() != 0
                || s.end().getSecond() != 0 || s.end().getNano() != 0) {
            throw new ClinicException("invalidInput", "Use an ordered date range and minute-precision same-day times; duration must be positive");
        }
        if (java.time.temporal.ChronoUnit.DAYS.between(s.from(), s.to()) > 3660)
            throw new ClinicException("invalidInput", "Date range cannot exceed 3661 inclusive days");
        if ((s.recurrence() == Recurrence.WEEKLY && s.weekdays().isEmpty())
                || (s.recurrence() == Recurrence.MONTHLY && (s.dayOfMonth() == null || s.dayOfMonth() < 1 || s.dayOfMonth() > 31)))
            throw new ClinicException("invalidInput", "Weekly recurrence needs weekdays; monthly needs a day from 1 to 31");
        long minutes = Duration.between(s.start(), s.end()).toMinutes();
        if (minutes % s.slotMinutes() != 0)
            throw new ClinicException("invalidInput", "Time window must be exactly divisible by slot duration (no discarded remainder)");
        List<AvailabilitySlot> staged = new ArrayList<>();
        for (LocalDate day = s.from(); !day.isAfter(s.to()); day = day.plusDays(1)) {
            boolean matches = s.recurrence() == Recurrence.WEEKLY
                    ? s.weekdays().contains(day.getDayOfWeek()) : day.getDayOfMonth() == s.dayOfMonth();
            if (!matches) continue;
            LocalDateTime limit = day.atTime(s.end());
            for (LocalDateTime start = day.atTime(s.start()); start.isBefore(limit); start = start.plusMinutes(s.slotMinutes())) {
                staged.add(new AvailabilitySlot("S" + (slots.size() + staged.size() + 1), s.professionalId(), start, start.plusMinutes(s.slotMinutes())));
            }
        }
        validateNewSlots(staged);
        staged.forEach(slot -> slots.put(slot.id(), slot));
        schedules.add(s);
        return List.copyOf(staged);
    }

    private void validateNewSlots(List<AvailabilitySlot> staged) {
        List<AvailabilitySlot> checked = new ArrayList<>(slots.values());
        for (AvailabilitySlot candidate : staged) {
            for (AvailabilitySlot existing : checked) {
                if (existing.professionalId().equals(candidate.professionalId())
                        && overlaps(existing.start(), existing.end(), candidate.start(), candidate.end()))
                    throw new ClinicException("duplicateAvailability", "Duplicate/overlapping availability for "
                            + candidate.professionalId() + " at " + candidate.start() + "; no changes applied");
            }
            checked.add(candidate);
        }
    }

    ImportResult importRows(List<CsvRow> rows) {
        // A private staging catalog prevents even professional registration leaking on a bad row.
        AvailabilityCatalog staged = new AvailabilityCatalog();
        staged.professionals.putAll(professionals);
        staged.slots.putAll(slots);
        int before = professionals.size();
        List<AvailabilitySlot> additions = new ArrayList<>();
        int rowNumber = 1;
        for (CsvRow row : rows) {
            rowNumber++;
            ClinicProfessional p = staged.registerProfessional(row.firstName(), row.lastName(), row.title(), row.specialty());
            AvailabilitySlot slot = new AvailabilitySlot("S" + (slots.size() + additions.size() + 1), p.id(),
                    row.date().atTime(row.start()), row.date().atTime(row.end()));
            try { staged.validateNewSlots(List.of(slot)); }
            catch (ClinicException e) { throw new ClinicException(e.code(), "CSV record " + rowNumber + ": " + e.getMessage()); }
            staged.slots.put(slot.id(), slot);
            additions.add(slot);
        }
        professionals.clear(); professionals.putAll(staged.professionals);
        slots.clear(); slots.putAll(staged.slots);
        return new ImportResult(professionals.size() - before, additions.size());
    }
}
