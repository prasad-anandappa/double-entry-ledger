package dev.ledger.reconciliation;

import java.time.LocalDate;

/**
 * One line of a bank statement, expressed in the ledger's conventions:
 * the amount is signed in the account's normal-balance direction
 * (a credit to a revenue account is positive; a fee reducing a cash
 * asset is negative).
 */
public record StatementLine(LocalDate valueDate,
                            String accountCode,
                            long amountMinor,
                            String reference,
                            String description) {

    public String normalisedReference() {
        return reference == null ? "" : reference.trim();
    }
}
