# NAJA7I — Payment Service

Money-of-record microservice for the NAJA7I platform. It owns:

- **Wallet balances** (one row per user, BIGINT centimes, CHECK `>= 0`)
- **Escrow** locked on `booking.requested`, released to prof on `booking.confirmed`,
  refunded on cancel/decline, split/refunded/paid on dispute resolution
- **Top-up requests** (CashPlus / bank transfer / WhatsApp) awaiting admin approval
- **Payout requests** (prof withdraws to bank) — admin approve then mark-paid
- **Immutable audit log** — every balance change is journaled atomically; the DB
  rejects UPDATE/DELETE on the table via triggers

## Architecture

Standard NAJA7I service layout:

- `domain/` — JPA entities (`Balance`, `Escrow`, `Topup`, `Payout`, `AuditLog`, `OutboxEvent`, `ProcessedEvent`)
- `application/` — services. The heart is **`BalanceLedger`** — every balance
  mutation in the codebase routes through this single method, which atomically:
  1. `SELECT ... FOR UPDATE` the balance row
  2. compute `balance_after`
  3. translate would-be-negative results into `PAYMENT_INSUFFICIENT_BALANCE`
  4. update the balance
  5. insert an `audit_log` row with the computed before/after
- `infrastructure/persistence/` — Spring Data JPA repositories
- `infrastructure/messaging/` — Kafka producer (outbox + scheduled poller),
  consumer (idempotent via `processed_events` PK dedup)
- `infrastructure/security/` — gateway-header auth filter, path-level role gating
- `infrastructure/web/` — REST controllers, DTOs

## Endpoints

All under `/api/v1/payment`. Auth via gateway headers (`X-User-Id`, `X-User-Role`).

### Student
- `GET  /balance/me`
- `GET  /audit/me`
- `POST /topups` — submit top-up (status `EN_ATTENTE_ADMIN`)
- `GET  /topups/me`

### Prof
- `GET  /balance/me`
- `GET  /audit/me`
- `POST /payouts` — request payout (DOES NOT debit; waits for admin)
- `GET  /payouts/me`

### Admin
- `GET  /admin/topups?status=...`
- `POST /admin/topups/{id}:decide` — APPROVE / MODIFY / REJECT (credits balance on first two)
- `GET  /admin/payouts?status=...`
- `POST /admin/payouts/{id}:decide` — APPROVE (debits balance) / REJECT
- `POST /admin/payouts/{id}:markPaid` — APPROVED → PAID after bank transfer
- `POST /admin/balance/{userId}:adjust` — manual ADMIN_CREDIT / ADMIN_DEBIT / ADMIN_ADJUST
- `GET  /admin/audit/{userId}`
- `POST /admin/audit/export?format=CSV` — CSV streaming export

## Kafka

Producers (outbox pattern, key in parens):
- `naja7i.payment.topup.submitted.v1`  (`studentId`)
- `naja7i.payment.topup.decided.v1`    (`studentId`)
- `naja7i.payment.escrow.locked.v1`    (`bookingId`)
- `naja7i.payment.escrow.released.v1`  (`bookingId`)
- `naja7i.payment.payout.requested.v1` (`profId`)
- `naja7i.payment.payout.decided.v1`   (`profId`)
- `naja7i.payment.balance.adjusted.v1` (`userId`)

Consumers (idempotent via `processed_events`):
- `naja7i.booking.requested.v1`         → lock escrow (debit student)
- `naja7i.booking.confirmed.v1`         → release escrow to prof
- `naja7i.booking.cancelled.v1`         → refund student
- `naja7i.booking.declined.v1`          → refund student
- `naja7i.booking.dispute_resolved.v1`  → REFUND / PAY_PROF / SPLIT

## Money safety invariants (enforced by tests + DB)

1. `balances.balance_minor >= 0` — DB CHECK; app layer surfaces as
   `PAYMENT_INSUFFICIENT_BALANCE`.
2. Every balance change inserts exactly one `audit_log` row in the SAME
   transaction (via `BalanceLedger.apply`). No code path may skip it.
3. `audit_log` is append-only — triggers reject UPDATE/DELETE.
4. `escrows.booking_id UNIQUE` prevents double-locking on duplicate
   `booking.requested`.
5. All consumers insert their `processed_events` row FIRST; duplicates are
   rejected before any money-moving side effect.

## Build & test

```
mvn -pl payment-service -am clean verify
```

Tests use Testcontainers (Postgres 16); Docker is required for the full suite,
but unit-only tests run without Docker. The audit-log immutability test fires
the real DB triggers — it MUST pass when Docker is available.
