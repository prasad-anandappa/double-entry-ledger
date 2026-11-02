package dev.ledger.api;

import dev.ledger.domain.Account;
import dev.ledger.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class AccountDtos {

    private AccountDtos() {
    }

    public record CreateAccountRequest(
            @NotBlank @Size(max = 32) String code,
            @NotBlank @Size(max = 200) String name,
            @NotNull AccountType type,
            @NotBlank @Size(min = 3, max = 3) String currency) {
    }

    public record AccountResponse(Long id, String code, String name, AccountType type,
                                  String currency, LocalDateTime createdAt) {

        public static AccountResponse from(Account account) {
            return new AccountResponse(account.getId(), account.getCode(), account.getName(),
                    account.getType(), account.getCurrency(), account.getCreatedAt());
        }
    }
}
