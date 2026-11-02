package dev.ledger.domain;

/**
 * The double-entry invariant, in one place and unit-testable without a
 * database: a postable entry has at least two lines, every line is a
 * debit XOR a credit, all accounts share one currency, and total debits
 * equal total credits. This is the only way to construct a
 * {@link JournalEntry}.
 */
public final class PostingRules {

    private PostingRules() {
    }

    public static JournalEntry validateAndCreate(java.time.LocalDate valueDate, String description,
                                                 String reference, String idempotencyKey,
                                                 String payloadHash,
                                                 java.util.List<JournalLine> lines) {
        if (lines == null || lines.size() < 2) {
            throw new UnbalancedEntryException("A journal entry needs at least two lines");
        }
        long currencies = lines.stream()
                .map(line -> line.getAccount().getCurrency())
                .distinct()
                .count();
        if (currencies > 1) {
            throw new UnbalancedEntryException(
                    "All lines of one entry must share a single currency");
        }
        long debits = lines.stream().mapToLong(JournalLine::getDebitMinor).sum();
        long credits = lines.stream().mapToLong(JournalLine::getCreditMinor).sum();
        if (debits == 0 || debits != credits) {
            throw new UnbalancedEntryException(
                    "Entry is not balanced: total debits " + debits + " != total credits " + credits);
        }
        return new JournalEntry(valueDate, description, reference, idempotencyKey, payloadHash, lines);
    }
}
