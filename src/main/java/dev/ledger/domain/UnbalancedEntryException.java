package dev.ledger.domain;

/** Total debits do not equal total credits. Mapped to HTTP 422. */
public class UnbalancedEntryException extends RuntimeException {
    public UnbalancedEntryException(String message) {
        super(message);
    }
}
