package dev.ledger.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostingRulesTest {

    private final Account cash = new Account("CASH-100", "Cash", AccountType.ASSET, "USD");
    private final Account revenueUsd = new Account("REV-400", "Revenue", AccountType.REVENUE, "USD");
    private final Account revenueEur = new Account("REV-401", "Revenue EUR", AccountType.REVENUE, "EUR");

    @Test
    void balancedEntryIsCreatedWithMatchingTotals() {
        JournalEntry entry = PostingRules.validateAndCreate(
                LocalDate.of(2026, 10, 1), "Invoice 001", "INV-001", null, "hash",
                List.of(JournalLine.debit(cash, 100_000), JournalLine.credit(revenueUsd, 100_000)));

        assertThat(entry.totalDebitsMinor()).isEqualTo(100_000);
        assertThat(entry.totalCreditsMinor()).isEqualTo(100_000);
        assertThat(entry.getLines()).hasSize(2);
    }

    @Test
    void unbalancedEntryIsRejected() {
        assertThatThrownBy(() -> PostingRules.validateAndCreate(
                LocalDate.of(2026, 10, 1), "Bad", null, null, "hash",
                List.of(JournalLine.debit(cash, 100_000), JournalLine.credit(revenueUsd, 99_999))))
                .isInstanceOf(UnbalancedEntryException.class)
                .hasMessageContaining("not balanced");
    }

    @Test
    void singleLineEntryIsRejected() {
        assertThatThrownBy(() -> PostingRules.validateAndCreate(
                LocalDate.of(2026, 10, 1), "Bad", null, null, "hash",
                List.of(JournalLine.debit(cash, 100_000))))
                .isInstanceOf(UnbalancedEntryException.class);
    }

    @Test
    void mixedCurrenciesAreRejected() {
        assertThatThrownBy(() -> PostingRules.validateAndCreate(
                LocalDate.of(2026, 10, 1), "Bad", null, null, "hash",
                List.of(JournalLine.debit(cash, 100_000), JournalLine.credit(revenueEur, 100_000))))
                .isInstanceOf(UnbalancedEntryException.class)
                .hasMessageContaining("single currency");
    }

    @Test
    void lineWithBothSidesIsRejected() {
        assertThatThrownBy(() -> new JournalLine(cash, 100, 100))
                .isInstanceOf(InvalidLineException.class);
    }

    @Test
    void emptyLineIsRejected() {
        assertThatThrownBy(() -> new JournalLine(cash, 0, 0))
                .isInstanceOf(InvalidLineException.class);
    }

    @Test
    void negativeLineIsRejected() {
        assertThatThrownBy(() -> new JournalLine(cash, -5, 0))
                .isInstanceOf(InvalidLineException.class);
    }
}
