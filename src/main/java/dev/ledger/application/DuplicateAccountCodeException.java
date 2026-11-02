package dev.ledger.application;

/** An account with this code already exists. Mapped to HTTP 409. */
public class DuplicateAccountCodeException extends RuntimeException {
    public DuplicateAccountCodeException(String code) {
        super("Account code already exists: " + code);
    }
}
