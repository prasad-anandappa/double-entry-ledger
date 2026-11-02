package dev.ledger.domain;

/** A journal line violates the debit-XOR-credit shape. Mapped to HTTP 422. */
public class InvalidLineException extends RuntimeException {
    public InvalidLineException(String message) {
        super(message);
    }
}
