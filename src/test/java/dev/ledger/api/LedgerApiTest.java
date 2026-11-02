package dev.ledger.api;

import dev.ledger.persistence.AccountRepository;
import dev.ledger.persistence.JournalEntryRepository;
import dev.ledger.persistence.ReconciliationRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end API tests on the H2 (PostgreSQL-mode) database — the same
 * Flyway migrations run here as against real PostgreSQL, which the
 * Testcontainers suite covers where Docker is available.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LedgerApiTest {

    @Autowired
    private MockMvc mockMvc;

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

    private void createAccount(String code, String type) throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"%s","type":"%s","currency":"USD"}
                                """.formatted(code, code, type)))
                .andExpect(status().isCreated());
    }

    private String entryJson(String description, String reference, String linesJson) {
        return """
                {"valueDate":"2026-10-01","description":"%s","reference":"%s","lines":[%s]}
                """.formatted(description, reference, linesJson);
    }

    @Test
    void createAccountAndRejectDuplicateCode() throws Exception {
        createAccount("CASH-100", "ASSET");

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"CASH-100","name":"Cash again","type":"ASSET","currency":"USD"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownAccountIs404() throws Exception {
        mockMvc.perform(get("/api/accounts/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postBalancedEntryAndReadBalances() throws Exception {
        createAccount("CASH-100", "ASSET");
        createAccount("REV-400", "REVENUE");

        mockMvc.perform(post("/api/journal-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryJson("Invoice 001", "INV-001",
                                """
                                {"accountCode":"CASH-100","debitMinor":100000,"creditMinor":0},
                                {"accountCode":"REV-400","debitMinor":0,"creditMinor":100000}
                                """)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalDebitsMinor").value(100000))
                .andExpect(jsonPath("$.totalCreditsMinor").value(100000));

        long cashId = accounts.findByCode("CASH-100").orElseThrow().getId();
        long revenueId = accounts.findByCode("REV-400").orElseThrow().getId();

        mockMvc.perform(get("/api/accounts/{id}/balance", cashId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceMinor").value(100000));
        mockMvc.perform(get("/api/accounts/{id}/balance", revenueId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceMinor").value(100000));

        mockMvc.perform(get("/api/balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void unbalancedEntryIs422() throws Exception {
        createAccount("CASH-100", "ASSET");
        createAccount("REV-400", "REVENUE");

        mockMvc.perform(post("/api/journal-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryJson("Broken", "INV-BAD",
                                """
                                {"accountCode":"CASH-100","debitMinor":100000,"creditMinor":0},
                                {"accountCode":"REV-400","debitMinor":0,"creditMinor":90000}
                                """)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void entryAgainstUnknownAccountIs404() throws Exception {
        createAccount("CASH-100", "ASSET");

        mockMvc.perform(post("/api/journal-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryJson("Ghost", "INV-GHOST",
                                """
                                {"accountCode":"CASH-100","debitMinor":100,"creditMinor":0},
                                {"accountCode":"NOPE-999","debitMinor":0,"creditMinor":100}
                                """)))
                .andExpect(status().isNotFound());
    }

    @Test
    void idempotentRetryReturnsOriginalAndConflictingReuseIs409() throws Exception {
        createAccount("CASH-100", "ASSET");
        createAccount("REV-400", "REVENUE");
        String payload = entryJson("Idempotent sale", "INV-777",
                """
                {"accountCode":"CASH-100","debitMinor":5000,"creditMinor":0},
                {"accountCode":"REV-400","debitMinor":0,"creditMinor":5000}
                """);

        String firstResponse = mockMvc.perform(post("/api/journal-entries")
                        .header("Idempotency-Key", "order-777")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // retry with the identical payload: same entry, no double posting
        String secondResponse = mockMvc.perform(post("/api/journal-entries")
                        .header("Idempotency-Key", "order-777")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number firstId = com.jayway.jsonpath.JsonPath.read(firstResponse, "$.id");
        Number secondId = com.jayway.jsonpath.JsonPath.read(secondResponse, "$.id");
        org.assertj.core.api.Assertions.assertThat(secondId.longValue())
                .isEqualTo(firstId.longValue());

        mockMvc.perform(get("/api/journal-entries"))
                .andExpect(status().isOk());
        // exactly one entry exists despite the retry — no double posting
        org.assertj.core.api.Assertions.assertThat(entries.count()).isEqualTo(1);

        // same key, different payload: conflict
        String differentPayload = entryJson("Idempotent sale", "INV-777",
                """
                {"accountCode":"CASH-100","debitMinor":6000,"creditMinor":0},
                {"accountCode":"REV-400","debitMinor":0,"creditMinor":6000}
                """);
        mockMvc.perform(post("/api/journal-entries")
                        .header("Idempotency-Key", "order-777")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(differentPayload))
                .andExpect(status().isConflict());
    }

    @Test
    void reconciliationReportsMatchedAndMissingInLedger() throws Exception {
        createAccount("CASH-100", "ASSET");
        createAccount("REV-400", "REVENUE");
        mockMvc.perform(post("/api/journal-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryJson("Invoice 001", "INV-001",
                                """
                                {"accountCode":"CASH-100","debitMinor":100000,"creditMinor":0},
                                {"accountCode":"REV-400","debitMinor":0,"creditMinor":100000}
                                """)))
                .andExpect(status().isCreated());

        String csv = """
                value_date,account_code,amount_minor,reference,description
                2026-10-01,CASH-100,100000,INV-001,Invoice 001 payment received
                2026-10-01,REV-400,100000,INV-001,Invoice 001 revenue recognised
                2026-10-03,CASH-100,-1500,FEE-0009,Bank service fee
                """;
        var file = new MockMultipartFile("file", "statement.csv",
                "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        String response = mockMvc.perform(multipart("/api/reconciliations").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(2))
                .andExpect(jsonPath("$.exceptionCount").value(1))
                .andExpect(jsonPath("$.exceptions[0].type").value("MISSING_IN_LEDGER"))
                .andExpect(jsonPath("$.exceptions[0].accountCode").value("CASH-100"))
                .andExpect(jsonPath("$.exceptions[0].amountMinor").value(-1500))
                .andReturn().getResponse().getContentAsString();

        Number runId = com.jayway.jsonpath.JsonPath.read(response, "$.id");
        mockMvc.perform(get("/api/reconciliations/{id}", runId.longValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(2))
                .andExpect(jsonPath("$.exceptions.length()").value(1));
    }

    @Test
    void reconciliationWithUnknownAccountCodeIs400() throws Exception {
        String csv = """
                value_date,account_code,amount_minor,reference,description
                2026-10-01,NOPE-999,100,REF-1,Mystery line
                """;
        var file = new MockMultipartFile("file", "statement.csv",
                "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/reconciliations").file(file))
                .andExpect(status().isBadRequest());
    }
}
