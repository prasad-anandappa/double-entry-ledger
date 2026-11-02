package dev.ledger.application;

public class AccountNotFoundException extends ResourceNotFoundException {
    public AccountNotFoundException(String identifier) {
        super("Account not found: " + identifier);
    }
}
