package dev.ledger.reconciliation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "reconciliation_exceptions")
public class ReconciliationException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private ReconciliationRun run;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExceptionType type;

    @Column(name = "account_code", nullable = false, length = 32)
    private String accountCode;

    @Column(name = "value_date", nullable = false)
    private LocalDate valueDate;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(length = 100)
    private String reference;

    @Column(length = 500)
    private String description;

    protected ReconciliationException() {
        // JPA
    }

    ReconciliationException(ReconciliationRun run, ReconciliationFinding finding) {
        this.run = run;
        this.type = finding.type();
        this.accountCode = finding.accountCode();
        this.valueDate = finding.valueDate();
        this.amountMinor = finding.amountMinor();
        this.reference = finding.reference();
        this.description = finding.description();
    }

    public Long getId() {
        return id;
    }

    public ExceptionType getType() {
        return type;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public LocalDate getValueDate() {
        return valueDate;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public String getReference() {
        return reference;
    }

    public String getDescription() {
        return description;
    }
}
