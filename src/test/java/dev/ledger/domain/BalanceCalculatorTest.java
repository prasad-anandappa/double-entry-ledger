package dev.ledger.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BalanceCalculatorTest {

    @Test
    void assetBalanceIsDebitsMinusCredits() {
        assertThat(BalanceCalculator.normalBalance(AccountType.ASSET, 100_000, 25_000))
                .isEqualTo(75_000);
    }

    @Test
    void expenseBalanceIsDebitsMinusCredits() {
        assertThat(BalanceCalculator.normalBalance(AccountType.EXPENSE, 4_200, 0))
                .isEqualTo(4_200);
    }

    @Test
    void liabilityBalanceIsCreditsMinusDebits() {
        assertThat(BalanceCalculator.normalBalance(AccountType.LIABILITY, 10_000, 90_000))
                .isEqualTo(80_000);
    }

    @Test
    void revenueBalanceIsCreditsMinusDebits() {
        assertThat(BalanceCalculator.normalBalance(AccountType.REVENUE, 0, 100_000))
                .isEqualTo(100_000);
    }

    @Test
    void effectOfCreditToRevenueIsPositive() {
        assertThat(BalanceCalculator.normalEffect(AccountType.REVENUE, 0, 100_000))
                .isEqualTo(100_000);
    }

    @Test
    void effectOfDebitReducingCashIsNegative() {
        // a bank fee posted as a credit to the cash asset reduces its balance
        assertThat(BalanceCalculator.normalEffect(AccountType.ASSET, 0, 1_500))
                .isEqualTo(-1_500);
    }
}
