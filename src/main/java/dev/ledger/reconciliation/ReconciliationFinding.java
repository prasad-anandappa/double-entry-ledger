package dev.ledger.reconciliation;

import java.time.LocalDate;

/** One unmatched or mismatched item found by a reconciliation run. */
public record ReconciliationFinding(ExceptionType type,
                                    String accountCode,
                                    LocalDate valueDate,
                                    long amountMinor,
                                    String reference,
                                    String description) {

    static ReconciliationFinding fromStatement(ExceptionType type, StatementLine line) {
        return new ReconciliationFinding(type, line.accountCode(), line.valueDate(),
                line.amountMinor(), line.reference(), line.description());
    }

    static ReconciliationFinding fromLedger(LedgerEffect effect) {
        return new ReconciliationFinding(ExceptionType.MISSING_ON_STATEMENT, effect.accountCode(),
                effect.valueDate(), effect.amountMinor(), effect.reference(), effect.description());
    }
}
