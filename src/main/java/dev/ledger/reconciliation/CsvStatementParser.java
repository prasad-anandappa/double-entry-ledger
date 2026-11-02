package dev.ledger.reconciliation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for the statement CSV format:
 *
 * <pre>value_date,account_code,amount_minor,reference,description</pre>
 *
 * Amounts are signed minor units in the account's normal-balance
 * direction. Supports RFC-4180-style quoted fields (embedded commas,
 * doubled quotes). A header row is required and validated.
 */
public final class CsvStatementParser {

    private static final List<String> EXPECTED_HEADER =
            List.of("value_date", "account_code", "amount_minor", "reference", "description");

    private CsvStatementParser() {
    }

    public static List<StatementLine> parse(Reader reader) throws IOException {
        List<StatementLine> lines = new ArrayList<>();
        boolean headerSeen = false;
        try (BufferedReader in = new BufferedReader(reader)) {
            String raw;
            int rowNumber = 0;
            while ((raw = in.readLine()) != null) {
                rowNumber++;
                if (raw.isBlank()) {
                    continue;
                }
                List<String> fields = splitRow(raw);
                if (!headerSeen) {
                    List<String> header = fields.stream().map(f -> f.trim().toLowerCase()).toList();
                    if (!header.equals(EXPECTED_HEADER)) {
                        throw new IllegalArgumentException(
                                "Statement CSV header must be " + String.join(",", EXPECTED_HEADER));
                    }
                    headerSeen = true;
                    continue;
                }
                if (fields.size() != EXPECTED_HEADER.size()) {
                    throw new IllegalArgumentException(
                            "Row " + rowNumber + ": expected 5 fields, found " + fields.size());
                }
                try {
                    lines.add(new StatementLine(
                            LocalDate.parse(fields.get(0).trim()),
                            fields.get(1).trim(),
                            Long.parseLong(fields.get(2).trim()),
                            blankToNull(fields.get(3)),
                            blankToNull(fields.get(4))));
                } catch (RuntimeException e) {
                    throw new IllegalArgumentException("Row " + rowNumber + ": " + e.getMessage(), e);
                }
            }
        }
        if (!headerSeen) {
            throw new IllegalArgumentException("Statement CSV is empty or missing its header row");
        }
        return lines;
    }

    /** Splits one CSV row honouring double-quoted fields. */
    static List<String> splitRow(String row) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < row.length(); i++) {
            char c = row.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < row.length() && row.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    private static String blankToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
