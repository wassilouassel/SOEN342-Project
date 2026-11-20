package clinic;

import clinic.Domain.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import static clinic.Domain.text;

/** Explicit UTF-8 interchange of offerings, not persistence of reservations or patient data. */
final class CsvAvailabilityStore {
    static final List<String> HEADER = List.of("First Name", "Last Name", "Title", "Specialty", "Date", "Start", "End");

    List<CsvRow> read(Path path) throws IOException {
        String source = Files.readString(path, StandardCharsets.UTF_8);
        if (source.startsWith("\uFEFF")) source = source.substring(1);
        List<List<String>> records = parse(source);
        if (records.isEmpty() || !records.get(0).equals(HEADER))
            throw new ClinicException("invalidCsv", "Expected exact header: " + String.join(",", HEADER));
        List<CsvRow> rows = new ArrayList<>();
        for (int i = 1; i < records.size(); i++) {
            List<String> r = records.get(i);
            try {
                if (r.size() != 7) throw new IllegalArgumentException("expected seven fields");
                String first = text(r.get(0), "First Name"), last = text(r.get(1), "Last Name");
                String title = text(r.get(2), "Title"), specialty = text(r.get(3), "Specialty");
                LocalDate date = LocalDate.parse(r.get(4));
                LocalTime start = LocalTime.parse(r.get(5)), end = LocalTime.parse(r.get(6));
                if (!start.isBefore(end) || start.getSecond() != 0 || start.getNano() != 0
                        || end.getSecond() != 0 || end.getNano() != 0)
                    throw new IllegalArgumentException("expected minute-precision start before end on the same day");
                rows.add(new CsvRow(first, last, title, specialty, date, start, end));
            } catch (RuntimeException e) {
                throw new ClinicException("invalidCsv", "CSV record " + (i + 1) + ": " + e.getMessage());
            }
        }
        return List.copyOf(rows);
    }

    void write(Path path, List<AvailabilitySlot> slots, List<ClinicProfessional> professionals) throws IOException {
        Map<String, ClinicProfessional> byId = professionals.stream().collect(Collectors.toMap(ClinicProfessional::id, p -> p));
        StringBuilder out = new StringBuilder(String.join(",", HEADER)).append('\n');
        for (AvailabilitySlot slot : slots) {
            ClinicProfessional p = byId.get(slot.professionalId());
            List<String> row = List.of(p.firstName(), p.lastName(), p.title(), p.specialty(),
                    slot.start().toLocalDate().toString(), slot.start().toLocalTime().toString(), slot.end().toLocalTime().toString());
            out.append(row.stream().map(CsvAvailabilityStore::quote).collect(Collectors.joining(","))).append('\n');
        }
        Path target = path.toAbsolutePath();
        Path temporary = Files.createTempFile(target.getParent(), ".availability-", ".csv");
        try {
            Files.writeString(temporary, out, StandardCharsets.UTF_8);
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    private static String quote(String value) {
        return value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0
                ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }

    /** Strict CSV with escaped quotes, quoted commas/newlines, LF/CRLF, and optional final newline. */
    private static List<List<String>> parse(String source) {
        List<List<String>> records = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false, closed = false, started = false;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < source.length() && source.charAt(i + 1) == '"') { field.append('"'); i++; }
                    else { quoted = false; closed = true; }
                } else field.append(c);
                continue;
            }
            if (c == ',' || c == '\r' || c == '\n') {
                row.add(field.toString()); field.setLength(0); closed = false; started = false;
                if (c != ',') {
                    if (c == '\r' && i + 1 < source.length() && source.charAt(i + 1) == '\n') i++;
                    records.add(List.copyOf(row)); row.clear();
                }
            } else if (c == '"' && !started && !closed) {
                quoted = true; started = true;
            } else {
                if (closed || c == '"') throw new ClinicException("invalidCsv", "Malformed quoting near CSV record " + (records.size() + 1));
                field.append(c); started = true;
            }
        }
        if (quoted) throw new ClinicException("invalidCsv", "Unterminated quoted field at CSV record " + (records.size() + 1));
        if (started || closed || !row.isEmpty()) { row.add(field.toString()); records.add(List.copyOf(row)); }
        return records;
    }
}
