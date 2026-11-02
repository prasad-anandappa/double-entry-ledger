package dev.ledger.api;

import dev.ledger.application.LedgerService;
import dev.ledger.domain.JournalEntry;
import dev.ledger.domain.JournalLine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class EntryDtos {

    private EntryDtos() {
    }

    public record LineRequest(
            @NotBlank String accountCode,
            @PositiveOrZero long debitMinor,
            @PositiveOrZero long creditMinor) {

        LedgerService.LineCommand toCommand() {
            return new LedgerService.LineCommand(accountCode, debitMinor, creditMinor);
        }
    }

    public record PostEntryRequest(
            @NotNull LocalDate valueDate,
            @NotBlank @Size(max = 500) String description,
            @Size(max = 100) String reference,
            @NotEmpty @Valid List<LineRequest> lines) {

        LedgerService.PostEntryCommand toCommand() {
            return new LedgerService.PostEntryCommand(valueDate, description, reference,
                    lines.stream().map(LineRequest::toCommand).toList());
        }
    }

    public record LineResponse(String accountCode, long debitMinor, long creditMinor) {

        static LineResponse from(JournalLine line) {
            return new LineResponse(line.getAccount().getCode(),
                    line.getDebitMinor(), line.getCreditMinor());
        }
    }

    public record JournalEntryResponse(Long id, LocalDate valueDate, String description,
                                       String reference, List<LineResponse> lines,
                                       long totalDebitsMinor, long totalCreditsMinor,
                                       LocalDateTime createdAt) {

        public static JournalEntryResponse from(JournalEntry entry) {
            return new JournalEntryResponse(entry.getId(), entry.getValueDate(),
                    entry.getDescription(), entry.getReference(),
                    entry.getLines().stream().map(LineResponse::from).toList(),
                    entry.totalDebitsMinor(), entry.totalCreditsMinor(), entry.getCreatedAt());
        }
    }
}
