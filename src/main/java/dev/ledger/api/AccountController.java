package dev.ledger.api;

import dev.ledger.application.LedgerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final LedgerService ledger;

    public AccountController(LedgerService ledger) {
        this.ledger = ledger;
    }

    @PostMapping
    public ResponseEntity<AccountDtos.AccountResponse> create(
            @Valid @RequestBody AccountDtos.CreateAccountRequest request) {
        var account = ledger.createAccount(request.code(), request.name(),
                request.type(), request.currency());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AccountDtos.AccountResponse.from(account));
    }

    @GetMapping
    public List<AccountDtos.AccountResponse> list() {
        return ledger.listAccounts().stream().map(AccountDtos.AccountResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AccountDtos.AccountResponse get(@PathVariable Long id) {
        return AccountDtos.AccountResponse.from(ledger.getAccount(id));
    }

    @GetMapping("/{id}/balance")
    public ReportDtos.BalanceResponse balance(@PathVariable Long id) {
        return ReportDtos.BalanceResponse.from(ledger.balanceFor(id));
    }
}
