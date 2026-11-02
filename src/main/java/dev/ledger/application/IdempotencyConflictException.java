package dev.ledger.application;

/**
 * An Idempotency-Key was reused with a different payload than the
 * original posting. Mapped to HTTP 409 — the caller must not silently
 * succeed, because the two payloads disagree about what was posted.
 */
public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String key) {
        super("Idempotency-Key '" + key + "' was already used with a different payload");
    }
}
