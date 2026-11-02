package dev.ledger.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A balanced set of journal lines posted atomically. An entry can only be
 * constructed through {@link PostingRules#validateAndCreate}, so an
 * unbalanced entry can never exist in the domain, let alone persist.
 */
@Entity
@Table(name = "journal_entries")
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "value_date", nullable = false)
    private LocalDate valueDate;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(length = 100)
    private String reference;

    @Column(name = "idempotency_key", unique = true, length = 200)
    private String idempotencyKey;

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalLine> lines = new ArrayList<>();

    protected JournalEntry() {
        // JPA
    }

    JournalEntry(LocalDate valueDate, String description, String reference,
                 String idempotencyKey, String payloadHash, List<JournalLine> lines) {
        this.valueDate = valueDate;
        this.description = description;
        this.reference = reference;
        this.idempotencyKey = idempotencyKey;
        this.payloadHash = payloadHash;
        this.createdAt = LocalDateTime.now();
        lines.forEach(line -> {
            line.attachTo(this);
            this.lines.add(line);
        });
    }

    public long totalDebitsMinor() {
        return lines.stream().mapToLong(JournalLine::getDebitMinor).sum();
    }

    public long totalCreditsMinor() {
        return lines.stream().mapToLong(JournalLine::getCreditMinor).sum();
    }

    public Long getId() {
        return id;
    }

    public LocalDate getValueDate() {
        return valueDate;
    }

    public String getDescription() {
        return description;
    }

    public String getReference() {
        return reference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<JournalLine> getLines() {
        return List.copyOf(lines);
    }
}
