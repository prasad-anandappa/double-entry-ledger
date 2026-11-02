package dev.ledger.application;

public class EntryNotFoundException extends ResourceNotFoundException {
    public EntryNotFoundException(Long id) {
        super("Journal entry not found: " + id);
    }
}
