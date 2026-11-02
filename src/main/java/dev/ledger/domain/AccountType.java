package dev.ledger.domain;

/**
 * Account categories. ASSET and EXPENSE accounts are "debit-normal"
 * (a debit increases their balance); LIABILITY, EQUITY and REVENUE
 * accounts are "credit-normal".
 */
public enum AccountType {
    ASSET(true),
    EXPENSE(true),
    LIABILITY(false),
    EQUITY(false),
    REVENUE(false);

    private final boolean debitNormal;

    AccountType(boolean debitNormal) {
        this.debitNormal = debitNormal;
    }

    public boolean isDebitNormal() {
        return debitNormal;
    }
}
