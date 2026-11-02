package dev.ledger.application;

import dev.ledger.domain.Account;
import dev.ledger.domain.AccountType;
import dev.ledger.domain.BalanceCalculator;
import dev.ledger.domain.JournalEntry;
import dev.ledger.domain.JournalLine;
import dev.ledger.domain.PostingRules;
import dev.ledger.persistence.AccountRepository;
import dev.ledger.persistence.JournalEntryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

/**
 * Core ledger use cases: accounts, journal posting (idempotent), and
 * derived balances.
 */
@Service
public class LedgerService {

    public record LineCommand(String accountCode, long debitMinor, long creditMinor) {
    }

    public record PostEntryCommand(LocalDate valueDate, String description, String reference,
                                   List<LineCommand> lines) {
    }

    public record Balance(Long accountId, String accountCode, String accountName, String currency,
                          long totalDebitsMinor, long totalCreditsMinor, long balanceMinor) {
    }

    private final AccountRepository accounts;
    private final JournalEntryRepository entries;

    public LedgerService(AccountRepository accounts, JournalEntryRepository entries) {
        this.accounts = accounts;
        this.entries = entries;
    }

    // ---------------------------------------------------------------- accounts

    @Transactional
    public Account createAccount(String code, String name, AccountType type, String currency) {
        if (accounts.existsByCode(code)) {
            throw new DuplicateAccountCodeException(code);
        }
        try {
            return accounts.saveAndFlush(new Account(code, name, type, currency.toUpperCase()));
        } catch (DataIntegrityViolationException race) {
            throw new DuplicateAccountCodeException(code);
        }
    }

    @Transactional(readOnly = true)
    public List<Account> listAccounts() {
        return accounts.findAll();
    }

    @Transactional(readOnly = true)
    public Account getAccount(Long id) {
        return accounts.findById(id).orElseThrow(() -> new AccountNotFoundException(String.valueOf(id)));
    }

    // ---------------------------------------------------------------- posting

    /**
     * Posts a balanced journal entry. When an idempotency key is supplied,
     * a retry with the same key and the same payload returns the original
     * entry instead of double-posting; the same key with a different
     * payload is a conflict. The unique constraint on the key closes the
     * race between the check and the insert.
     */
    @Transactional
    public JournalEntry postEntry(PostEntryCommand command, String idempotencyKey) {
        String payloadHash = payloadHash(command);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = entries.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return resolveIdempotent(existing.get(), payloadHash, idempotencyKey);
            }
        }

        List<JournalLine> lines = command.lines().stream()
                .map(line -> new JournalLine(resolveAccount(line.accountCode()),
                        line.debitMinor(), line.creditMinor()))
                .toList();
        JournalEntry entry = PostingRules.validateAndCreate(command.valueDate(),
                command.description(), command.reference(),
                (idempotencyKey == null || idempotencyKey.isBlank()) ? null : idempotencyKey,
                payloadHash, lines);
        try {
            return entries.saveAndFlush(entry);
        } catch (DataIntegrityViolationException race) {
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                return entries.findByIdempotencyKey(idempotencyKey)
                        .map(found -> resolveIdempotent(found, payloadHash, idempotencyKey))
                        .orElseThrow(() -> race);
            }
            throw race;
        }
    }

    private JournalEntry resolveIdempotent(JournalEntry existing, String payloadHash, String key) {
        if (!payloadHash.equals(existing.getPayloadHash())) {
            throw new IdempotencyConflictException(key);
        }
        return entries.findByIdWithLines(existing.getId()).orElse(existing);
    }

    @Transactional(readOnly = true)
    public JournalEntry getEntry(Long id) {
        return entries.findByIdWithLines(id).orElseThrow(() -> new EntryNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<JournalEntry> listEntries() {
        return entries.findAllWithLines();
    }

    // ---------------------------------------------------------------- balances

    @Transactional(readOnly = true)
    public Balance balanceFor(Long accountId) {
        Account account = getAccount(accountId);
        List<Object[]> rows = entries.totalsForAccount(accountId);
        long debits = 0;
        long credits = 0;
        if (!rows.isEmpty()) {
            debits = ((Number) rows.get(0)[0]).longValue();
            credits = ((Number) rows.get(0)[1]).longValue();
        }
        return toBalance(account, debits, credits);
    }

    @Transactional(readOnly = true)
    public List<Balance> allBalances() {
        var totalsByAccount = new java.util.HashMap<Long, long[]>();
        for (Object[] row : entries.totalsForAllAccounts()) {
            totalsByAccount.put(((Number) row[0]).longValue(),
                    new long[]{((Number) row[1]).longValue(), ((Number) row[2]).longValue()});
        }
        return accounts.findAll().stream()
                .map(account -> {
                    long[] totals = totalsByAccount.getOrDefault(account.getId(), new long[]{0, 0});
                    return toBalance(account, totals[0], totals[1]);
                })
                .sorted(Comparator.comparing(Balance::accountCode))
                .toList();
    }

    private Balance toBalance(Account account, long debits, long credits) {
        return new Balance(account.getId(), account.getCode(), account.getName(),
                account.getCurrency(), debits, credits,
                BalanceCalculator.normalBalance(account.getType(), debits, credits));
    }

    private Account resolveAccount(String code) {
        return accounts.findByCode(code).orElseThrow(() -> new AccountNotFoundException(code));
    }

    /** Canonical fingerprint of a posting payload, for idempotency comparison. */
    static String payloadHash(PostEntryCommand command) {
        String canonicalLines = command.lines().stream()
                .map(line -> line.accountCode() + ":" + line.debitMinor() + ":" + line.creditMinor())
                .sorted()
                .reduce("", (a, b) -> a + "|" + b);
        String canonical = command.valueDate() + "|" + command.description() + "|"
                + (command.reference() == null ? "" : command.reference()) + canonicalLines;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
