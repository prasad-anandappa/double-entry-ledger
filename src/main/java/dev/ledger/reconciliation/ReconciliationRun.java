package dev.ledger.reconciliation;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reconciliation_runs")
public class ReconciliationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "statement_lines", nullable = false)
    private int statementLines;

    @Column(name = "ledger_effects", nullable = false)
    private int ledgerEffects;

    @Column(nullable = false)
    private int matched;

    @Column(name = "exception_count", nullable = false)
    private int exceptionCount;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReconciliationException> exceptions = new ArrayList<>();

    protected ReconciliationRun() {
        // JPA
    }

    public ReconciliationRun(int statementLines, int ledgerEffects, int matched,
                             List<ReconciliationFinding> findings) {
        this.createdAt = LocalDateTime.now();
        this.statementLines = statementLines;
        this.ledgerEffects = ledgerEffects;
        this.matched = matched;
        this.exceptionCount = findings.size();
        findings.forEach(f -> this.exceptions.add(new ReconciliationException(this, f)));
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getStatementLines() {
        return statementLines;
    }

    public int getLedgerEffects() {
        return ledgerEffects;
    }

    public int getMatched() {
        return matched;
    }

    public int getExceptionCount() {
        return exceptionCount;
    }

    public List<ReconciliationException> getExceptions() {
        return List.copyOf(exceptions);
    }
}
