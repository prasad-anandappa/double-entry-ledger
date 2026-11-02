package dev.ledger.persistence;

import dev.ledger.domain.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    Optional<JournalEntry> findByIdempotencyKey(String idempotencyKey);

    @Query("""
            select distinct e from JournalEntry e
            join fetch e.lines l
            join fetch l.account
            order by e.valueDate, e.id
            """)
    List<JournalEntry> findAllWithLines();

    @Query("""
            select e from JournalEntry e
            join fetch e.lines l
            join fetch l.account
            where e.id = :id
            """)
    Optional<JournalEntry> findByIdWithLines(@Param("id") Long id);

    /** Aggregate posted totals for one account: [totalDebits, totalCredits]. */
    @Query("""
            select coalesce(sum(l.debitMinor), 0), coalesce(sum(l.creditMinor), 0)
            from JournalLine l
            where l.account.id = :accountId
            """)
    List<Object[]> totalsForAccount(@Param("accountId") Long accountId);

    /** Aggregate posted totals for every account: [accountId, totalDebits, totalCredits]. */
    @Query("""
            select l.account.id, coalesce(sum(l.debitMinor), 0), coalesce(sum(l.creditMinor), 0)
            from JournalLine l
            group by l.account.id
            """)
    List<Object[]> totalsForAllAccounts();
}
