package clinic;

import clinic.Domain.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.*;
import java.util.*;

/** Terminal adapter. EOF at any prompt exits without committing a partial command. */
public final class Main {
    private final ClinicSystem system = new ClinicSystem();
    private final BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    public static void main(String[] args) throws IOException { new Main().run(); }

    private void run() throws IOException {
        System.out.println("Appointment tracking — in-memory Java PoC (not production authentication/audit)");
        System.out.println("Starts empty. Single Main clinic; times in America/Toronto. Search is advisory.");
        try {
            while (true) {
                System.out.println("\n1 Register patient   2 Register service   3 Register professional");
                System.out.println("4 Make availability  5 Import CSV         6 Export all availability CSV");
                System.out.println("7 Search patient     8 Search availability 9 Request appointment");
                System.out.println("10 Show state        11 Register demo referral   0 Exit");
                String choice = ask("Choice");
                try {
                    switch (choice) {
                        case "0" -> { System.out.println("Goodbye. In-memory data discarded."); return; }
                        case "1" -> registerPatient();
                        case "2" -> System.out.println(system.registerService(ask("Service name"), integer("Duration minutes"), yesNo("Requires referral (y/n)")));
                        case "3" -> System.out.println(system.registerProfessional(ask("First name"), ask("Last name"), ask("Title"), ask("Specialty")));
                        case "4" -> makeAvailability();
                        case "5" -> System.out.println(system.importAvailability(Path.of(ask("Import CSV path"))));
                        case "6" -> { system.exportAvailability(Path.of(ask("Export CSV path (existing file replaced)"))); System.out.println("Exported every slot, including reserved and past slots; bookings are NOT exported."); }
                        case "7" -> print(system.searchPatient(ask("Name fragment or exact patient ID, insurance number, phone")));
                        case "8" -> searchAvailability();
                        case "9" -> requestAppointment();
                        case "10" -> showState();
                        case "11" -> System.out.println(system.registerReferral(ask("Patient ID"), ask("Target professional ID (blank for specialty only)"),
                                ask("Target specialty (blank for professional only)"), yesNo("Active (y/n)")));
                        default -> System.out.println("Choose a listed number.");
                    }
                } catch (ClinicException e) {
                    System.out.println("Rejected [" + e.code() + "]: " + e.getMessage());
                } catch (IllegalArgumentException | DateTimeException e) {
                    System.out.println("Invalid input: " + e.getMessage() + ". No domain changes applied.");
                } catch (IOException e) {
                    if (e instanceof EOFException) throw e;
                    System.out.println("File/input error: " + e.getMessage());
                }
            }
        } catch (EOFException e) { System.out.println("\nEnd of input. Goodbye. In-memory data discarded."); }
    }

    private void registerPatient() throws IOException {
        String first = ask("First name"), last = ask("Last name");
        LocalDate dob = date("Date of birth (YYYY-MM-DD)");
        System.out.println(system.registerPatient(first, last, dob, ask("Phone"), ask("Insurance/RAMQ number (blank if none)")));
    }

    private void makeAvailability() throws IOException {
        print(system.professionals());
        String professional = ask("Professional ID");
        LocalDate from = date("From date inclusive (YYYY-MM-DD)"), to = date("To date inclusive (YYYY-MM-DD)");
        Recurrence recurrence = Recurrence.valueOf(ask("Recurrence WEEKLY or MONTHLY").toUpperCase(Locale.ROOT));
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        Integer dayOfMonth = null;
        if (recurrence == Recurrence.WEEKLY) {
            for (String day : ask("Weekdays, comma-separated (MONDAY,TUESDAY,...)").split(",", -1))
                days.add(DayOfWeek.valueOf(day.strip().toUpperCase(Locale.ROOT)));
        } else dayOfMonth = integer("Day of month 1–31 (missing days skipped, never clamped)");
        LocalTime start = LocalTime.parse(ask("Start time (HH:MM)")), end = LocalTime.parse(ask("End time (HH:MM)"));
        List<AvailabilitySlot> result = system.makeAvailability(new Schedule(professional, from, to, recurrence, days,
                dayOfMonth, start, end, integer("Slot duration minutes (must divide time window)")));
        System.out.println("Created " + result.size() + " slots atomically."); print(result);
    }

    private void searchAvailability() throws IOException {
        print(system.services()); print(system.professionals());
        print(system.searchAvailability(ask("Service ID"), ask("Professional ID"),
                date("From date inclusive (YYYY-MM-DD)"), date("To date inclusive (YYYY-MM-DD)")));
        System.out.println("Advisory only; patient overlap/referral checked when booking. Entire selected slot is reserved.");
    }

    private void requestAppointment() throws IOException {
        String patient = ask("Patient ID"), service = ask("Service ID"), slot = ask("Slot ID from availability search");
        RequestChannel channel = RequestChannel.valueOf(ask("Request received at CLINIC or by PHONE").toUpperCase(Locale.ROOT));
        Appointment result = system.requestAppointment(patient, service, slot, channel, ask("Referral ID (blank if none)"));
        System.out.println(result);
        System.out.println("Requested by staff, NOT confirmed. Whole slot reserved; no automatic confirmation.");
    }

    private void showState() {
        System.out.println("Patients:"); print(system.patients());
        System.out.println("Services:"); print(system.services());
        System.out.println("Professionals (all practise at " + ClinicSystem.LOCATION + "):"); print(system.professionals());
        System.out.println("All availability (not necessarily bookable):"); print(system.slots());
        System.out.println("Appointments:"); print(system.appointments());
        System.out.println("Demo referrals:"); print(system.referrals());
    }

    private String ask(String prompt) throws IOException {
        System.out.print(prompt + ": "); System.out.flush();
        String value = input.readLine();
        if (value == null) throw new EOFException();
        return value.strip();
    }
    private int integer(String prompt) throws IOException { return Integer.parseInt(ask(prompt)); }
    private LocalDate date(String prompt) throws IOException { return LocalDate.parse(ask(prompt)); }
    private boolean yesNo(String prompt) throws IOException {
        return switch (ask(prompt).toLowerCase(Locale.ROOT)) {
            case "y", "yes" -> true;
            case "n", "no" -> false;
            default -> throw new IllegalArgumentException("Expected y or n");
        };
    }
    private void print(List<?> values) {
        if (values.isEmpty()) System.out.println("(none)");
        else values.forEach(System.out::println);
    }
}
