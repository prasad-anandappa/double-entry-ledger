package dev.ledger.reconciliation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Deterministic two-pass matcher between bank-statement lines and ledger
 * effects.
 *
 * <ol>
 *   <li><b>Exact pass</b> — same account, value date, amount and reference
 *       (blank reference counts as a value) pairs off as MATCHED.</li>
 *   <li><b>Mismatch pass</b> — among the leftovers, same account, value
 *       date and non-blank reference but a different amount pairs off as
 *       AMOUNT_MISMATCH, so a keyed-wrong amount is reported once instead
 *       of as two "missing" items.</li>
 *   <li>Whatever remains is MISSING_IN_LEDGER (statement side) or
 *       MISSING_ON_STATEMENT (ledger side).</li>
 * </ol>
 *
 * Inputs are sorted first so results are stable run-to-run.
 */
public final class ReconciliationMatcher {

    public record Result(int matched, List<ReconciliationFinding> findings) {
    }

    private static final Comparator<StatementLine> STATEMENT_ORDER =
            Comparator.comparing(StatementLine::accountCode)
                    .thenComparing(StatementLine::valueDate)
                    .thenComparingLong(StatementLine::amountMinor)
                    .thenComparing(StatementLine::normalisedReference);

    private static final Comparator<LedgerEffect> LEDGER_ORDER =
            Comparator.comparing(LedgerEffect::accountCode)
                    .thenComparing(LedgerEffect::valueDate)
                    .thenComparingLong(LedgerEffect::amountMinor)
                    .thenComparing(LedgerEffect::normalisedReference);

    private ReconciliationMatcher() {
    }

    public static Result match(List<StatementLine> statement, List<LedgerEffect> ledger) {
        List<StatementLine> openStatement = new ArrayList<>(statement);
        List<LedgerEffect> openLedger = new ArrayList<>(ledger);
        openStatement.sort(STATEMENT_ORDER);
        openLedger.sort(LEDGER_ORDER);

        int matched = 0;
        List<ReconciliationFinding> findings = new ArrayList<>();

        // Pass 1: exact matches.
        for (var sit = openStatement.iterator(); sit.hasNext(); ) {
            StatementLine s = sit.next();
            LedgerEffect hit = findExact(openLedger, s);
            if (hit != null) {
                openLedger.remove(hit);
                sit.remove();
                matched++;
            }
        }

        // Pass 2: same account/date/reference, different amount.
        for (var sit = openStatement.iterator(); sit.hasNext(); ) {
            StatementLine s = sit.next();
            if (s.normalisedReference().isEmpty()) {
                continue;
            }
            LedgerEffect hit = findByReference(openLedger, s);
            if (hit != null) {
                openLedger.remove(hit);
                sit.remove();
                findings.add(new ReconciliationFinding(ExceptionType.AMOUNT_MISMATCH,
                        s.accountCode(), s.valueDate(), s.amountMinor(), s.reference(),
                        "Statement amount " + s.amountMinor() + " vs ledger amount "
                                + hit.amountMinor() + " for reference " + s.normalisedReference()));
            }
        }

        // Leftovers.
        openStatement.forEach(s -> findings.add(
                ReconciliationFinding.fromStatement(ExceptionType.MISSING_IN_LEDGER, s)));
        openLedger.forEach(e -> findings.add(ReconciliationFinding.fromLedger(e)));

        findings.sort(Comparator.comparing(ReconciliationFinding::accountCode)
                .thenComparing(ReconciliationFinding::valueDate)
                .thenComparing(f -> f.type().name()));
        return new Result(matched, List.copyOf(findings));
    }

    private static LedgerEffect findExact(List<LedgerEffect> open, StatementLine s) {
        for (LedgerEffect e : open) {
            if (e.accountCode().equals(s.accountCode())
                    && e.valueDate().equals(s.valueDate())
                    && e.amountMinor() == s.amountMinor()
                    && e.normalisedReference().equals(s.normalisedReference())) {
                return e;
            }
        }
        return null;
    }

    private static LedgerEffect findByReference(List<LedgerEffect> open, StatementLine s) {
        for (LedgerEffect e : open) {
            if (e.accountCode().equals(s.accountCode())
                    && e.valueDate().equals(s.valueDate())
                    && !e.normalisedReference().isEmpty()
                    && e.normalisedReference().equals(s.normalisedReference())) {
                return e;
            }
        }
        return null;
    }
}
