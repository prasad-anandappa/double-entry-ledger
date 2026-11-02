package dev.ledger.domain;

/**
 * Balance arithmetic. Balances are always derived from posted lines,
 * never stored, so the ledger can be re-derived and audited at any time.
 * The reported balance is signed in the account's "normal" direction:
 * positive means the account carries its normal (debit or credit) balance.
 */
public final class BalanceCalculator {

    private BalanceCalculator() {
    }

    public static long normalBalance(AccountType type, long totalDebitsMinor, long totalCreditsMinor) {
        return type.isDebitNormal()
                ? totalDebitsMinor - totalCreditsMinor
                : totalCreditsMinor - totalDebitsMinor;
    }

    /**
     * The effect of one journal line on its account's normal balance:
     * +amount for a posting in the normal direction, -amount against it.
     * Used by the reconciliation engine to compare ledger activity with
     * statement lines that are also expressed in normal-balance terms.
     */
    public static long normalEffect(AccountType type, long debitMinor, long creditMinor) {
        return normalBalance(type, debitMinor, creditMinor);
    }
}
