package dev.ledger.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * One side of a journal entry: an amount posted as a debit XOR a credit
 * against a single account. Amounts are minor units (e.g. cents) — never
 * floating point. The one-sided rule is enforced here and again by a
 * database check constraint.
 */
@Entity
@Table(name = "journal_lines")
public class JournalLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_id", nullable = false)
    private JournalEntry entry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "debit_minor", nullable = false)
    private long debitMinor;

    @Column(name = "credit_minor", nullable = false)
    private long creditMinor;

    protected JournalLine() {
        // JPA
    }

    public JournalLine(Account account, long debitMinor, long creditMinor) {
        if (debitMinor < 0 || creditMinor < 0) {
            throw new InvalidLineException("Line amounts cannot be negative");
        }
        if ((debitMinor > 0) == (creditMinor > 0)) {
            throw new InvalidLineException(
                    "Each line must be a debit XOR a credit (exactly one side greater than zero)");
        }
        this.account = Objects.requireNonNull(account, "account");
        this.debitMinor = debitMinor;
        this.creditMinor = creditMinor;
    }

    public static JournalLine debit(Account account, long amountMinor) {
        return new JournalLine(account, amountMinor, 0);
    }

    public static JournalLine credit(Account account, long amountMinor) {
        return new JournalLine(account, 0, amountMinor);
    }

    void attachTo(JournalEntry entry) {
        this.entry = entry;
    }

    public Long getId() {
        return id;
    }

    public JournalEntry getEntry() {
        return entry;
    }

    public Account getAccount() {
        return account;
    }

    public long getDebitMinor() {
        return debitMinor;
    }

    public long getCreditMinor() {
        return creditMinor;
    }
}
