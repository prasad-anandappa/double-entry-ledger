package dev.ledger.reconciliation;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvStatementParserTest {

    @Test
    void parsesRowsIncludingQuotedFieldsWithCommas() throws Exception {
        String csv = """
                value_date,account_code,amount_minor,reference,description
                2026-10-01,CASH-100,100000,INV-001,"Payment, invoice 001"
                2026-10-03,CASH-100,-1500,FEE-0009,Bank service fee
                """;

        List<StatementLine> lines = CsvStatementParser.parse(new StringReader(csv));

        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).valueDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(lines.get(0).accountCode()).isEqualTo("CASH-100");
        assertThat(lines.get(0).amountMinor()).isEqualTo(100_000);
        assertThat(lines.get(0).reference()).isEqualTo("INV-001");
        assertThat(lines.get(0).description()).isEqualTo("Payment, invoice 001");
        assertThat(lines.get(1).amountMinor()).isEqualTo(-1_500);
    }

    @Test
    void rejectsWrongHeader() {
        String csv = "date,account,amount\n2026-10-01,CASH-100,100\n";
        assertThatThrownBy(() -> CsvStatementParser.parse(new StringReader(csv)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("header");
    }

    @Test
    void rejectsRowWithWrongFieldCount() {
        String csv = """
                value_date,account_code,amount_minor,reference,description
                2026-10-01,CASH-100,100000
                """;
        assertThatThrownBy(() -> CsvStatementParser.parse(new StringReader(csv)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Row 2");
    }

    @Test
    void rejectsNonNumericAmount() {
        String csv = """
                value_date,account_code,amount_minor,reference,description
                2026-10-01,CASH-100,ten,INV-001,Bad amount
                """;
        assertThatThrownBy(() -> CsvStatementParser.parse(new StringReader(csv)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Row 2");
    }
}
