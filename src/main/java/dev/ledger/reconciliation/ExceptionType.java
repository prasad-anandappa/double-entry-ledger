package dev.ledger.reconciliation;

/** How one reconciliation exception differs between statement and ledger. */
public enum ExceptionType {
    /** Present on the bank statement, absent from the ledger. */
    MISSING_IN_LEDGER,
    /** Present in the ledger, absent from the bank statement. */
    MISSING_ON_STATEMENT,
    /** Same account, date and reference on both sides, different amount. */
    AMOUNT_MISMATCH
}
