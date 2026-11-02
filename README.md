# Double-Entry Ledger

![CI](https://github.com/prasad-anandappa/double-entry-ledger/actions/workflows/ci.yml/badge.svg)

A double-entry accounting ledger and reconciliation service built with
**Java 21, Spring Boot 3 and PostgreSQL**. Every posting is balanced by
construction, safe to retry, and fully auditable — the ledger is the source
of truth, and balances are always derived from it, never stored.

## What it does

- **Chart of accounts** — assets, liabilities, equity, revenue and expenses,
  each with a currency.
- **Journal posting** — multi-line entries where total debits must equal
  total credits. The invariant is enforced in the domain model *and* by
  database constraints, so an unbalanced entry can never persist.
- **Idempotent postings** — send an `Idempotency-Key` header and retries are
  safe: the same key + same payload returns the original entry; the same key
  with a *different* payload returns `409 Conflict`.
- **Derived balances** — balances are computed from posted lines on demand,
  signed in each account's normal direction (debit-normal for assets and
  expenses, credit-normal for liabilities, equity and revenue).
- **Statement reconciliation** — upload a bank-statement CSV and get back a
  persisted run: what matched, and the exceptions that need a human —
  lines missing in the ledger, lines missing on the statement, and amount
  mismatches paired by reference so a keyed-wrong amount shows up once.

## Quick start

You need Docker (for PostgreSQL) — or any PostgreSQL 16 with a `ledger`
database/user matching `docker-compose.yml`.

```bash
docker compose up --build        # starts PostgreSQL and the app on :8080
```

Then, in another terminal:

```bash
# create two accounts
curl -s -X POST localhost:8080/api/accounts -H 'Content-Type: application/json' -d \
  '{"code":"CASH-100","name":"Cash","type":"ASSET","currency":"USD"}'
curl -s -X POST localhost:8080/api/accounts -H 'Content-Type: application/json' -d \
  '{"code":"REV-400","name":"Sales revenue","type":"REVENUE","currency":"USD"}'

# post a balanced entry (amounts are minor units — 100000 = $1,000.00)
curl -s -X POST localhost:8080/api/journal-entries \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: order-001' -d \
  '{"valueDate":"2026-10-01","description":"Invoice 001","reference":"INV-001",
    "lines":[{"accountCode":"CASH-100","debitMinor":100000,"creditMinor":0},
             {"accountCode":"REV-400","debitMinor":0,"creditMinor":100000}]}'

# read balances
curl -s localhost:8080/api/balances

# reconcile against the sample bank statement
curl -s -X POST localhost:8080/api/reconciliations -F file=@samples/statement.csv
```

The sample statement matches the entry above on both accounts and adds one
bank fee that is not in the ledger — the reconciliation reports
`matched: 2` and one `MISSING_IN_LEDGER` exception.

## API summary

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/accounts` | Create an account (`201`, duplicate code `409`) |
| `GET` | `/api/accounts` | List accounts |
| `GET` | `/api/accounts/{id}` | One account |
| `GET` | `/api/accounts/{id}/balance` | Derived balance for one account |
| `GET` | `/api/balances` | Derived balances for all accounts |
| `POST` | `/api/journal-entries` | Post an entry (optional `Idempotency-Key` header) |
| `GET` | `/api/journal-entries/{id}` | One entry with its lines |
| `GET` | `/api/journal-entries` | All entries |
| `POST` | `/api/reconciliations` | Upload a statement CSV (`multipart`, field `file`) and reconcile |
| `GET` | `/api/reconciliations/{id}` | One run with its exceptions |
| `GET` | `/api/reconciliations` | Run history (summaries) |

Errors are RFC 7807 problem details: `404` unknown account/entry,
`409` duplicate account code or idempotency-key conflict, `422` a
well-formed request that breaks a ledger rule (unbalanced entry, a line
that is not a clean debit-XOR-credit, mixed currencies), `400` a malformed
statement CSV.

### Statement CSV format

```csv
value_date,account_code,amount_minor,reference,description
2026-10-01,CASH-100,100000,INV-001,Invoice 001 payment received
```

`amount_minor` is signed in the account's normal-balance direction: a
credit to a revenue account is positive; a fee that reduces a cash asset
is negative.

## Design decisions

- **Minor units everywhere.** Money is `long` minor units (cents) end to
  end. No `double`, no rounding surprises; formatting is a display concern.
- **The invariant lives in the domain.** A `JournalEntry` can only be
  created through `PostingRules.validateAndCreate`, which checks line
  shape, single currency and debit = credit. A database `CHECK` constraint
  on each line (exactly one side > 0) is defence in depth, not the
  primary rule.
- **Balances are derived, never stored.** There is no balance column to
  drift out of sync; any balance can be reproduced from the journal, which
  is what makes the ledger auditable.
- **Idempotency by key + payload hash.** The stored SHA-256 fingerprint of
  the canonical payload distinguishes a true retry (return the original)
  from key reuse with a different payload (conflict). The unique
  constraint on the key closes the check-then-insert race, and the loser
  of that race re-reads and resolves instead of failing.
- **Reconciliation matches in two passes.** Exact matches on
  account/date/amount/reference first; then, among leftovers, same
  account/date/reference with a different amount is reported as one
  `AMOUNT_MISMATCH` instead of two misleading "missing" items.
- **Flyway owns the schema.** JPA runs with `ddl-auto: validate`, so the
  migrations — not Hibernate — are the schema contract, in tests and in
  production alike.

## Architecture

```
api/            REST controllers, DTOs, RFC 7807 error mapping
application/    LedgerService, ReconciliationService (use cases, transactions)
domain/         Account, JournalEntry, JournalLine, PostingRules, BalanceCalculator
reconciliation/ statement model, CSV parser, matcher, run/exception entities
persistence/    Spring Data JPA repositories
```

## Tests

```bash
mvn verify
```

- Unit tests for the posting invariant, balance arithmetic, the matcher
  and the CSV parser.
- MockMvc API tests against H2 in PostgreSQL mode — the same Flyway
  migrations run as in production.
- A Testcontainers suite (`PostgresLedgerTest`) that runs the full flow
  against real PostgreSQL. It needs a Docker daemon, skips cleanly
  without one, and runs in CI where Docker is available.

## Tech

Java 21 · Spring Boot 3.4 · Spring Data JPA · PostgreSQL 16 · Flyway ·
JUnit 5 · Testcontainers · Docker Compose · GitHub Actions

## License

MIT — see [LICENSE](LICENSE).
