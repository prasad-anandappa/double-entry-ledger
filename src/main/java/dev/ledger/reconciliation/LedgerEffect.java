package dev.ledger.reconciliation;

import java.time.LocalDate;

/**
 * The ledger-side counterpart of a {@link StatementLine}: the net effect
 * of one journal entry on one account, in normal-balance terms.
 */
public record LedgerEffect(Long entryId,
                           LocalDate valueDate,
                           String accountCode,
                           long amountMinor,
                           String reference,
                           String description) {

    public String normalisedReference() {
        return reference == null ? "" : reference.trim();
    }
}
