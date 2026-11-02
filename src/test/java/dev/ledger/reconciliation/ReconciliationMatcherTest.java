package dev.ledger.reconciliation;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationMatcherTest {

    private static final LocalDate D1 = LocalDate.of(2026, 10, 1);

    private StatementLine stmt(String account, long amount, String ref) {
        return new StatementLine(D1, account, amount, ref, "statement line");
    }

    private LedgerEffect ledger(String account, long amount, String ref) {
        return new LedgerEffect(1L, D1, account, amount, ref, "ledger entry");
    }

    @Test
    void exactLinesMatch() {
        var result = ReconciliationMatcher.match(
                List.of(stmt("CASH-100", 100_000, "INV-001")),
                List.of(ledger("CASH-100", 100_000, "INV-001")));

        assertThat(result.matched()).isEqualTo(1);
        assertThat(result.findings()).isEmpty();
    }

    @Test
    void blankReferencesStillMatchExactly() {
        var result = ReconciliationMatcher.match(
                List.of(stmt("CASH-100", 5_000, null)),
                List.of(ledger("CASH-100", 5_000, null)));

        assertThat(result.matched()).isEqualTo(1);
        assertThat(result.findings()).isEmpty();
    }

    @Test
    void statementOnlyLineIsMissingInLedger() {
        var result = ReconciliationMatcher.match(
                List.of(stmt("CASH-100", -1_500, "FEE-0009")),
                List.of());

        assertThat(result.matched()).isZero();
        assertThat(result.findings()).hasSize(1);
        assertThat(result.findings().get(0).type()).isEqualTo(ExceptionType.MISSING_IN_LEDGER);
        assertThat(result.findings().get(0).amountMinor()).isEqualTo(-1_500);
    }

    @Test
    void ledgerOnlyEffectIsMissingOnStatement() {
        var result = ReconciliationMatcher.match(
                List.of(),
                List.of(ledger("CASH-100", 7_500, "DEP-1")));

        assertThat(result.matched()).isZero();
        assertThat(result.findings()).hasSize(1);
        assertThat(result.findings().get(0).type()).isEqualTo(ExceptionType.MISSING_ON_STATEMENT);
    }

    @Test
    void sameReferenceDifferentAmountIsOneMismatchNotTwoMissing() {
        var result = ReconciliationMatcher.match(
                List.of(stmt("CASH-100", 100_000, "INV-002")),
                List.of(ledger("CASH-100", 99_000, "INV-002")));

        assertThat(result.matched()).isZero();
        assertThat(result.findings()).hasSize(1);
        assertThat(result.findings().get(0).type()).isEqualTo(ExceptionType.AMOUNT_MISMATCH);
    }

    @Test
    void duplicatesPairOffOneToOne() {
        var result = ReconciliationMatcher.match(
                List.of(stmt("CASH-100", 2_000, null), stmt("CASH-100", 2_000, null)),
                List.of(ledger("CASH-100", 2_000, null)));

        assertThat(result.matched()).isEqualTo(1);
        assertThat(result.findings()).hasSize(1);
        assertThat(result.findings().get(0).type()).isEqualTo(ExceptionType.MISSING_IN_LEDGER);
    }
}
