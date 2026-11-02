package dev.ledger.application;

import dev.ledger.domain.Account;
import dev.ledger.domain.BalanceCalculator;
import dev.ledger.domain.JournalEntry;
import dev.ledger.persistence.AccountRepository;
import dev.ledger.persistence.JournalEntryRepository;
import dev.ledger.persistence.ReconciliationRunRepository;
import dev.ledger.reconciliation.CsvStatementParser;
import dev.ledger.reconciliation.LedgerEffect;
import dev.ledger.reconciliation.ReconciliationMatcher;
import dev.ledger.reconciliation.ReconciliationRun;
import dev.ledger.reconciliation.StatementLine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs a bank statement against the ledger and persists the outcome:
 * how much matched, and the exceptions a human needs to look at.
 */
@Service
public class ReconciliationService {

    private final AccountRepository accounts;
    private final JournalEntryRepository entries;
    private final ReconciliationRunRepository runs;

    public ReconciliationService(AccountRepository accounts, JournalEntryRepository entries,
                                 ReconciliationRunRepository runs) {
        this.accounts = accounts;
        this.entries = entries;
        this.runs = runs;
    }

    @Transactional
    public ReconciliationRun runReconciliation(Reader statementCsv) throws IOException {
        List<StatementLine> statement = CsvStatementParser.parse(statementCsv);

        Map<String, Account> byCode = new LinkedHashMap<>();
        accounts.findAll().forEach(account -> byCode.put(account.getCode(), account));
        for (StatementLine line : statement) {
            if (!byCode.containsKey(line.accountCode())) {
                throw new IllegalArgumentException(
                        "Statement references unknown account code: " + line.accountCode());
            }
        }

        List<LedgerEffect> effects = ledgerEffects();
        ReconciliationMatcher.Result result = ReconciliationMatcher.match(statement, effects);
        ReconciliationRun run = new ReconciliationRun(
                statement.size(), effects.size(), result.matched(), result.findings());
        return runs.saveAndFlush(run);
    }

    @Transactional(readOnly = true)
    public ReconciliationRun getRun(Long id) {
        return runs.findByIdWithExceptions(id).orElseThrow(() -> new RunNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<ReconciliationRun> listRuns() {
        return runs.findAll();
    }

    /**
     * Flattens every posted entry into one effect per entry per account —
     * the net movement a statement would show — in normal-balance terms.
     * Entries whose lines for an account cancel out exactly have no
     * statement-visible effect and are skipped.
     */
    private List<LedgerEffect> ledgerEffects() {
        List<LedgerEffect> effects = new ArrayList<>();
        for (JournalEntry entry : entries.findAllWithLines()) {
            Map<Account, Long> netByAccount = new LinkedHashMap<>();
            entry.getLines().forEach(line -> netByAccount.merge(line.getAccount(),
                    BalanceCalculator.normalEffect(line.getAccount().getType(),
                            line.getDebitMinor(), line.getCreditMinor()),
                    Long::sum));
            netByAccount.forEach((account, net) -> {
                if (net != 0) {
                    effects.add(new LedgerEffect(entry.getId(), entry.getValueDate(),
                            account.getCode(), net, entry.getReference(), entry.getDescription()));
                }
            });
        }
        return effects;
    }
}
