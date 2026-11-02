package dev.ledger.api;

import dev.ledger.application.LedgerService;
import dev.ledger.reconciliation.ExceptionType;
import dev.ledger.reconciliation.ReconciliationException;
import dev.ledger.reconciliation.ReconciliationRun;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ReportDtos {

    private ReportDtos() {
    }

    public record BalanceResponse(Long accountId, String accountCode, String accountName,
                                  String currency, long totalDebitsMinor, long totalCreditsMinor,
                                  long balanceMinor) {

        public static BalanceResponse from(LedgerService.Balance balance) {
            return new BalanceResponse(balance.accountId(), balance.accountCode(),
                    balance.accountName(), balance.currency(), balance.totalDebitsMinor(),
                    balance.totalCreditsMinor(), balance.balanceMinor());
        }
    }

    public record ExceptionResponse(ExceptionType type, String accountCode, LocalDate valueDate,
                                    long amountMinor, String reference, String description) {

        static ExceptionResponse from(ReconciliationException exception) {
            return new ExceptionResponse(exception.getType(), exception.getAccountCode(),
                    exception.getValueDate(), exception.getAmountMinor(),
                    exception.getReference(), exception.getDescription());
        }
    }

    public record ReconciliationResponse(Long id, LocalDateTime createdAt, int statementLines,
                                         int ledgerEffects, int matched, int exceptionCount,
                                         List<ExceptionResponse> exceptions) {

        public static ReconciliationResponse from(ReconciliationRun run) {
            return new ReconciliationResponse(run.getId(), run.getCreatedAt(),
                    run.getStatementLines(), run.getLedgerEffects(), run.getMatched(),
                    run.getExceptionCount(),
                    run.getExceptions().stream().map(ExceptionResponse::from).toList());
        }

        /** Summary form (no exception detail) for list views. */
        public static ReconciliationResponse summary(ReconciliationRun run) {
            return new ReconciliationResponse(run.getId(), run.getCreatedAt(),
                    run.getStatementLines(), run.getLedgerEffects(), run.getMatched(),
                    run.getExceptionCount(), List.of());
        }
    }
}
