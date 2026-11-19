package clinic;

import clinic.Domain.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import static clinic.Domain.*;

/** Single-process transaction boundary: every public operation acquires this same monitor. */
public final class ClinicSystem {
    public static final String LOCATION = "Main clinic";
    public static final ZoneId CLINIC_ZONE = ZoneId.of("America/Toronto");
    private final Clock clock;
    private final AvailabilityCatalog catalog = new AvailabilityCatalog();
    private final AppointmentBook book = new AppointmentBook();
    private final CsvAvailabilityStore csv = new CsvAvailabilityStore();
    private final Map<String, Patient> patients = new LinkedHashMap<>();
    private final Map<String, MedicalService> services = new LinkedHashMap<>();
    private final Map<String, Referral> referrals = new LinkedHashMap<>();

    public ClinicSystem(Clock clock) { this.clock = Objects.requireNonNull(clock).withZone(CLINIC_ZONE); }
    public ClinicSystem() { this(Clock.system(CLINIC_ZONE)); }
    private LocalDateTime now() { return LocalDateTime.now(clock); }

    public synchronized Patient registerPatient(String first, String last, LocalDate birth, String phone, String insurance) {
        first = text(first, "First name"); last = text(last, "Last name");
        phone = text(phone, "Phone"); insurance = insurance == null ? "" : insurance.strip();
        if (birth == null || birth.isAfter(now().toLocalDate()))
            throw new ClinicException("invalidInput", "Birth date must not be in the future");
        for (Patient p : patients.values()) {
            if (!insurance.isEmpty() && p.insuranceNumber().equalsIgnoreCase(insurance))
                throw new ClinicException("duplicatePatient", "Insurance number already registered as " + p.id());
        }
        Patient p = new Patient("P" + (patients.size() + 1), first, last, birth, phone, insurance);
        patients.put(p.id(), p); return p;
    }

    /** Simplified O01: one nonblank query matches name fragments, or exact ID/phone/insurance. */
    public synchronized List<Patient> searchPatient(String query) {
        String q = text(query, "Patient query").toLowerCase(Locale.ROOT);
        return patients.values().stream().filter(p -> p.id().equalsIgnoreCase(q)
                || (p.firstName() + " " + p.lastName()).toLowerCase(Locale.ROOT).contains(q)
                || p.phoneNumber().equalsIgnoreCase(q) || p.insuranceNumber().equalsIgnoreCase(q)).toList();
    }

    public synchronized MedicalService registerService(String name, int minutes, boolean requiresReferral) {
        name = text(name, "Service name");
        if (minutes < 1 || minutes > 1440) throw new ClinicException("invalidInput", "Duration must be 1–1440 minutes");
        MedicalService service = new MedicalService("M" + (services.size() + 1), name, minutes, requiresReferral);
        services.put(service.id(), service); return service;
    }

    public synchronized ClinicProfessional registerProfessional(String first, String last, String title, String specialty) {
        return catalog.registerProfessional(first, last, title, specialty);
    }

    public synchronized Referral registerReferral(String patientId, String professionalId, String specialty, boolean active) {
        patient(patientId);
        professionalId = professionalId == null || professionalId.isBlank() ? null : professionalId.strip();
        specialty = specialty == null || specialty.isBlank() ? null : specialty.strip();
        if (professionalId == null && specialty == null)
            throw new ClinicException("invalidInput", "Referral needs a professional and/or specialty target");
        if (professionalId != null) catalog.professional(professionalId);
        Referral r = new Referral("R" + (referrals.size() + 1), patientId, professionalId, specialty, active);
        referrals.put(r.id(), r); return r;
    }

    public synchronized List<AvailabilitySlot> makeAvailability(Schedule schedule) { return catalog.makeAvailability(schedule); }

    /** Advisory, professional-only conflicts. One candidate per whole offered slot, starting at its start. */
    public synchronized List<AvailabilitySlot> searchAvailability(String serviceId, String professionalId, LocalDate from, LocalDate to) {
        MedicalService service = service(serviceId); catalog.professional(professionalId);
        if (from == null || to == null || from.isAfter(to)) throw new ClinicException("invalidInput", "Invalid date range");
        LocalDateTime current = now();
        return catalog.slots().stream().filter(s -> s.professionalId().equals(professionalId)
                && !s.start().toLocalDate().isBefore(from) && !s.start().toLocalDate().isAfter(to)
                && s.start().isAfter(current) && !s.start().plusMinutes(service.durationMinutes()).isAfter(s.end())
                && book.professionalAvailable(s, s.start().plusMinutes(service.durationMinutes())))
                .sorted(Comparator.comparing(AvailabilitySlot::start)).toList();
    }

    public synchronized Appointment requestAppointment(String patientId, String serviceId, String slotId,
                                                        RequestChannel channel, String referralId) {
        Patient patient = patient(patientId); MedicalService service = service(serviceId);
        AvailabilitySlot slot = catalog.slot(slotId);
        ClinicProfessional professional = catalog.professional(slot.professionalId());
        if (channel == null) throw new ClinicException("invalidInput", "Choose CLINIC or PHONE");
        LocalDateTime end = slot.start().plusMinutes(service.durationMinutes());
        if (!slot.start().isAfter(now()) || end.isAfter(slot.end()))
            throw new ClinicException("slotUnavailable", "Appointment must be strictly future and fit entirely within one slot");
        if (!book.professionalAvailable(slot, end) || !book.patientAvailable(patientId, slot.start(), end))
            throw new ClinicException("slotUnavailable", "Professional, patient, or whole slot already reserved");
        referralId = referralId == null || referralId.isBlank() ? null : referralId.strip();
        if (referralId == null && service.requiresReferral())
            throw new ClinicException("referralRequired", "This service requires a referral");
        if (referralId != null) {
            Referral r = referrals.get(referralId);
            if (r == null || !r.active() || !r.patientId().equals(patientId)
                    || (r.professionalId() != null && !r.professionalId().equals(professional.id()))
                    || (r.specialty() != null && !r.specialty().equalsIgnoreCase(professional.specialty())))
                throw new ClinicException("referralInvalid", "Referral missing, inactive, or patient/target mismatch");
        }
        return book.reserve(patient, service, slot, LOCATION, channel, referralId);
    }

    public synchronized ImportResult importAvailability(Path path) throws IOException { return catalog.importRows(csv.read(path)); }
    public synchronized void exportAvailability(Path path) throws IOException { csv.write(path, catalog.slots(), catalog.professionals()); }

    public synchronized List<Patient> patients() { return List.copyOf(patients.values()); }
    public synchronized List<MedicalService> services() { return List.copyOf(services.values()); }
    public synchronized List<ClinicProfessional> professionals() { return catalog.professionals(); }
    public synchronized List<AvailabilitySlot> slots() { return catalog.slots(); }
    public synchronized List<Schedule> schedules() { return catalog.schedules(); }
    public synchronized List<Appointment> appointments() { return book.appointments(); }
    public synchronized List<Referral> referrals() { return List.copyOf(referrals.values()); }

    private Patient patient(String id) {
        Patient p = patients.get(id);
        if (p == null) throw new ClinicException("notFound", "Patient not found: " + id);
        return p;
    }
    private MedicalService service(String id) {
        MedicalService s = services.get(id);
        if (s == null) throw new ClinicException("notFound", "Service not found: " + id);
        return s;
    }
}
