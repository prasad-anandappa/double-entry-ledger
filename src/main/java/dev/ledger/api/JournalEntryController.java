package dev.ledger.api;

import dev.ledger.application.LedgerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class JournalEntryController {

    private final LedgerService ledger;

    public JournalEntryController(LedgerService ledger) {
        this.ledger = ledger;
    }

    /**
     * Posts a journal entry. Send an {@code Idempotency-Key} header to make
     * retries safe: the same key with the same payload returns the original
     * entry; with a different payload it returns 409.
     */
    @PostMapping("/journal-entries")
    public ResponseEntity<EntryDtos.JournalEntryResponse> post(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody EntryDtos.PostEntryRequest request) {
        var entry = ledger.postEntry(request.toCommand(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EntryDtos.JournalEntryResponse.from(entry));
    }

    @GetMapping("/journal-entries/{id}")
    public EntryDtos.JournalEntryResponse get(@PathVariable Long id) {
        return EntryDtos.JournalEntryResponse.from(ledger.getEntry(id));
    }

    @GetMapping("/journal-entries")
    public List<EntryDtos.JournalEntryResponse> list() {
        return ledger.listEntries().stream().map(EntryDtos.JournalEntryResponse::from).toList();
    }

    @GetMapping("/balances")
    public List<ReportDtos.BalanceResponse> balances() {
        return ledger.allBalances().stream().map(ReportDtos.BalanceResponse::from).toList();
    }
}
