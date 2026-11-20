package clinic;

import clinic.Domain.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Dependency-free integration tests. Checks are active even without -ea. */
public final class ClinicSystemTest {
    private static int checks;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2030-01-01T12:00:00Z"), ClinicSystem.CLINIC_ZONE);
    private static final LocalDate MONDAY = LocalDate.of(2030, 1, 7);
    private static final String HEADER = String.join(",", CsvAvailabilityStore.HEADER) + "\n";
    private static Path temporary;

    public static void main(String[] args) throws Exception {
        temporary = Files.createTempDirectory("clinic-tests-");
        try {
            emptyAndRegistration();
            weeklyAndMonthly();
            bookingAndConflicts();
            referrals();
            futureRevalidation();
            csv(Path.of(args[0]));
            concurrency();
            cli();
            System.out.println("PASS: " + checks + " checks (registration, recurrence, booking, referrals, clock, CSV, concurrency, CLI)");
        } finally {
            try (var paths = Files.walk(temporary)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    private static void emptyAndRegistration() {
        ClinicSystem s = new ClinicSystem(CLOCK);
        check(s.patients().isEmpty() && s.services().isEmpty() && s.professionals().isEmpty()
                && s.slots().isEmpty() && s.appointments().isEmpty() && s.referrals().isEmpty() && s.schedules().isEmpty(), "zero boot data");
        Patient p = patient(s, "Amélie", "123");
        check(s.searchPatient("amé").equals(List.of(p)), "accent name search");
        check(s.searchPatient("123").equals(List.of(p)), "insurance search");
        check(s.searchPatient("unknown").isEmpty(), "empty patient result");
        reject("invalidInput", () -> s.searchPatient(" "));
        reject("duplicatePatient", () -> patient(s, "Other", "123"));
        reject("invalidInput", () -> s.registerPatient("A", "B", LocalDate.of(2031, 1, 1), "1", "2"));
        check(s.patients().size() == 1, "failed registrations atomic");
        Patient privatePatient = patient(s, "Fatima", "");
        Patient secondPrivate = patient(s, "Maya", "  ");
        check(privatePatient.insuranceNumber().isEmpty() && secondPrivate.insuranceNumber().isEmpty()
                && s.patients().size() == 3, "insurance optional and blanks not unique");
        check(s.searchPatient("Fatima").equals(List.of(privatePatient)), "private patient searchable without insurance");
        reject("invalidInput", () -> s.registerService("Test", 0, false));
        check(s.services().isEmpty(), "failed service registration atomic");
        ClinicProfessional professional = professional(s, "A");
        check(s.registerProfessional(" a ", "Example", "dr.", "family medicine").equals(professional), "professional normalized identity");
        check(s.registerProfessional("A", "Example", "Nurse", "Family Medicine").id().equals("PRO2"), "title participates in identity");
        try { s.patients().clear(); throw new AssertionError("Mutable snapshot"); }
        catch (UnsupportedOperationException expected) { checks++; }
    }

    private static void weeklyAndMonthly() {
        ClinicSystem s = new ClinicSystem(CLOCK);
        String p = professional(s, "Weekly").id();
        List<AvailabilitySlot> slots = s.makeAvailability(weekly(p, MONDAY, MONDAY.plusDays(7), Set.of(DayOfWeek.MONDAY), "09:00", "10:00", 30));
        check(slots.size() == 4 && slots.get(3).start().equals(MONDAY.plusDays(7).atTime(9, 30)), "weekly inclusive boundaries");
        int before = s.slots().size(), schedules = s.schedules().size();
        // New earlier slots would be created before a later overlap: all must roll back.
        reject("duplicateAvailability", () -> s.makeAvailability(weekly(p, MONDAY.minusDays(7), MONDAY, Set.of(DayOfWeek.MONDAY), "09:00", "10:00", 30)));
        check(s.slots().size() == before && s.schedules().size() == schedules, "recurrence collision has no partial slots/schedule");
        reject("invalidInput", () -> s.makeAvailability(weekly(p, MONDAY, MONDAY, Set.of(DayOfWeek.MONDAY), "12:00", "13:00", 45)));
        reject("invalidInput", () -> s.makeAvailability(weekly(p, MONDAY, MONDAY, Set.of(), "12:00", "13:00", 30)));
        reject("invalidInput", () -> s.makeAvailability(weekly(p, MONDAY.plusDays(1), MONDAY, Set.of(DayOfWeek.MONDAY), "12:00", "13:00", 30)));
        String p2 = professional(s, "Monthly").id();
        List<AvailabilitySlot> monthly = s.makeAvailability(new Schedule(p2, LocalDate.of(2030, 1, 31), LocalDate.of(2030, 3, 31),
                Recurrence.MONTHLY, Set.of(), 31, LocalTime.of(8, 0), LocalTime.of(9, 0), 60));
        check(monthly.size() == 2 && monthly.get(0).start().toLocalDate().equals(LocalDate.of(2030, 1, 31))
                && monthly.get(1).start().toLocalDate().equals(LocalDate.of(2030, 3, 31)), "monthly inclusive; nonexistent day skipped");
        reject("invalidInput", () -> s.makeAvailability(new Schedule(p2, MONDAY, MONDAY, Recurrence.MONTHLY, Set.of(), 0,
                LocalTime.of(8, 0), LocalTime.of(9, 0), 30)));
    }

    private static void bookingAndConflicts() {
        ClinicSystem s = new ClinicSystem(CLOCK);
        Patient patient = patient(s, "Patient", "A"), other = patient(s, "Other", "B");
        MedicalService half = s.registerService("Visit", 30, false), hour = s.registerService("Long", 60, false);
        String pro = professional(s, "First").id(), second = professional(s, "Second").id();
        List<AvailabilitySlot> a = s.makeAvailability(weekly(pro, MONDAY, MONDAY, Set.of(DayOfWeek.MONDAY), "09:00", "11:00", 30));
        List<AvailabilitySlot> b = s.makeAvailability(weekly(second, MONDAY, MONDAY, Set.of(DayOfWeek.MONDAY), "09:15", "10:15", 30));
        check(s.searchAvailability(half.id(), pro, MONDAY, MONDAY).size() == 4 && s.appointments().isEmpty(), "search advisory");
        check(s.searchAvailability(hour.id(), pro, MONDAY, MONDAY).isEmpty(), "service cannot straddle adjacent slots");
        reject("slotUnavailable", () -> s.requestAppointment(patient.id(), hour.id(), a.get(0).id(), RequestChannel.CLINIC, null));
        check(s.appointments().isEmpty(), "full interval fit checked on booking");
        Appointment booked = s.requestAppointment(patient.id(), half.id(), a.get(0).id(), RequestChannel.PHONE, null);
        check(booked.status().equals("requested") && booked.requestedBy().equals("staff") && booked.channel() == RequestChannel.PHONE
                && booked.location().equals(ClinicSystem.LOCATION), "requested staff phone booking");
        reject("slotUnavailable", () -> s.requestAppointment(other.id(), half.id(), a.get(0).id(), RequestChannel.CLINIC, null));
        // Professional-only search still lists this slot despite patient conflict.
        check(s.searchAvailability(half.id(), second, MONDAY, MONDAY).size() == 2, "search doesn't filter patient-specific conflict");
        reject("slotUnavailable", () -> s.requestAppointment(patient.id(), half.id(), b.get(0).id(), RequestChannel.CLINIC, null));
        check(s.appointments().size() == 1, "overlap rejection leaves no partial appointment");
        Appointment adjacent = s.requestAppointment(patient.id(), half.id(), a.get(1).id(), RequestChannel.CLINIC, null);
        check(booked.endTime().equals(adjacent.dateTime()), "touching endpoints allowed");
        check(s.searchAvailability(half.id(), pro, MONDAY, MONDAY).size() == 2, "requested status reserves");
        reject("notFound", () -> s.requestAppointment("bad", half.id(), a.get(2).id(), RequestChannel.CLINIC, null));
        reject("invalidInput", () -> s.requestAppointment(patient.id(), half.id(), a.get(2).id(), null, null));
        check(s.appointments().size() == 2, "bad IDs/channel no mutations");
        String third = professional(s, "Third").id();
        AvailabilitySlot large = s.makeAvailability(weekly(third, MONDAY, MONDAY, Set.of(DayOfWeek.MONDAY), "12:00", "13:00", 60)).get(0);
        s.requestAppointment(patient.id(), half.id(), large.id(), RequestChannel.CLINIC, null);
        MedicalService shortVisit = s.registerService("Short", 15, false);
        check(s.searchAvailability(shortVisit.id(), third, MONDAY, MONDAY).isEmpty(), "short service conservatively reserves entire slot");
    }

    private static void referrals() {
        ClinicSystem s = new ClinicSystem(CLOCK);
        Patient a = patient(s, "A", "1"), b = patient(s, "B", "2");
        String p = professional(s, "Doctor").id(), other = professional(s, "Other").id();
        MedicalService service = s.registerService("Specialist", 30, true), optional = s.registerService("Optional", 30, false);
        AvailabilitySlot slot = s.makeAvailability(weekly(p, MONDAY, MONDAY, Set.of(DayOfWeek.MONDAY), "09:00", "09:30", 30)).get(0);
        reject("referralRequired", () -> s.requestAppointment(a.id(), service.id(), slot.id(), RequestChannel.CLINIC, null));
        reject("referralInvalid", () -> s.requestAppointment(a.id(), optional.id(), slot.id(), RequestChannel.CLINIC, "unknown"));
        Referral wrongPatient = s.registerReferral(b.id(), p, null, true);
        Referral inactive = s.registerReferral(a.id(), p, null, false);
        Referral wrongProfessional = s.registerReferral(a.id(), other, "Family Medicine", true);
        Referral wrongSpecialty = s.registerReferral(a.id(), p, "Cardiology", true);
        for (Referral r : List.of(wrongPatient, inactive, wrongProfessional, wrongSpecialty))
            reject("referralInvalid", () -> s.requestAppointment(a.id(), service.id(), slot.id(), RequestChannel.CLINIC, r.id()));
        check(s.appointments().isEmpty(), "all referral failures atomic");
        Referral valid = s.registerReferral(a.id(), p, "family medicine", true);
        Appointment appointment = s.requestAppointment(a.id(), service.id(), slot.id(), RequestChannel.CLINIC, valid.id());
        check(appointment.referralId().equals(valid.id()) && s.referrals().contains(valid), "both referral targets match and referral unchanged");
    }

    private static void futureRevalidation() {
        MutableClock clock = new MutableClock(Instant.parse("2030-01-07T13:00:00Z")); // 08:00 Toronto
        ClinicSystem s = new ClinicSystem(clock);
        Patient patient = patient(s, "A", "A");
        String p = professional(s, "A").id();
        MedicalService service = s.registerService("Visit", 30, false);
        AvailabilitySlot slot = s.makeAvailability(weekly(p, MONDAY, MONDAY, Set.of(DayOfWeek.MONDAY), "09:00", "09:30", 30)).get(0);
        check(s.searchAvailability(service.id(), p, MONDAY, MONDAY).size() == 1, "future search uses injected clock");
        clock.instant = Instant.parse("2030-01-07T14:00:00Z"); // exactly 09:00
        reject("slotUnavailable", () -> s.requestAppointment(patient.id(), service.id(), slot.id(), RequestChannel.CLINIC, null));
        check(s.searchAvailability(service.id(), p, MONDAY, MONDAY).isEmpty() && s.appointments().isEmpty(), "start equal now rejected after earlier advisory search");
        clock.instant = Instant.parse("2030-01-07T14:01:00Z");
        reject("slotUnavailable", () -> s.requestAppointment(patient.id(), service.id(), slot.id(), RequestChannel.CLINIC, null));
    }

    private static void csv(Path root) throws Exception {
        ClinicSystem s = new ClinicSystem(CLOCK);
        ImportResult result = s.importAvailability(root.resolve("samples/availability.csv"));
        check(result.equals(new ImportResult(5, 8)), "supplied eight rows dedupe five professionals");
        check(s.professionals().get(0).firstName().equals("Amélie"), "UTF-8 accents preserved");
        check(s.patients().isEmpty() && s.services().isEmpty(), "import doesn't manufacture catalog beyond professionals/slots");
        reject("duplicateAvailability", () -> s.importAvailability(root.resolve("samples/availability.csv")));
        check(s.professionals().size() == 5 && s.slots().size() == 8, "duplicate file atomic");
        Path exported = temporary.resolve("roundtrip.csv");
        s.exportAvailability(exported);
        check(Files.readString(exported).contains("Amélie"), "export UTF-8");
        ClinicSystem copy = new ClinicSystem(CLOCK);
        check(copy.importAvailability(exported).equals(result), "CSV roundtrip");
        check(copy.slots().equals(s.slots()) && copy.professionals().equals(s.professionals()), "roundtrip retains availability values");
        Path malformed = csvFile("malformed.csv", HEADER + "New,Person,Dr.,Test,2030-01-07,09:00,10:00\nBad,Date,Dr.,Test,not-date,09:00,10:00\n");
        reject("invalidCsv", () -> s.importAvailability(malformed));
        check(s.professionals().size() == 5 && s.slots().size() == 8, "malformed later row has no partial imports");
        Path duplicate = csvFile("duplicate.csv", HEADER + "New,Person,Dr.,Test,2030-01-07,09:00,10:00\nAmélie,Tremblay,Dr.,Family Medicine,2026-11-02,09:15,09:45\n");
        reject("duplicateAvailability", () -> s.importAvailability(duplicate));
        check(s.professionals().size() == 5 && s.slots().size() == 8, "overlapping later row rolls back new professional and slot");
        Path internal = csvFile("internal.csv", HEADER + "New,Person,Dr.,Test,2030-01-07,09:00,10:00\nnew,person,dr.,test,2030-01-07,09:00,10:00\n");
        reject("duplicateAvailability", () -> s.importAvailability(internal));
        check(s.professionals().size() == 5, "within-file duplicate rolls back");
        for (String bad : List.of("bad header\n", HEADER + "A,B,Dr.,Test,2030-01-07,10:00,09:00\n",
                HEADER + "A,B,Dr.,Test,2030-01-07,09:00,10:00,extra\n", HEADER + "\"unterminated",
                HEADER + "A,B,Dr.,Test,2030-01-07,09:00:01,10:00\n", HEADER + "\n")) {
            Path badFile = csvFile("bad.csv", bad);
            reject("invalidCsv", () -> s.importAvailability(badFile));
        }
        ClinicSystem quoted = new ClinicSystem(CLOCK);
        Path quotes = csvFile("quoted.csv", "\uFEFF" + HEADER.replace("\n", "\r\n") + "\"Éloïse, Anne\",\"O\"\"Neil\",Dr.,\"Family\nMedicine\",2030-01-07,09:00,10:00\r\n");
        quoted.importAvailability(quotes);
        quoted.exportAvailability(exported);
        ClinicSystem quotedCopy = new ClinicSystem(CLOCK); quotedCopy.importAvailability(exported);
        check(quoted.professionals().equals(quotedCopy.professionals()), "BOM, CRLF, escaped quotes, comma and newline roundtrip");
        ClinicSystem booked = new ClinicSystem(CLOCK);
        Path future = csvFile("future.csv", HEADER + "Amélie,Tremblay,Dr.,Family Medicine,2030-01-07,09:00,09:30\n");
        booked.importAvailability(future);
        Patient p = patient(booked, "A", "A");
        MedicalService service = booked.registerService("Visit", 30, false);
        Appointment a = booked.requestAppointment(p.id(), service.id(), "S1", RequestChannel.PHONE, null);
        reject("duplicateAvailability", () -> booked.importAvailability(future));
        check(booked.appointments().equals(List.of(a)) && booked.searchAvailability(service.id(), "PRO1", MONDAY, MONDAY).isEmpty(), "import cannot reset booked slot");
        booked.exportAvailability(exported);
        ClinicSystem all = new ClinicSystem(CLOCK); all.importAvailability(exported);
        check(all.slots().size() == 1 && all.appointments().isEmpty(), "export includes reserved offerings, not reservations");
        Path empty = temporary.resolve("empty.csv"); new ClinicSystem(CLOCK).exportAvailability(empty);
        check(Files.readString(empty).equals(HEADER), "empty dataset exports header only");
    }

    private static void concurrency() throws Exception {
        ClinicSystem s = new ClinicSystem(CLOCK);
        Patient a = patient(s, "A", "A"), b = patient(s, "B", "B");
        String p = professional(s, "A").id(); MedicalService service = s.registerService("Visit", 30, false);
        String slot = s.makeAvailability(weekly(p, MONDAY, MONDAY, Set.of(DayOfWeek.MONDAY), "09:00", "09:30", 30)).get(0).id();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch gate = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (Patient patient : List.of(a, b)) futures.add(pool.submit(() -> {
                gate.await();
                try { s.requestAppointment(patient.id(), service.id(), slot, RequestChannel.CLINIC, null); return true; }
                catch (ClinicException e) { if (!e.code().equals("slotUnavailable")) throw e; return false; }
            }));
            gate.countDown();
            int successes = 0;
            for (Future<Boolean> future : futures) if (future.get(5, TimeUnit.SECONDS)) successes++;
            check(successes == 1 && s.appointments().size() == 1, "simultaneous requests cannot both reserve");
        } finally { pool.shutdownNow(); }
    }

    private static void cli() throws Exception {
        String eof = runCli(""); check(eof.contains("End of input. Goodbye."), "menu EOF exits normally");
        String partial = runCli("1\nPartial\n"); check(partial.contains("End of input. Goodbye."), "mid-command EOF exits normally");
        String invalid = runCli("bogus\n2\nVisit\nnot-a-number\n1\nAlex\nExample\nnot-a-date\n0\n");
        check(invalid.contains("Invalid input:") && invalid.contains("Goodbye. In-memory data discarded."), "invalid command/number/date recovers");
        String walkthrough = runCli("5\n" + temporary.resolve("future.csv") + "\n1\nAlex\nExample\n1990-01-01\n555-0100\nDEMO1\n2\nVisit\n30\nn\n7\nAlex\n8\nM1\nPRO1\n2030-01-07\n2030-01-07\n9\nP1\nM1\nS1\nPHONE\n\n10\n0\n");
        // The CLI uses the real clock, so exercise successful booking with a far-future CSV instead below.
        check(walkthrough.contains("Patient[id=P1") && walkthrough.contains("MedicalService[id=M1"), "CLI registration walkthrough");
        Path farFuture = csvFile("far-future.csv", HEADER + "Amélie,Tremblay,Dr.,Family Medicine,2099-01-05,09:00,09:30\n");
        String booked = runCli("5\n" + farFuture + "\n1\nAlex\nExample\n1990-01-01\n555-0100\nDEMO1\n2\nVisit\n30\nn\n8\nM1\nPRO1\n2099-01-05\n2099-01-05\n9\nP1\nM1\nS1\nPHONE\n\n0\n");
        check(booked.contains("status=requested, requestedBy=staff, channel=PHONE"), "CLI end-to-end import/search/phone booking");
    }

    private static String runCli(String input) throws Exception {
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Dfile.encoding=UTF-8", "-cp", System.getProperty("java.class.path"), "clinic.Main").redirectErrorStream(true).start();
        try (OutputStream out = process.getOutputStream()) { out.write(input.getBytes(StandardCharsets.UTF_8)); }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        check(process.waitFor() == 0, "CLI process exit status");
        return output;
    }

    private static Patient patient(ClinicSystem s, String first, String insurance) {
        return s.registerPatient(first, "Example", LocalDate.of(1990, 1, 1), "555-0100", insurance);
    }
    private static ClinicProfessional professional(ClinicSystem s, String first) {
        return s.registerProfessional(first, "Example", "Dr.", "Family Medicine");
    }
    private static Schedule weekly(String professional, LocalDate from, LocalDate to, Set<DayOfWeek> days, String start, String end, int minutes) {
        return new Schedule(professional, from, to, Recurrence.WEEKLY, days, null, LocalTime.parse(start), LocalTime.parse(end), minutes);
    }
    private static Path csvFile(String name, String content) throws IOException {
        Path path = temporary.resolve(name); Files.writeString(path, content, StandardCharsets.UTF_8); return path;
    }
    private static void check(boolean condition, String message) {
        checks++; if (!condition) throw new AssertionError(message);
    }
    private static void reject(String code, Throwing action) {
        checks++;
        try { action.run(); throw new AssertionError("Expected rejection " + code); }
        catch (ClinicException e) { if (!e.code().equals(code)) throw new AssertionError("Expected " + code + " got " + e.code(), e); }
        catch (Exception e) { throw new AssertionError("Unexpected checked exception", e); }
    }
    @FunctionalInterface private interface Throwing { void run() throws Exception; }
    private static final class MutableClock extends Clock {
        private Instant instant;
        MutableClock(Instant instant) { this.instant = instant; }
        @Override public ZoneId getZone() { return ClinicSystem.CLINIC_ZONE; }
        @Override public Clock withZone(ZoneId zone) {
            if (!zone.equals(getZone())) throw new IllegalArgumentException("Test clock uses clinic zone");
            return this;
        }
        @Override public Instant instant() { return instant; }
    }
}
