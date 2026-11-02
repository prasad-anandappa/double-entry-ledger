package dev.ledger.persistence;

import dev.ledger.reconciliation.ReconciliationRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReconciliationRunRepository extends JpaRepository<ReconciliationRun, Long> {

    @Query("""
            select r from ReconciliationRun r
            left join fetch r.exceptions
            where r.id = :id
            """)
    Optional<ReconciliationRun> findByIdWithExceptions(@Param("id") Long id);
}
