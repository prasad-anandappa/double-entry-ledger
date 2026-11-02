package dev.ledger.application;

public class RunNotFoundException extends ResourceNotFoundException {
    public RunNotFoundException(Long id) {
        super("Reconciliation run not found: " + id);
    }
}
