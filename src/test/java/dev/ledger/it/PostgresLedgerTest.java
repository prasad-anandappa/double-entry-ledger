package dev.ledger.it;

import dev.ledger.application.LedgerService;
import dev.ledger.application.ReconciliationService;
import dev.ledger.domain.AccountType;
import dev.ledger.persistence.AccountRepository;
import dev.ledger.persistence.JournalEntryRepository;
import dev.ledger.persistence.ReconciliationRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The same flows as the H2 suite, against a real PostgreSQL started by
 * Testcontainers — this is where the Flyway migrations and JPA mappings
 * are proven against the production database engine. Skipped
 * automatically on machines without a Docker daemon; runs in CI.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PostgresLedgerTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ledger")
            .withUsername("ledger")
            .withPassword("ledger");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private LedgerService ledger;

    @Autowired
    private ReconciliationService reconciliations;

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private JournalEntryRepository entries;

    @Autowired
    private ReconciliationRunRepository runs;

    @BeforeEach
    void cleanDatabase() {
        runs.deleteAll();
        entries.deleteAll();
        accounts.deleteAll();
    }

    @Test
    void fullFlowAgainstRealPostgres() throws Exception {
        ledger.createAccount("CASH-100", "Cash", AccountType.ASSET, "USD");
        ledger.createAccount("REV-400", "Revenue", AccountType.REVENUE, "USD");

        var entry = ledger.postEntry(new LedgerService.PostEntryCommand(
                LocalDate.of(2026, 10, 1), "Invoice 001", "INV-001",
                List.of(new LedgerService.LineCommand("CASH-100", 100_000, 0),
                        new LedgerService.LineCommand("REV-400", 0, 100_000))), "pg-key-1");
        assertThat(entry.getId()).isNotNull();

        // idempotent replay returns the same row, no duplicate
        var replay = ledger.postEntry(new LedgerService.PostEntryCommand(
                LocalDate.of(2026, 10, 1), "Invoice 001", "INV-001",
                List.of(new LedgerService.LineCommand("CASH-100", 100_000, 0),
                        new LedgerService.LineCommand("REV-400", 0, 100_000))), "pg-key-1");
        assertThat(replay.getId()).isEqualTo(entry.getId());
        assertThat(entries.count()).isEqualTo(1);

        var cash = accounts.findByCode("CASH-100").orElseThrow();
        assertThat(ledger.balanceFor(cash.getId()).balanceMinor()).isEqualTo(100_000);

        String csv = """
                value_date,account_code,amount_minor,reference,description
                2026-10-01,CASH-100,100000,INV-001,Invoice 001 payment received
                2026-10-01,REV-400,100000,INV-001,Invoice 001 revenue recognised
                2026-10-03,CASH-100,-1500,FEE-0009,Bank service fee
                """;
        var run = reconciliations.runReconciliation(new StringReader(csv));
        assertThat(run.getMatched()).isEqualTo(2);
        assertThat(run.getExceptionCount()).isEqualTo(1);
        assertThat(run.getExceptions().get(0).getType().name()).isEqualTo("MISSING_IN_LEDGER");
    }
}
