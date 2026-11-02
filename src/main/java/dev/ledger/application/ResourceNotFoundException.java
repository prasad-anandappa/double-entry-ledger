package dev.ledger.application;

/** Base type for "the thing you asked for does not exist" — HTTP 404. */
public abstract class ResourceNotFoundException extends RuntimeException {
    protected ResourceNotFoundException(String message) {
        super(message);
    }
}
