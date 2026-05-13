# booking-service

Spring Boot 3.3 microservice for the NAJA7I (Moroccan EdTech) platform.

- Port: **8105**
- DB: PostgreSQL `booking_db`
- Java package: `ma.naja7i.booking`
- Group/artifact: `ma.naja7i:booking-service:0.1.0-SNAPSHOT`

## Responsibilities

Booking lifecycle (state machine):

```
DEMANDEE -> ACCEPTEE_PAR_PROF -> EN_COURS -> TERMINEE -> CONFIRMEE
   |               |                 |          |
   `---> ANNULEE   `---> ANNULEE     `---> LITIGE   `---> LITIGE (within window)
```

- STUDENT: create / cancel / confirm / dispute / list
- PROF: accept / decline / start
- ADMIN: resolve disputes / filter list
- Consumes `naja7i.video.session.ended.v1` (EN_COURS -> TERMINEE)
- Consumes `naja7i.auth.user.suspended.v1` (bulk-cancel open bookings)

## Key design points

- **Money in BIGINT centimes** (never DECIMAL / float / double).
- **Outbox pattern** — every domain event is persisted in the same TX as the
  business write, then published by `OutboxPoller` every 2s.
- **Anti-bypass** — booking-service exposes only opaque UUIDs (no phone/email).
  See `BookingViewMasker`.
- **Service discovery** — calls `calendar-service` and `pricing-service` via
  Eureka service names (`http://calendar-service`, `http://pricing-service`)
  using a `@LoadBalanced RestClient.Builder`.
- **Idempotent consumers** — `processed_events(event_id PK)` table.
- **Optimistic locking** on `bookings` and `disputes` (@Version).

## Build & run

From `backend/`:

```bash
mvn -pl booking-service -am clean verify
```

Locally:

```bash
mvn -pl booking-service spring-boot:run
```

## Topics produced (via outbox)

- `naja7i.booking.requested.v1`
- `naja7i.booking.accepted.v1`
- `naja7i.booking.declined.v1`
- `naja7i.booking.started.v1`
- `naja7i.booking.confirmed.v1`
- `naja7i.booking.cancelled.v1`
- `naja7i.booking.disputed.v1`
- `naja7i.booking.dispute_resolved.v1`

`naja7i.booking.ended.v1` is produced by **video-service**, not here.
