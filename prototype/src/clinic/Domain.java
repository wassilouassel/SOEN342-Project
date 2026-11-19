package clinic;

import java.time.*;
import java.util.*;

/** Immutable values; all mutations go through ClinicSystem. */
public final class Domain {
    private Domain() {}

    public record Patient(String id, String firstName, String lastName, LocalDate dateOfBirth,
                          String phoneNumber, String insuranceNumber) {}
    public record ClinicProfessional(String id, String firstName, String lastName,
                                     String title, String specialty) {}
    public record MedicalService(String id, String name, int durationMinutes, boolean requiresReferral) {}
    public record AvailabilitySlot(String id, String professionalId, LocalDateTime start, LocalDateTime end) {}
    public enum Recurrence { WEEKLY, MONTHLY }
    public enum RequestChannel { CLINIC, PHONE }

    public record Schedule(String professionalId, LocalDate from, LocalDate to,
                           Recurrence recurrence, Set<DayOfWeek> weekdays, Integer dayOfMonth,
                           LocalTime start, LocalTime end, int slotMinutes) {
        public Schedule {
            weekdays = weekdays == null ? Set.of() : Set.copyOf(weekdays);
        }
    }

    public record Appointment(String id, String patientId, String serviceId, String professionalId,
                              String location, String slotId, LocalDateTime dateTime, LocalDateTime endTime,
                              String status, String requestedBy, RequestChannel channel, String referralId) {}
    public record Referral(String id, String patientId, String professionalId, String specialty, boolean active) {}
    public record ImportResult(int professionalsCreated, int slotsCreated) {}
    public record CsvRow(String firstName, String lastName, String title, String specialty,
                         LocalDate date, LocalTime start, LocalTime end) {}

    static String text(String value, String label) {
        if (value == null || value.isBlank()) throw new ClinicException("invalidInput", label + " is required");
        return value.strip();
    }

    static String key(String... values) {
        // Length-delimited rather than delimiter-separated, so names cannot cause key collisions.
        StringBuilder key = new StringBuilder();
        for (String value : values) {
            String normalized = value.strip().toLowerCase(Locale.ROOT);
            key.append(normalized.length()).append(':').append(normalized);
        }
        return key.toString();
    }

    static boolean overlaps(LocalDateTime a, LocalDateTime b, LocalDateTime c, LocalDateTime d) {
        return a.isBefore(d) && c.isBefore(b);
    }
}
